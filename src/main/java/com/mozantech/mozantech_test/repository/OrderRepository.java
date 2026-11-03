package com.mozantech.mozantech_test.repository;

import com.mozantech.mozantech_test.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {

    @Query("select o from Order o where o.item.id = :itemId and o.quantity > " +
            "(select coalesce(sum(m.quantity), 0L) from StockMovement m where m.assignedOrder = o) " +
            "order by o.creationDate, o.id")
    List<Order> findIncomplete(@Param("itemId") Long itemId);
}
