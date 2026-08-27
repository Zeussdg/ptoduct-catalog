package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.entity.CariPaymentMethod;
import com.ikibm.catalog.entity.CariTransactionType;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.CariAccountService;
import com.ikibm.catalog.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequestMapping("/admin/customers/{customerId}/cari")
public class AdminCariController {

    private final CariAccountService cariAccountService;
    private final UserService userService;

    public AdminCariController(CariAccountService cariAccountService, UserService userService) {
        this.cariAccountService = cariAccountService;
        this.userService = userService;
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

    private Instant toInstant(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return null;
        return LocalDate.parse(isoDate).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
