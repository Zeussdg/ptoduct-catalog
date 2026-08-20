package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.OrderItem;
import com.ikibm.catalog.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    @Query("select oi.currency, sum(oi.totalPrice) from OrderItem oi " +
            "where oi.order.status <> :excludedStatus group by oi.currency")
    List<Object[]> sumAmountByCurrency(@Param("excludedStatus") OrderStatus excludedStatus);
}
