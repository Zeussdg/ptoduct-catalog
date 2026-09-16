package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer>, JpaSpecificationExecutor<Invoice> {

    boolean existsByOrder_Id(Integer orderId);

    Optional<Invoice> findByOrder_Id(Integer orderId);

    List<Invoice> findByUser_IdOrderByIssuedAtDesc(Integer userId);

    Optional<Invoice> findByIdAndUser_Id(Integer id, Integer userId);

    /** Cari ekstre (detaylı) PDF'i için — birden fazla faturanın kalemlerini TEK sorguda, N+1 oluşturmadan getirir. */
    @Query("select distinct i from Invoice i left join fetch i.items where i.id in :ids")
    List<Invoice> findAllWithItemsByIdIn(@Param("ids") Collection<Integer> ids);
}
