package com.mozantech.mozantech_test.service;

import com.mozantech.mozantech_test.domain.Item;
import com.mozantech.mozantech_test.domain.StockMovement;
import com.mozantech.mozantech_test.dto.request.QuantityRequest;
import com.mozantech.mozantech_test.dto.request.StockMovementRequest;
import com.mozantech.mozantech_test.dto.response.StockMovementResponse;
import com.mozantech.mozantech_test.exception.ConflictException;
import com.mozantech.mozantech_test.exception.NotFoundException;
import com.mozantech.mozantech_test.repository.ItemRepository;
import com.mozantech.mozantech_test.repository.StockMovementRepository;
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
public class StockMovementService {
    private final StockMovementRepository movements;
    private final ItemRepository items;
    private final FulfillmentService fulfillment;

    public StockMovementResponse create(StockMovementRequest r) {
        Item item = items.findByIdForUpdate(r.getItemId()).orElseThrow(() -> new NotFoundException("Item " + r.getItemId() + " not found"));
        StockMovement m = new StockMovement();
        m.setItem(item);
        m.setQuantity(r.getQuantity());
        movements.save(m);
        log.info("STOCK MOVEMENT CREATED id={} item={} quantity={}", m.getId(), item.getName(), m.getQuantity());
        fulfillment.distribute(m);
        return toResponse(m);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> findAll() {
        return movements.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StockMovementResponse findById(Long id) {
        return toResponse(get(id));
    }

    public StockMovementResponse update(Long id, QuantityRequest r) {
        StockMovement m = get(id);
        if (m.getAssignedOrder() != null)
            throw new ConflictException("Movement já foi usado pela order " + m.getAssignedOrder().getId());
        items.findByIdForUpdate(m.getItem().getId());
        m.setQuantity(r.getQuantity());
        log.info("STOCK MOVEMENT UPDATED id={} quantity={}", id, r.getQuantity());
        fulfillment.distribute(m);
        return toResponse(m);
    }

    public void delete(Long id) {
        StockMovement m = get(id);
        if (m.getAssignedOrder() != null)
            throw new ConflictException("Movement já foi usado pela order " + m.getAssignedOrder().getId());
        movements.delete(m);
        log.info("STOCK MOVEMENT DELETED id={}", id);
    }

    private StockMovement get(Long id) {
        return movements.findById(id).orElseThrow(() -> new NotFoundException("Stock movement " + id + " não encontrado"));
    }

    StockMovementResponse toResponse(StockMovement m) {
        StockMovementResponse r = new StockMovementResponse();
        r.setId(m.getId());
        r.setCreationDate(m.getCreationDate());
        r.setItemId(m.getItem().getId());
        r.setQuantity(m.getQuantity());
        r.setOrderId(m.getAssignedOrder() == null ? null : m.getAssignedOrder().getId());
        return r;
    }
}
