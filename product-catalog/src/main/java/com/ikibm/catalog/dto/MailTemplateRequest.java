package com.ikibm.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ikibm.catalog.entity.MailTemplateStatus;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MailTemplateRequest(
        @NotBlank(message = "Şablon adı zorunludur") String name,
        @NotBlank(message = "Mail konusu zorunludur") String subject,
        @NotBlank(message = "Mail başlığı zorunludur") String title,
        @NotBlank(message = "Mail metni zorunludur") String content,
        String imageUrl,
        String campaignImageUrl,
        MailTemplateStatus status) {
}
