package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.Quote;
import com.ikibm.catalog.entity.QuoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Integer> {

    List<Quote> findByUser_IdOrderByCreatedAtDesc(Integer userId);

    Optional<Quote> findByIdAndUser_Id(Integer id, Integer userId);

    Page<Quote> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Quote> findByStatusOrderByCreatedAtDesc(QuoteStatus status, Pageable pageable);

    List<Quote> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(QuoteStatus status);
}
