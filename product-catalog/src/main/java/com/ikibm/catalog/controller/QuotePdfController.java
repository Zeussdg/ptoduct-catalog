package com.ikibm.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikibm.catalog.dto.QuotePdfRequest;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.PdfService;
import com.ikibm.catalog.service.QuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class QuotePdfController {

    private static final Logger log = LoggerFactory.getLogger(QuotePdfController.class);

    private final PdfService pdfService;
    private final QuoteService quoteService;
    private final ObjectMapper objectMapper;

    public QuotePdfController(PdfService pdfService, QuoteService quoteService, ObjectMapper objectMapper) {
        this.pdfService = pdfService;
        this.quoteService = quoteService;
        this.objectMapper = objectMapper;
    }

    /** Drawer'ın "Teklifi PDF'e geçir" gönderimi: payload JSON → indirilebilir PDF + quotes kaydı. */
    @PostMapping("/quote/pdf")
    public ResponseEntity<byte[]> quotePdf(@RequestParam("payload") String payload,
                                            @AuthenticationPrincipal CatalogUserDetails principal) throws Exception {
        QuotePdfRequest req = objectMapper.readValue(payload, QuotePdfRequest.class);
        byte[] pdf = pdfService.generate(req);

        try {
            quoteService.createFromPdfRequest(req, principal != null ? principal.getUser() : null);
        } catch (Exception e) {
            log.warn("PDF teklifi quotes tablosuna kaydedilemedi, PDF yine de döndürülüyor", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("teklif-" + LocalDate.now() + ".pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
