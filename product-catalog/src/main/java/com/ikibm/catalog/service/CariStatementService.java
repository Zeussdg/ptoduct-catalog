package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.CariStatementData;
import com.ikibm.catalog.dto.CariStatementLine;
import com.ikibm.catalog.entity.CariAccount;
import com.ikibm.catalog.entity.CariReferenceType;
import com.ikibm.catalog.entity.CariTransaction;
import com.ikibm.catalog.entity.CariTransactionType;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.Invoice;
import com.ikibm.catalog.entity.Order;
import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.repository.InvoiceRepository;
import com.ikibm.catalog.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cari ekstre (PDF) için mevcut cari hesap verisini orkestre eder — CariAccountService'e (bakiye/hareket
 * hesaplama mantığı) HİÇBİR ŞEKİLDE dokunmaz, sadece history()'i tekrar kullanır ve (detaylı modda)
 * ORDER/INVOICE referanslarını toplu (N+1'siz) çözer. Yeni bir bakiye/borç hesaplama mantığı YOKTUR —
 * Toplam Borç/Alacak, zaten var olan CariTransaction.amount değerlerinin (cari-detail.html'deki Borç/
 * Alacak kolon ayrımıyla birebir aynı kritere göre) filtrelenmiş kümedeki toplamıdır.
 */
@Service
public class CariStatementService {

    private final CariAccountService cariAccountService;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;

    public CariStatementService(CariAccountService cariAccountService, OrderRepository orderRepository,
                                InvoiceRepository invoiceRepository) {
        this.cariAccountService = cariAccountService;
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
    }

    /** from/to null olabilir (filtresiz = tüm geçmiş). to VERİLMİŞSE ÇAĞIRAN TARAF üst sınırı hariç
     * (bir sonraki günün başlangıcı) olarak geçmelidir — bkz. AdminCariController. */
    public CariStatementData build(User customer, boolean detailed, Instant from, Instant to) {
        CariAccount account = cariAccountService.getOrCreateForUser(customer.getId());
        List<CariTransaction> all = cariAccountService.history(account.getId());

        List<CariTransaction> filtered = all.stream()
                .filter(t -> from == null || !t.getTransactionDate().isBefore(from))
                .filter(t -> to == null || t.getTransactionDate().isBefore(to))
                .toList();

        List<CariStatementLine> lines = detailed ? resolveReferences(filtered) : plainLines(filtered);

        Map<Currency, BigDecimal> debtByCurrency = new EnumMap<>(Currency.class);
        Map<Currency, BigDecimal> creditByCurrency = new EnumMap<>(Currency.class);
        for (CariTransaction t : filtered) {
            if (t.getType() == CariTransactionType.DEBT || t.getType() == CariTransactionType.ADJUSTMENT) {
                debtByCurrency.merge(t.getCurrency(), t.getAmount(), BigDecimal::add);
            } else {
                creditByCurrency.merge(t.getCurrency(), t.getAmount(), BigDecimal::add);
            }
        }

        return new CariStatementData(customer, account, Instant.now(), from, to, detailed, lines, debtByCurrency, creditByCurrency);
    }

    private List<CariStatementLine> plainLines(List<CariTransaction> transactions) {
        return transactions.stream().map(t -> new CariStatementLine(t, null, null)).toList();
    }

    private List<CariStatementLine> resolveReferences(List<CariTransaction> transactions) {
        List<Integer> orderIds = transactions.stream()
                .filter(t -> t.getReferenceType() == CariReferenceType.ORDER)
                .map(CariTransaction::getReferenceId).distinct().toList();
        List<Integer> invoiceIds = transactions.stream()
                .filter(t -> t.getReferenceType() == CariReferenceType.INVOICE)
                .map(CariTransaction::getReferenceId).distinct().toList();

        Map<Integer, Order> ordersById = orderIds.isEmpty() ? Map.of()
                : orderRepository.findAllWithItemsByIdIn(orderIds).stream()
                        .collect(Collectors.toMap(Order::getId, Function.identity()));
        Map<Integer, Invoice> invoicesById = invoiceIds.isEmpty() ? Map.of()
                : invoiceRepository.findAllWithItemsByIdIn(invoiceIds).stream()
                        .collect(Collectors.toMap(Invoice::getId, Function.identity()));

        return transactions.stream().map(t -> new CariStatementLine(t,
                t.getReferenceType() == CariReferenceType.ORDER ? ordersById.get(t.getReferenceId()) : null,
                t.getReferenceType() == CariReferenceType.INVOICE ? invoicesById.get(t.getReferenceId()) : null
        )).toList();
    }
}
