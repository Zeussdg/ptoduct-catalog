package com.ikibm.catalog.dto;

import com.ikibm.catalog.entity.MailTemplate;
import com.ikibm.catalog.entity.MailTemplateStatus;

import java.time.Instant;

public record MailTemplateResponse(
        Integer id,
        String name,
        String subject,
        String title,
        String content,
        String imageUrl,
        String campaignImageUrl,
        MailTemplateStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static MailTemplateResponse of(MailTemplate t) {
        return new MailTemplateResponse(t.getId(), t.getName(), t.getSubject(), t.getTitle(), t.getContent(),
                t.getImageUrl(), t.getCampaignImageUrl(), t.getStatus(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
