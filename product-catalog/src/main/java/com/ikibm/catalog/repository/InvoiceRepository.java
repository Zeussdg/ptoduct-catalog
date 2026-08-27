package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    boolean existsByOrder_Id(Integer orderId);

    Optional<Invoice> findByOrder_Id(Integer orderId);

    List<Invoice> findByUser_IdOrderByIssuedAtDesc(Integer userId);

    Optional<Invoice> findByIdAndUser_Id(Integer id, Integer userId);
}
