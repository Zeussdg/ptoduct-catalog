package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.service.CampaignBannerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Mail Gönderme — şimdilik tamamen frontend/UI: gerçek mail gönderimi, şablon veritabanı veya
 * müşteri sorgusu yok. Şablonlar tarayıcıda (localStorage) tutulur, "Mail Gönder" pasiftir. */
@Controller
@RequestMapping("/admin/mail")
public class AdminMailController {

    private final CampaignBannerService campaignBannerService;

    public AdminMailController(CampaignBannerService campaignBannerService) {
        this.campaignBannerService = campaignBannerService;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("campaigns", campaignBannerService.campaignsWithImages());
        return "admin/mail";
    }
}
