package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.AuditLogService;
import com.ikibm.catalog.service.ExcelImportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/import")
public class AdminImportController {

    private final ExcelImportService excelImportService;
    private final AuditLogService auditLogService;

    public AdminImportController(ExcelImportService excelImportService, AuditLogService auditLogService) {
        this.excelImportService = excelImportService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String form() {
        return "admin/import";
    }

    @PostMapping
    public String upload(@RequestParam("file") MultipartFile file,
                         @AuthenticationPrincipal CatalogUserDetails me, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Excel dosyası seçilmedi");
            return "redirect:/admin/import";
        }
        try {
            ExcelImportService.Report report = excelImportService.importFrom(file.getInputStream(), file.getOriginalFilename());
            ra.addFlashAttribute("report", report);
            if (report.ok()) {
                auditLogService.record(me.getId(), "PRODUCTS_IMPORTED", "Product", null, null);
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "İçe aktarma başarısız: " + e.getMessage());
        }
        return "redirect:/admin/import";
    }
}
