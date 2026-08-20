package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.AuditLogService;
import com.ikibm.catalog.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final AuditLogService auditLogService;

    public AdminOrderController(OrderService orderService, AuditLogService auditLogService) {
        this.orderService = orderService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status, Model model) {
        model.addAttribute("orders", orderService.listAll(status, 1).getContent());
        model.addAttribute("status", status);
        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("order", orderService.getByIdAdmin(id));
        return "admin/order-detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Integer id, @RequestParam String status,
                               @RequestParam(required = false) String adminNote,
                               @RequestParam(required = false) String carrierName,
                               @RequestParam(required = false) String trackingNumber,
                               @AuthenticationPrincipal CatalogUserDetails me) {
        orderService.updateStatus(id, status, adminNote, carrierName, trackingNumber);
        auditLogService.record(me.getId(), "ORDER_STATUS_CHANGED", "Order", String.valueOf(id), null);
        return "redirect:/admin/orders/" + id;
    }
}
