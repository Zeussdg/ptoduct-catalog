package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.Order;
import com.ikibm.catalog.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByUser_IdOrderByCreatedAtDesc(Integer userId);

    Optional<Order> findByIdAndUser_Id(Integer id, Integer userId);

    Optional<Order> findByQuote_Id(Integer quoteId);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    List<Order> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(OrderStatus status);

    long countByCreatedAtGreaterThanEqual(Instant from);

    @Query("select o.status, count(o) from Order o group by o.status")
    List<Object[]> countGroupByStatus();

    /** Cari ekstre (detaylı) PDF'i için — birden fazla siparişin kalemlerini TEK sorguda, N+1 oluşturmadan getirir. */
    @Query("select distinct o from Order o left join fetch o.items where o.id in :ids")
    List<Order> findAllWithItemsByIdIn(@Param("ids") Collection<Integer> ids);
}
