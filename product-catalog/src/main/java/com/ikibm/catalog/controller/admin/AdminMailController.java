package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.dto.MailSendRequest;
import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.service.CampaignBannerService;
import com.ikibm.catalog.service.MailService;
import com.ikibm.catalog.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/** Mail Gönderme — sayfa (Thymeleaf) + şablon CRUD'u değişmeden kalır (bkz. AdminMailTemplateController).
 * Bu controller'a SADECE gerçek gönderim için iki JSON endpoint'i eklendi: alıcı seçimi için gerçek
 * müşteri listesi ve Resend üzerinden gönderimi tetikleyen /send. Yetkilendirme mevcut
 * SecurityConfig kuralına tabidir (/admin/** -> ADMIN veya SUPER_ADMIN), ayrı bir değişiklik gerekmez. */
@Controller
@RequestMapping("/admin/mail")
public class AdminMailController {

    private static final Logger log = LoggerFactory.getLogger(AdminMailController.class);

    private final CampaignBannerService campaignBannerService;
    private final UserService userService;
    private final MailService mailService;

    public AdminMailController(CampaignBannerService campaignBannerService, UserService userService,
                                MailService mailService) {
        this.campaignBannerService = campaignBannerService;
        this.userService = userService;
        this.mailService = mailService;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("campaigns", campaignBannerService.campaignsWithImages());
        return "admin/mail";
    }

    /** "Seçili Müşteriler" listesini gerçek verilerle doldurmak için — mevcut UserService.listCustomers()
     * (zaten Cari/Fatura ekranlarında kullanılan aynı sorgu) tekrar kullanılır, yeni bir repository
     * metodu gerekmez. */
    @GetMapping("/customers")
    @ResponseBody
    public List<Map<String, Object>> customers() {
        return userService.listCustomers().stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .map(this::toOption)
                .toList();
    }

    private Map<String, Object> toOption(User u) {
        return Map.of("id", u.getId(), "displayName", u.getDisplayName(), "email", u.getEmail());
    }

    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> send(@RequestBody MailSendRequest request) {
        try {
            var result = mailService.send(request);
            return ResponseEntity.ok(Map.of("message", result.message(), "sent", result.sent(), "failed", result.failed()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Mail gönderimi başarısız oldu", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Mail gönderilemedi. Lütfen tekrar deneyin."));
        }
    }
}
