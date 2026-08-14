package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.AuditLogService;
import com.ikibm.catalog.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public AdminUserController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String role, Model model) {
        model.addAttribute("users", userService.listUsers(role));
        model.addAttribute("roleFilter", role);
        return "admin/users";
    }

    @PostMapping
    public String create(@RequestParam String email, @RequestParam(required = false) String username,
                         @RequestParam String password, @RequestParam String role,
                         @RequestParam(required = false) String name, @RequestParam(required = false) String surname,
                         @AuthenticationPrincipal CatalogUserDetails me, RedirectAttributes ra) {
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            ra.addFlashAttribute("error", "Geçerli bir e-posta adresi girin");
            return "redirect:/admin/users";
        }
        if (password == null || password.length() < 8) {
            ra.addFlashAttribute("error", "Şifre en az 8 karakter olmalı");
            return "redirect:/admin/users";
        }
        try {
            User u = userService.createUser(email, username, password, role, name, surname);
            auditLogService.record(me.getId(), "USER_CREATED", "User", String.valueOf(u.getId()), null);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/role")
    public String updateRole(@PathVariable Integer id, @RequestParam String role,
                             @AuthenticationPrincipal CatalogUserDetails me, RedirectAttributes ra) {
        if (id.equals(me.getId())) { ra.addFlashAttribute("error", "Kendi rolünüzü değiştiremezsiniz"); return "redirect:/admin/users"; }
        userService.updateRole(id, role);
        auditLogService.record(me.getId(), "USER_ROLE_CHANGED", "User", String.valueOf(id), null);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Integer id, @RequestParam String status,
                               @AuthenticationPrincipal CatalogUserDetails me, RedirectAttributes ra) {
        if (id.equals(me.getId())) { ra.addFlashAttribute("error", "Kendi durumunuzu değiştiremezsiniz"); return "redirect:/admin/users"; }
        userService.updateStatus(id, status);
        auditLogService.record(me.getId(), "USER_STATUS_CHANGED", "User", String.valueOf(id), null);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, @AuthenticationPrincipal CatalogUserDetails me,
                         RedirectAttributes ra) {
        if (id.equals(me.getId())) { ra.addFlashAttribute("error", "Kendi hesabınızı silemezsiniz"); return "redirect:/admin/users"; }
        userService.deleteUser(id);
        auditLogService.record(me.getId(), "USER_DELETED", "User", String.valueOf(id), null);
        return "redirect:/admin/users";
    }
}
