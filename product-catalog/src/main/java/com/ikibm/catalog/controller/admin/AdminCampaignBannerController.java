package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.AuditLogService;
import com.ikibm.catalog.service.CampaignBannerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/campaign-banners")
public class AdminCampaignBannerController {

    private final CampaignBannerService campaignBannerService;
    private final AuditLogService auditLogService;

    public AdminCampaignBannerController(CampaignBannerService campaignBannerService, AuditLogService auditLogService) {
        this.campaignBannerService = campaignBannerService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("campaigns", campaignBannerService.campaignsWithImages());
        return "admin/campaign-banners";
    }

    @PostMapping("/{campaignId}/image")
    public String upload(@PathVariable String campaignId, @RequestParam("image") MultipartFile image,
                         @AuthenticationPrincipal CatalogUserDetails me, RedirectAttributes ra) {
        try {
            if (image.isEmpty()) throw new IllegalArgumentException("Görsel dosyası zorunludur");
            campaignBannerService.setBannerImage(campaignId, image.getBytes(), image.getContentType(), image.getOriginalFilename());
            auditLogService.record(me.getId(), "CAMPAIGN_BANNER_SET", "CampaignBanner", campaignId, null);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/campaign-banners";
    }

    @PostMapping("/{campaignId}/delete")
    public String delete(@PathVariable String campaignId, @AuthenticationPrincipal CatalogUserDetails me,
                         RedirectAttributes ra) {
        try {
            campaignBannerService.deleteBanner(campaignId);
            auditLogService.record(me.getId(), "CAMPAIGN_BANNER_DELETED", "CampaignBanner", campaignId, null);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/campaign-banners";
    }
}
