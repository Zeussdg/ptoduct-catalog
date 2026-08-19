package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.QuoteItem;
import com.ikibm.catalog.entity.QuoteStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuoteItemRepository extends JpaRepository<QuoteItem, Integer> {

    @Query("select qi.currency, sum(qi.totalPrice) from QuoteItem qi " +
            "where qi.quote.status <> :excludedStatus group by qi.currency")
    List<Object[]> sumAmountByCurrency(@Param("excludedStatus") QuoteStatus excludedStatus);

    @Query("select qi.productName, qi.productCode, count(qi), sum(qi.qty) from QuoteItem qi " +
            "group by qi.productName, qi.productCode order by sum(qi.qty) desc")
    List<Object[]> topProducts(Pageable pageable);

    @Query("select qi.quote.user.id, qi.currency, sum(qi.totalPrice) from QuoteItem qi " +
            "where qi.quote.user is not null group by qi.quote.user.id, qi.currency")
    List<Object[]> sumAmountByUserAndCurrency();

    @Query("select qi.currency, sum(qi.totalPrice) from QuoteItem qi where qi.quote.user is null group by qi.currency")
    List<Object[]> sumAmountForGuestsByCurrency();
}
