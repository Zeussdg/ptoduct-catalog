package com.ikibm.catalog.service;

import com.ikibm.catalog.config.AppProperties;
import com.ikibm.catalog.dto.MailSendRequest;
import com.ikibm.catalog.dto.MailSendResult;
import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin panelindeki "Mail Gönderme" ekranından tetiklenen tek seferlik gerçek mail gönderimi —
 * Resend REST API'sine (https://api.resend.com/emails) doğrudan HTTP isteğiyle bağlanır, resmi
 * SDK gerekmez (proje zaten spring-boot-restclient'a sahip). Kalıcı bir gönderim geçmişi TUTMAZ;
 * her çağrı anlık, bağımsız bir işlemdir. Her alıcıya AYRI bir istek gönderilir (CC/BCC KULLANILMAZ)
 * — böylece müşteriler birbirinin e-posta adresini asla görmez.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final String GENERIC_ERROR = "Mail gönderilemedi. Lütfen tekrar deneyin.";
    /** Resend'in content_id özelliğiyle gömülen görsele mail HTML'inde <img src="cid:..."> ile referans
     * vermek için kullanılan sabit kimlik — tek bir mailde tek görsel olduğundan sabit olması yeterli. */
    private static final String IMAGE_CID = "mail-image";

    private final RestClient restClient;
    private final AppProperties.Resend config;
    private final UserRepository userRepository;
    private final UserService userService;
    private final StorageService storageService;

    public MailService(RestClient.Builder restClientBuilder, AppProperties appProperties,
                        UserRepository userRepository, UserService userService, StorageService storageService) {
        this.config = appProperties.getResend();
        this.restClient = restClientBuilder.baseUrl("https://api.resend.com").build();
        this.userRepository = userRepository;
        this.userService = userService;
        this.storageService = storageService;
    }

    public MailSendResult send(MailSendRequest req) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException("Mail gönderme servisi yapılandırılmamış (RESEND_API_KEY tanımlı değil).");
        }
        if (req.subject() == null || req.subject().isBlank()) {
            throw new IllegalArgumentException("Mail konusu boş olamaz");
        }
        if (req.content() == null || req.content().isBlank()) {
            throw new IllegalArgumentException("Mail metni boş olamaz");
        }

        List<User> recipients = resolveRecipients(req);
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("Gönderilecek müşteri bulunamadı (seçilen müşterilerin e-posta adresi yok)");
        }

        EmbeddedImage image = resolveImage(req.imageUrl());
        String html = buildHtml(req.title(), req.content(), image != null);

        int sent = 0;
        int failed = 0;
        for (User recipient : recipients) {
            try {
                sendOne(recipient.getEmail(), req.subject(), html, image);
                sent++;
            } catch (Exception e) {
                failed++;
                log.warn("Mail gönderilemedi: alıcı id={}", recipient.getId(), e);
            }
        }

        if (sent == 0) {
            throw new IllegalStateException(GENERIC_ERROR);
        }
        String message = failed == 0
                ? (sent == 1 ? "Mail başarıyla gönderildi." : sent + " müşteriye mail başarıyla gönderildi.")
                : sent + " müşteriye gönderildi, " + failed + " müşteriye gönderilemedi.";
        return new MailSendResult(sent, failed, message);
    }

    private List<User> resolveRecipients(MailSendRequest req) {
        String type = req.recipientType() == null ? "" : req.recipientType();
        List<User> candidates = switch (type) {
            case "ALL" -> userService.listCustomers();
            case "SELECTED" -> {
                if (req.customerIds() == null || req.customerIds().isEmpty()) {
                    throw new IllegalArgumentException("Gönderilecek müşteri seçilmedi");
                }
                yield userRepository.findAllById(req.customerIds());
            }
            default -> throw new IllegalArgumentException("Bu alıcı türü için gönderim henüz desteklenmiyor");
        };
        return candidates.stream().filter(u -> u.getEmail() != null && !u.getEmail().isBlank()).toList();
    }

    /** mail.html'deki canlı önizlemeyle (görsel + başlık + metin) aynı sırayı izleyen sade bir e-posta
     * gövdesi — metin kullanıcıdan geldiği için HtmlUtils.htmlEscape ile kaçışlanır, satır sonları <br>'a çevrilir.
     * Görsel varsa data: URI DEĞİL, cid: referansı kullanılır (bkz. resolveImage/sendOne) — birçok mail
     * istemcisi (özellikle masaüstü Outlook) base64 data URI'leri güvenlik gerekçesiyle engeller, ama
     * Resend'in content_id ekli tam MIME eki olarak gönderilen görseller (cid:) evrensel desteklenir. */
    private String buildHtml(String title, String content, boolean hasImage) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:600px;margin:0 auto\">");
        if (hasImage) {
            sb.append("<img src=\"cid:").append(IMAGE_CID)
                    .append("\" alt=\"\" style=\"max-width:100%;border-radius:6px;margin-bottom:16px;display:block\"/>");
        }
        if (title != null && !title.isBlank()) {
            sb.append("<h2 style=\"margin:0 0 12px;color:#111827;font-size:18px\">")
                    .append(HtmlUtils.htmlEscape(title)).append("</h2>");
        }
        sb.append("<div style=\"color:#374151;font-size:14px;line-height:1.6\">")
                .append(HtmlUtils.htmlEscape(content).replace("\n", "<br>"))
                .append("</div></div>");
        return sb.toString();
    }

    /** StorageService'in yazdığı dosyayı (site-göreli "/uploads/..." URL üzerinden) diskten okur —
     * mail HTML'inde bir <img src="cid:..."> ile referans verilecek, gerçek bayt içeriği Resend'e
     * attachments[].content olarak (content_id ile) gönderilecektir. Dosya bulunamazsa görsel sessizce
     * atlanır (kırık görsel yerine görsel olmadan gönderim tercih edilir). */
    private EmbeddedImage resolveImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        byte[] bytes = storageService.readByPublicUrl(imageUrl);
        if (bytes == null) {
            log.warn("Mail görseli atlandı: '{}' diskte bulunamadı", imageUrl);
            return null;
        }
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        return new EmbeddedImage(bytes, filename, guessMimeType(imageUrl));
    }

    private String guessMimeType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private void sendOne(String to, String subject, String html, EmbeddedImage image) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", config.getFrom());
        body.put("to", List.of(to));
        body.put("subject", subject);
        body.put("html", html);
        if (image != null) {
            body.put("attachments", List.of(Map.of(
                    "filename", image.filename(),
                    "content", Base64.getEncoder().encodeToString(image.bytes()),
                    "content_type", image.contentType(),
                    "content_id", IMAGE_CID
            )));
        }
        try {
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // e.getResponseBodyAsString() Resend'in KENDİ hata mesajıdır (bizim Authorization header'ımızı
            // hiçbir zaman içermez) — teşhis için loglamak güvenlidir, API key asla loglanmaz.
            log.warn("Resend API hata döndürdü: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new RuntimeException("Resend API hata döndürdü: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.warn("Resend API isteği başarısız oldu (ağ/timeout)", e);
            throw new RuntimeException("Resend API isteğine ulaşılamadı", e);
        }
    }

    private record EmbeddedImage(byte[] bytes, String filename, String contentType) {}
}
