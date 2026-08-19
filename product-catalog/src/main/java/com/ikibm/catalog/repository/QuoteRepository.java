package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.Quote;
import com.ikibm.catalog.entity.QuoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Integer> {

    List<Quote> findByUser_IdOrderByCreatedAtDesc(Integer userId);

    Optional<Quote> findByIdAndUser_Id(Integer id, Integer userId);

    Page<Quote> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Quote> findByStatusOrderByCreatedAtDesc(QuoteStatus status, Pageable pageable);

    List<Quote> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(QuoteStatus status);

    long countByStatusIn(Collection<QuoteStatus> statuses);

    long countByCreatedAtGreaterThanEqual(Instant from);

    long countByUserIsNull();

    @Query("select q.status, count(q) from Quote q group by q.status")
    List<Object[]> countGroupByStatus();

    @Query("select extract(year from q.createdAt), extract(month from q.createdAt), count(q) " +
            "from Quote q where q.createdAt >= :from " +
            "group by extract(year from q.createdAt), extract(month from q.createdAt)")
    List<Object[]> countGroupByYearMonth(@Param("from") Instant from);

    @Query("select q.user.id, count(q) from Quote q where q.user is not null group by q.user.id order by count(q) desc")
    List<Object[]> countGroupByUser(Pageable pageable);
}
