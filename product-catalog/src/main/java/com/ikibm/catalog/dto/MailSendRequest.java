package com.ikibm.catalog.dto;

import java.util.List;

/** Admin panelindeki "Mail Gönderme" ekranından gelen tek seferlik gönderim isteği — kalıcı bir
 * kayıt oluşturmaz, sadece MailService.send(...) çağrısının girdisidir. */
public record MailSendRequest(
        String recipientType,       // "ALL" | "SELECTED" (mail.html'deki mailRecipients radio değerleriyle birebir)
        List<Integer> customerIds,  // recipientType == "SELECTED" iken kullanılır
        String subject,
        String title,
        String content,
        String imageUrl             // mevcut StorageService/CampaignBanner tarafından üretilmiş gerçek public URL — opsiyonel
) {
}
