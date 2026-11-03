package com.mozantech.mozantech_test.repository;

import com.mozantech.mozantech_test.domain.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("select m from StockMovement m where m.item.id = :itemId and m.assignedOrder is null " +
            "order by m.creationDate, m.id")
    List<StockMovement> findFree(@Param("itemId") Long itemId);

    List<StockMovement> findByAssignedOrderIdOrderById(Long orderId);

    @Query("select coalesce(sum(m.quantity), 0L) from StockMovement m where m.assignedOrder.id = :orderId")
    long fulfilled(@Param("orderId") Long orderId);
}
