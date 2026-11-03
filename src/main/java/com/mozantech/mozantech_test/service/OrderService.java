package com.mozantech.mozantech_test.service;

import com.mozantech.mozantech_test.domain.Item;
import com.mozantech.mozantech_test.domain.Order;
import com.mozantech.mozantech_test.domain.StockMovement;
import com.mozantech.mozantech_test.domain.User;
import com.mozantech.mozantech_test.dto.request.OrderRequest;
import com.mozantech.mozantech_test.dto.request.QuantityRequest;
import com.mozantech.mozantech_test.dto.response.OrderResponse;
import com.mozantech.mozantech_test.dto.response.StockMovementResponse;
import com.mozantech.mozantech_test.exception.ConflictException;
import com.mozantech.mozantech_test.exception.NotFoundException;
import com.mozantech.mozantech_test.repository.ItemRepository;
import com.mozantech.mozantech_test.repository.OrderRepository;
import com.mozantech.mozantech_test.repository.StockMovementRepository;
import com.mozantech.mozantech_test.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orders;
    private final UserRepository users;
    private final ItemRepository items;
    private final StockMovementRepository movements;
    private final FulfillmentService fulfillment;
    private final StockMovementService movementService;

    public OrderResponse create(OrderRequest r) {
        User user = users.findById(r.getUserId()).orElseThrow(() -> new NotFoundException("User " + r.getUserId() + " not found"));
        Item item = items.findByIdForUpdate(r.getItemId()).orElseThrow(() -> new NotFoundException("Item " + r.getItemId() + " not found"));
        Order o = new Order();
        o.setUser(user);
        o.setItem(item);
        o.setQuantity(r.getQuantity());
        orders.save(o);
        log.info("ORDER CREATED id={} item={} quantity={} user={}", o.getId(), item.getName(), o.getQuantity(), user.getEmail());
        fulfillment.fulfill(o);
        return toResponse(o);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orders.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return toResponse(get(id));
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> movementsOf(Long id) {
        get(id);
        return movements.findByAssignedOrderIdOrderById(id).stream().map(movementService::toResponse).collect(Collectors.toList());
    }

    public OrderResponse update(Long id, QuantityRequest r) {
        Order o = get(id);
        items.findByIdForUpdate(o.getItem().getId());
        long done = movements.fulfilled(id);
        if (done >= o.getQuantity()) throw new ConflictException("Orders já feitas não podem ser alteradas");
        if (r.getQuantity() < done)
            throw new ConflictException("Quantidade não pode ser abaixo de " + done + " unidades já armazenadas");
        o.setQuantity(r.getQuantity());
        log.info("ORDER UPDATED id={} quantity={}", id, r.getQuantity());
        fulfillment.fulfill(o);
        return toResponse(o);
    }

    public void delete(Long id) {
        Order o = get(id);
        items.findByIdForUpdate(o.getItem().getId());
        List<StockMovement> released = movements.findByAssignedOrderIdOrderById(id);
        released.forEach(m -> m.setAssignedOrder(null));
        movements.saveAll(released);
        orders.delete(o);
        orders.flush();
        log.info("ORDER DELETED id={} releasedMovements={}", id, released.size());
        released.forEach(fulfillment::distribute);
    }

    private Order get(Long id) {
        return orders.findById(id).orElseThrow(() -> new NotFoundException("Order " + id + " não encontrada"));
    }

    private OrderResponse toResponse(Order o) {
        long done = movements.fulfilled(o.getId());
        OrderResponse r = new OrderResponse();
        r.setId(o.getId());
        r.setCreationDate(o.getCreationDate());
        r.setItemId(o.getItem().getId());
        r.setItemName(o.getItem().getName());
        r.setUserId(o.getUser().getId());
        r.setQuantity(o.getQuantity());
        r.setFulfilledQuantity(done);
        r.setCompletionPercent(Math.round(done * 1000.0 / o.getQuantity()) / 10.0);
        r.setCompleted(done >= o.getQuantity());
        return r;
    }
}
