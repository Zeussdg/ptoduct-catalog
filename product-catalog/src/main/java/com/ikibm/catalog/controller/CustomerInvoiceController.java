package com.ikibm.catalog.controller;

import com.ikibm.catalog.entity.Invoice;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.InvoicePdfService;
import com.ikibm.catalog.service.InvoiceService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Müşterinin kendi faturalarını görüntülemesi — her sorguda user ownership kontrolü InvoiceService
 * içindeki findByIdAndUser_Id ile yapılır (mevcut CustomerOrderController.getForUser ile aynı desen):
 * başka müşterinin fatura ID'si denenirse 404 döner (403 değil — kaynağın var olduğu bile sızdırılmaz). */
@Controller
public class CustomerInvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    public CustomerInvoiceController(InvoiceService invoiceService, InvoicePdfService invoicePdfService) {
        this.invoiceService = invoiceService;
        this.invoicePdfService = invoicePdfService;
    }

    @GetMapping("/invoices")
    public String list(@AuthenticationPrincipal CatalogUserDetails principal, Model model) {
        model.addAttribute("invoices", invoiceService.listForUser(principal.getId()));
        return "public/invoices";
    }

    @GetMapping("/invoices/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal CatalogUserDetails principal, @PathVariable Integer id) {
        Invoice invoice = invoiceService.getForUser(principal.getId(), id);
        byte[] pdf = invoicePdfService.generate(invoice);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(invoice.getInvoiceNumber() + ".pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
