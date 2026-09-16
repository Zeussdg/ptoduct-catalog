package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.dto.CariStatementData;
import com.ikibm.catalog.entity.CariPaymentMethod;
import com.ikibm.catalog.entity.CariTransactionType;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.CariAccountService;
import com.ikibm.catalog.service.CariStatementPdfService;
import com.ikibm.catalog.service.CariStatementService;
import com.ikibm.catalog.service.UserService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/admin/customers/{customerId}/cari")
public class AdminCariController {

    private final CariAccountService cariAccountService;
    private final UserService userService;
    private final CariStatementService cariStatementService;
    private final CariStatementPdfService cariStatementPdfService;

    public AdminCariController(CariAccountService cariAccountService, UserService userService,
                               CariStatementService cariStatementService, CariStatementPdfService cariStatementPdfService) {
        this.cariAccountService = cariAccountService;
        this.userService = userService;
        this.cariStatementService = cariStatementService;
        this.cariStatementPdfService = cariStatementPdfService;
    }

    @GetMapping
    public String detail(@PathVariable Integer customerId, Model model) {
        var customer = userService.getById(customerId);
        var account = cariAccountService.getOrCreateForUser(customerId);
        model.addAttribute("customer", customer);
        model.addAttribute("account", account);
        model.addAttribute("transactions", cariAccountService.history(account.getId()));
        model.addAttribute("availableLimit", cariAccountService.availableLimit(account.getId()));
        model.addAttribute("overdueTotal", cariAccountService.overdueTotal(account.getId()));
        model.addAttribute("upcomingDueTotal", cariAccountService.upcomingDueTotal(account.getId(), 30));
        model.addAttribute("balanceBreakdown", cariAccountService.balanceBreakdownByCurrency(account.getId()));
        model.addAttribute("paymentMethods", CariPaymentMethod.values());
        model.addAttribute("currencies", Currency.values());
        return "admin/cari-detail";
    }

    @PostMapping("/payments")
    public String recordPayment(@PathVariable Integer customerId,
            @RequestParam BigDecimal amount, @RequestParam Currency currency,
            @RequestParam String paymentDate, @RequestParam CariPaymentMethod method,
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal CatalogUserDetails principal, RedirectAttributes ra) {
        try {
            cariAccountService.recordPayment(customerId, amount, currency, toInstant(paymentDate), method,
                    description, principal.getId());
            ra.addFlashAttribute("message", "Ödeme kaydedildi");
        } catch (ConflictException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/customers/" + customerId + "/cari";
    }

    @PostMapping("/transactions")
    public String addTransaction(@PathVariable Integer customerId,
            @RequestParam CariTransactionType type, @RequestParam BigDecimal amount, @RequestParam Currency currency,
            @RequestParam String transactionDate, @RequestParam(required = false) String dueDate,
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal CatalogUserDetails principal, RedirectAttributes ra) {
        try {
            cariAccountService.addManualTransaction(customerId, type, amount, currency, toInstant(transactionDate),
                    toInstant(dueDate), description, principal.getId());
            ra.addFlashAttribute("message", "Cari hareket eklendi");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/customers/" + customerId + "/cari";
    }

    /** Cari ekstre PDF'i — mevcut AdminInvoiceController'daki view/download endpoint çifti deseniyle
     * aynı: tek bir private yardımcı, ContentDisposition farkıyla iki GET endpoint'i. Yetkilendirme
     * mevcut /admin/** kuralına tabidir, ayrıca bir SecurityConfig değişikliği gerekmiyor. */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> viewStatementPdf(@PathVariable Integer customerId,
            @RequestParam(defaultValue = "summary") String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        return statementPdfResponse(customerId, type, from, to, ContentDisposition.inline());
    }

    @GetMapping("/pdf/download")
    public ResponseEntity<byte[]> downloadStatementPdf(@PathVariable Integer customerId,
            @RequestParam(defaultValue = "summary") String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        return statementPdfResponse(customerId, type, from, to, ContentDisposition.attachment());
    }

    private ResponseEntity<byte[]> statementPdfResponse(Integer customerId, String type, String from, String to,
            ContentDisposition.Builder disposition) {
        User customer = userService.getById(customerId);
        boolean detailed = "detailed".equalsIgnoreCase(type);
        Instant fromInstant = toInstant(from);
        Instant toExclusive = toInstant(to);
        if (toExclusive != null) {
            toExclusive = toExclusive.plus(1, ChronoUnit.DAYS);
        }

        CariStatementData data = cariStatementService.build(customer, detailed, fromInstant, toExclusive);
        byte[] pdf = cariStatementPdfService.generate(data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = "cari-ekstre-" + customerId + (detailed ? "-detayli" : "-ozet") + ".pdf";
        headers.setContentDisposition(disposition.filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    private Instant toInstant(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return null;
        return LocalDate.parse(isoDate).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
