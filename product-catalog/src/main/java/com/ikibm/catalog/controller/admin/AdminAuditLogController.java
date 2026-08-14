package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.service.AuditLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/admin/audit-logs")
    public String list(Model model) {
        model.addAttribute("logs", auditLogService.list(1).getContent());
        return "admin/audit-logs";
    }
}
