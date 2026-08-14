package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.AuditLogService;
import com.ikibm.catalog.service.QuoteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/quotes")
public class AdminQuoteController {

    private final QuoteService quoteService;
    private final AuditLogService auditLogService;

    public AdminQuoteController(QuoteService quoteService, AuditLogService auditLogService) {
        this.quoteService = quoteService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status, Model model) {
        model.addAttribute("quotes", quoteService.listAll(status, 1).getContent());
        model.addAttribute("status", status);
        return "admin/quotes";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("quote", quoteService.getByIdAdmin(id));
        return "admin/quote-detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Integer id, @RequestParam String status,
                               @RequestParam(required = false) String adminNote,
                               @AuthenticationPrincipal CatalogUserDetails me) {
        quoteService.updateStatus(id, status, adminNote);
        auditLogService.record(me.getId(), "QUOTE_STATUS_CHANGED", "Quote", String.valueOf(id), null);
        return "redirect:/admin/quotes/" + id;
    }
}
