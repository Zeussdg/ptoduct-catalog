package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.dto.MailTemplateRequest;
import com.ikibm.catalog.dto.MailTemplateResponse;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.service.MailTemplateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Mail şablonları için JSON REST API — projedeki ilk tam CRUD JSON kaynağı olduğundan, paylaşılan
 * (HTML odaklı) GlobalExceptionHandler'a dokunmadan hatalar burada JSON'a çevrilir. Gerçek mail
 * gönderimiyle hiçbir ilgisi yok, sadece şablon verisinin database ile CRUD'unu sağlar. */
@RestController
@RequestMapping("/admin/mail-templates")
public class AdminMailTemplateController {

    private static final Logger log = LoggerFactory.getLogger(AdminMailTemplateController.class);

    private final MailTemplateService mailTemplateService;

    public AdminMailTemplateController(MailTemplateService mailTemplateService) {
        this.mailTemplateService = mailTemplateService;
    }

    @GetMapping
    public List<MailTemplateResponse> list() {
        return mailTemplateService.list().stream().map(MailTemplateResponse::of).toList();
    }

    @GetMapping("/search")
    public List<MailTemplateResponse> search(@RequestParam(required = false) String query) {
        return mailTemplateService.search(query).stream().map(MailTemplateResponse::of).toList();
    }

    @GetMapping("/{id}")
    public MailTemplateResponse get(@PathVariable Integer id) {
        return MailTemplateResponse.of(mailTemplateService.get(id));
    }

    @PostMapping
    public ResponseEntity<MailTemplateResponse> create(@Valid @RequestBody MailTemplateRequest request) {
        MailTemplateResponse created = MailTemplateResponse.of(mailTemplateService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public MailTemplateResponse update(@PathVariable Integer id, @Valid @RequestBody MailTemplateRequest request) {
        return MailTemplateResponse.of(mailTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        mailTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Şablonun kendi görselini yükler/değiştirir — mevcut StorageService (uploads/mail/{id}/...)
     * kullanılır, eski görsel (varsa) otomatik silinir, kampanya görseli seçimi temizlenir. */
    @PostMapping("/{id}/image")
    public MailTemplateResponse uploadImage(@PathVariable Integer id, @RequestParam("image") MultipartFile image) {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Görsel dosyası zorunludur");
        }
        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Görsel okunamadı");
        }
        return MailTemplateResponse.of(mailTemplateService.setImage(id, bytes, image.getContentType(), image.getOriginalFilename()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getDefaultMessage())
                .orElse("Geçersiz veri");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    @ExceptionHandler({ConflictException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "Dosya çok büyük — en fazla 5MB yükleyebilirsiniz"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        log.error("Mail şablonu işlenemedi", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Mail şablonu işlenemedi"));
    }
}
