package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.MailTemplateRequest;
import com.ikibm.catalog.entity.MailTemplate;
import com.ikibm.catalog.entity.MailTemplateStatus;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.MailTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Mail şablonlarının CRUD'u — gerçek mail gönderimiyle hiçbir ilgisi yok, sadece şablon verisini
 * (isim/konu/başlık/metin/görsel) veritabanında tutar. Görseller mevcut StorageService (uploads/mail/...)
 * üzerinden diske yazılır; database'de sadece URL/key tutulur, base64 kaydedilmez. */
@Service
public class MailTemplateService {

    private final MailTemplateRepository repository;
    private final StorageService storageService;

    public MailTemplateService(MailTemplateRepository repository, StorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    public List<MailTemplate> list() {
        return repository.findAllByOrderByUpdatedAtDesc();
    }

    public List<MailTemplate> search(String query) {
        if (query == null || query.isBlank()) return list();
        return repository.findByNameContainingIgnoreCaseOrSubjectContainingIgnoreCaseOrderByUpdatedAtDesc(query, query);
    }

    public MailTemplate get(Integer id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Mail şablonu bulunamadı"));
    }

    @Transactional
    public MailTemplate create(MailTemplateRequest req) {
        MailTemplate t = new MailTemplate();
        applyRequest(t, req);
        return repository.save(t);
    }

    @Transactional
    public MailTemplate update(Integer id, MailTemplateRequest req) {
        MailTemplate t = get(id);
        applyRequest(t, req);
        return repository.save(t);
    }

    @Transactional
    public void delete(Integer id) {
        MailTemplate t = get(id);
        if (t.getImageKey() != null) {
            storageService.deleteObject(t.getImageKey());
        }
        repository.delete(t);
    }

    /** Şablonun kendi yüklediği görseli değiştirir — eskisi (varsa) storage'dan silinir, StorageService
     * gerçek dosya doğrulamasını (uzantı/MIME/magic-byte) yapar. Kampanya görseli seçiliyse temizlenir
     * (bir şablon aynı anda hem kendi görselini hem kampanya görselini kullanamaz). */
    @Transactional
    public MailTemplate setImage(Integer id, byte[] bytes, String contentType, String originalName) {
        MailTemplate t = get(id);
        StorageService.Uploaded up = storageService.uploadMailImage(t.getId(), bytes, contentType, originalName);
        String oldKey = t.getImageKey();
        t.setImageKey(up.key());
        t.setImageUrl(up.url());
        t.setCampaignImageUrl(null);
        MailTemplate saved = repository.save(t);
        if (oldKey != null) {
            storageService.deleteObject(oldKey);
        }
        return saved;
    }

    private void applyRequest(MailTemplate t, MailTemplateRequest req) {
        t.setName(req.name());
        t.setSubject(req.subject());
        t.setTitle(req.title());
        t.setContent(req.content());
        t.setStatus(req.status() != null ? req.status() : MailTemplateStatus.ACTIVE);
        t.setCampaignImageUrl(req.campaignImageUrl());
        // Kendi yüklenen görsel (imageUrl/imageKey) SADECE setImage(...) upload endpoint'i üzerinden
        // gerçek bir dosyayla birlikte set edilir. Bu JSON isteği kampanya görseli seçtiyse ya da
        // görsel tamamen kaldırıldıysa (imageUrl=null), eski yüklenmiş dosyayı temizleriz.
        if (req.campaignImageUrl() != null || req.imageUrl() == null) {
            if (t.getImageKey() != null) {
                storageService.deleteObject(t.getImageKey());
            }
            t.setImageUrl(null);
            t.setImageKey(null);
        }
    }
}
