package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.CariAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CariAccountRepository extends JpaRepository<CariAccount, Integer> {

    Optional<CariAccount> findByUser_Id(Integer userId);

    List<CariAccount> findTop10ByOrderByCurrentBalanceDesc();

    /** Kredi limitini aşan hesaplar (dashboard hazırlığı). */
    @Query("select a from CariAccount a where a.creditLimit > 0 and a.currentBalance > a.creditLimit")
    List<CariAccount> findOverLimit();
}
