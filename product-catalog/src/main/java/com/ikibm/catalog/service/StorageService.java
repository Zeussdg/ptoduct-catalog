package com.ikibm.catalog.service;

import com.ikibm.catalog.config.AppProperties;
import com.ikibm.catalog.exception.ConflictException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Yerel disk tabanlı görsel deposu. Dosyalar yapılandırılabilir bir kök klasöre
 * (varsayılan ./uploads) anahtar şemasıyla yazılır:
 *   products/{productId}/{uuid}-{ad}  ·  campaigns/{campaignId}/{uuid}-{ad}  ·  mail/{templateId}/{uuid}-{ad}
 * URL'ler /uploads/{key} altından servis edilir (bkz. WebConfig resource handler).
 */
@Service
public class StorageService {

    private static final String PUBLIC_BASE = "/uploads";

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final Path root;

    public StorageService(AppProperties appProperties) {
        this.root = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Yükleme klasörü oluşturulamadı: " + root, e);
        }
    }

    public boolean isConfigured() {
        return true;
    }

    public Uploaded uploadProductImage(Integer productId, byte[] bytes, String contentType, String originalName) {
        return put("products/" + productId, bytes, originalName);
    }

    public Uploaded uploadCampaignBanner(String campaignId, byte[] bytes, String contentType, String originalName) {
        return put("campaigns/" + sanitize(campaignId), bytes, originalName);
    }

    /** Mail Gönderme modülü için görsel yükler — uzantı, MIME type ve gerçek dosya imzası
     * (magic bytes) doğrulanır, sadece dosya adına/Content-Type başlığına güvenilmez. */
    public Uploaded uploadMailImage(Integer mailTemplateId, byte[] bytes, String contentType, String originalName) {
        validateImage(bytes, contentType, originalName);
        return put("mail/" + mailTemplateId, bytes, originalName);
    }

    private void validateImage(byte[] bytes, String contentType, String originalName) {
        if (bytes == null || bytes.length == 0) {
            throw new ConflictException("Görsel dosyası boş olamaz");
        }
        String ext = extensionOf(originalName);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new ConflictException("Desteklenmeyen dosya uzantısı — sadece JPEG, PNG, WEBP, GIF yükleyebilirsiniz");
        }
        if (contentType == null || !ALLOWED_IMAGE_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ConflictException("Desteklenmeyen dosya türü — sadece JPEG, PNG, WEBP, GIF yükleyebilirsiniz");
        }
        if (!looksLikeAllowedImage(bytes)) {
            throw new ConflictException("Dosya içeriği geçerli bir görsel gibi görünmüyor");
        }
    }

    private String extensionOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Uzantı/Content-Type başlığı istemciden geldiği için güvenilmez — asıl kanıt dosyanın ilk
     * baytlarındaki gerçek format imzasıdır (ör. "malware.exe"nin ".jpg" olarak yeniden adlandırılmasını
     * bu şekilde yakalarız). */
    private boolean looksLikeAllowedImage(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return true; // JPEG: FF D8 FF
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return true; // PNG: 89 50 4E 47 ...
        }
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8'
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a') {
            return true; // GIF87a / GIF89a
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return true; // RIFF....WEBP
        }
        return false;
    }

    private Uploaded put(String dirKey, byte[] bytes, String originalName) {
        String key = dirKey + "/" + UUID.randomUUID() + "-" + sanitize(originalName);
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new ConflictException("Geçersiz dosya yolu");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new ConflictException("Görsel kaydedilemedi: " + e.getMessage());
        }
        return new Uploaded(key, PUBLIC_BASE + "/" + key);
    }

    public void deleteObject(String key) {
        if (key == null || key.isBlank()) return;
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Dosya silinemezse kırık link yerine sessiz geç (DB satırı ayrıca kaldırılır).
        }
    }

    private String sanitize(String name) {
        return name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record Uploaded(String key, String url) {}
}
