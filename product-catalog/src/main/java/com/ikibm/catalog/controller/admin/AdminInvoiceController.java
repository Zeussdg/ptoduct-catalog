package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.entity.Invoice;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.AuditLogService;
import com.ikibm.catalog.service.InvoicePdfService;
import com.ikibm.catalog.service.InvoiceService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Fatura oluşturma/görüntüleme/iptal — mevcut /admin/** yetkilendirme kuralına (ADMIN+SUPER_ADMIN)
 * tabidir, SecurityConfig'e hiçbir yeni kural eklenmedi (genel kural zaten kapsıyor). */
@Controller
public class AdminInvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final AuditLogService auditLogService;

    public AdminInvoiceController(InvoiceService invoiceService, InvoicePdfService invoicePdfService,
                                  AuditLogService auditLogService) {
        this.invoiceService = invoiceService;
        this.invoicePdfService = invoicePdfService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/admin/orders/{orderId}/invoice")
    public String create(@PathVariable Integer orderId, @AuthenticationPrincipal CatalogUserDetails me, RedirectAttributes ra) {
        try {
            Invoice invoice = invoiceService.createFromOrder(orderId, me.getId());
            auditLogService.record(me.getId(), "INVOICE_CREATED", "Invoice", String.valueOf(invoice.getId()), null);
            ra.addFlashAttribute("message", "Fatura oluşturuldu: " + invoice.getInvoiceNumber());
        } catch (ConflictException | NotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + orderId;
    }

    @PostMapping("/admin/invoices/{id}/cancel")
    public String cancel(@PathVariable Integer id, @AuthenticationPrincipal CatalogUserDetails me, RedirectAttributes ra) {
        Invoice invoice = invoiceService.getByIdAdmin(id);
        Integer orderId = invoice.getOrder().getId();
        try {
            invoiceService.cancel(id, me.getId());
            auditLogService.record(me.getId(), "INVOICE_CANCELLED", "Invoice", String.valueOf(id), null);
            ra.addFlashAttribute("message", "Fatura iptal edildi");
        } catch (ConflictException | NotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + orderId;
    }

    @GetMapping("/admin/invoices/{id}/pdf")
    public ResponseEntity<byte[]> viewPdf(@PathVariable Integer id) {
        Invoice invoice = invoiceService.getByIdAdmin(id);
        return pdfResponse(invoice, ContentDisposition.inline());
    }

    @GetMapping("/admin/invoices/{id}/pdf/download")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Integer id) {
        Invoice invoice = invoiceService.getByIdAdmin(id);
        return pdfResponse(invoice, ContentDisposition.attachment());
    }

    private ResponseEntity<byte[]> pdfResponse(Invoice invoice, ContentDisposition.Builder disposition) {
        byte[] pdf = invoicePdfService.generate(invoice);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(disposition.filename(invoice.getInvoiceNumber() + ".pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
