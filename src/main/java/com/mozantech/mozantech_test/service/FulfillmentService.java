package com.mozantech.mozantech_test.service;

import com.mozantech.mozantech_test.domain.Order;
import com.mozantech.mozantech_test.domain.StockMovement;
import com.mozantech.mozantech_test.dto.event.OrderCompletedEvent;
import com.mozantech.mozantech_test.repository.OrderRepository;
import com.mozantech.mozantech_test.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FulfillmentService {

    private final StockMovementRepository movements;
    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    @Transactional(propagation = Propagation.MANDATORY)
    public void fulfill(Order order) {
        long needed = order.getQuantity() - movements.fulfilled(order.getId());
        for (StockMovement free : movements.findFree(order.getItem().getId())) {
            if (needed <= 0) break;
            needed -= assign(free, order, (int) needed);
        }
        if (needed <= 0) completed(order);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void distribute(StockMovement movement) {
        for (Order order : orders.findIncomplete(movement.getItem().getId())) {
            int missing = (int) (order.getQuantity() - movements.fulfilled(order.getId()));
            if (missing <= 0) continue;
            int taken = assign(movement, order, missing);
            if (taken == missing) completed(order);
            if (movement.getAssignedOrder() != null) break;   // movement fully used
        }
    }

    private int assign(StockMovement free, Order order, int needed) {
        int taken = Math.min(free.getQuantity(), needed);
        if (free.getQuantity() <= needed) {
            free.setAssignedOrder(order);
        } else {
            StockMovement part = new StockMovement();
            part.setItem(free.getItem());
            part.setCreationDate(free.getCreationDate());
            part.setQuantity(needed);
            part.setAssignedOrder(order);
            part.setOrigin(free.getOrigin() != null ? free.getOrigin() : free);
            movements.save(part);
            free.setQuantity(free.getQuantity() - needed);
        }
        movements.save(free);
        log.info("STOCK MOVEMENT {} -> ORDER {}: {} units of item {}",
                free.getId(), order.getId(), taken, free.getItem().getId());
        return taken;
    }

    private void completed(Order order) {
        log.info("ORDER COMPLETED id={} item={} quantity={} user={}",
                order.getId(), order.getItem().getName(), order.getQuantity(), order.getUser().getEmail());
        events.publishEvent(new OrderCompletedEvent(order.getId(), order.getUser().getName(),
                order.getUser().getEmail(), order.getItem().getName(), order.getQuantity()));
    }
}

