package com.ikibm.catalog.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "mail_templates")
public class MailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Admin'in kendi yüklediği mail görselinin gerçek public URL'i (StorageService.uploadMailImage). */
    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private String imageUrl;

    /** Aynı görselin storage'daki anahtarı — sadece silme/değiştirme için, API'ye dışa aktarılmaz. */
    @Column(name = "image_key", length = 500)
    private String imageKey;

    /** Mevcut kampanya banner'larından seçilen görselin gerçek URL'i (CampaignBannerService). */
    @Column(name = "campaign_image_url", length = 500)
    private String campaignImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MailTemplateStatus status = MailTemplateStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getImageKey() { return imageKey; }
    public void setImageKey(String imageKey) { this.imageKey = imageKey; }
    public String getCampaignImageUrl() { return campaignImageUrl; }
    public void setCampaignImageUrl(String campaignImageUrl) { this.campaignImageUrl = campaignImageUrl; }
    public MailTemplateStatus getStatus() { return status; }
    public void setStatus(MailTemplateStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
