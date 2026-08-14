package com.ikibm.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikibm.catalog.dto.QuotePdfRequest;
import com.ikibm.catalog.service.PdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class QuotePdfController {

    private final PdfService pdfService;
    private final ObjectMapper objectMapper;

    public QuotePdfController(PdfService pdfService, ObjectMapper objectMapper) {
        this.pdfService = pdfService;
        this.objectMapper = objectMapper;
    }

    /** Drawer'ın "Teklifi PDF'e geçir" gönderimi: payload JSON → indirilebilir PDF. */
    @PostMapping("/quote/pdf")
    public ResponseEntity<byte[]> quotePdf(@RequestParam("payload") String payload) throws Exception {
        QuotePdfRequest req = objectMapper.readValue(payload, QuotePdfRequest.class);
        byte[] pdf = pdfService.generate(req);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("teklif-" + LocalDate.now() + ".pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
