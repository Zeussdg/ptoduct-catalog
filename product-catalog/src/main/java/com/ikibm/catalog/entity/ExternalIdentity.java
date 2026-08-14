package com.ikibm.catalog.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Bir kullanıcının harici kaynak sistemdeki kimliğine gevşek bağı. Gerçek harici
 * DB henüz bilinmediği için bu tablo şu an kullanılmıyor (iskele — bkz. plan).
 */
@Entity
@Table(name = "external_identities",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source", "external_id"}))
public class ExternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String source;

    @Column(name = "external_id", nullable = false, length = 191)
    private String externalId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
