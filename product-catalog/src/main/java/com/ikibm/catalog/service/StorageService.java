package com.ikibm.catalog.service;

import com.ikibm.catalog.config.AppProperties;
import com.ikibm.catalog.exception.ConflictException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Yerel disk tabanlı görsel deposu. Dosyalar yapılandırılabilir bir kök klasöre
 * (varsayılan ./uploads) anahtar şemasıyla yazılır:
 *   products/{productId}/{uuid}-{ad}  ·  campaigns/{campaignId}/{uuid}-{ad}
 * URL'ler /uploads/{key} altından servis edilir (bkz. WebConfig resource handler).
 */
@Service
public class StorageService {

    private static final String PUBLIC_BASE = "/uploads";

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
