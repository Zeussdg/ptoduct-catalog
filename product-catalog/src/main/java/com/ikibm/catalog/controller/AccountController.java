package com.ikibm.catalog.controller;

import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/account")
    public String account(@AuthenticationPrincipal CatalogUserDetails principal, Model model) {
        model.addAttribute("user", userService.getById(principal.getId()));
        return "public/account";
    }

    @PostMapping("/account")
    public String updateProfile(@AuthenticationPrincipal CatalogUserDetails principal,
                                @RequestParam(required = false) String name,
                                @RequestParam(required = false) String surname,
                                @RequestParam(required = false) String companyName,
                                @RequestParam(required = false) String phone,
                                RedirectAttributes ra) {
        userService.updateProfile(principal.getId(), name, surname, companyName, phone);
        ra.addFlashAttribute("profileMessage", "Profil güncellendi");
        return "redirect:/account";
    }

    @PostMapping("/account/password")
    public String changePassword(@AuthenticationPrincipal CatalogUserDetails principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes ra) {
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("pwError", "Yeni şifreler eşleşmiyor");
            return "redirect:/account";
        }
        if (newPassword.length() < 8) {
            ra.addFlashAttribute("pwError", "Yeni şifre en az 8 karakter olmalı");
            return "redirect:/account";
        }
        try {
            userService.changePassword(principal.getId(), currentPassword, newPassword);
            ra.addFlashAttribute("pwMessage", "Şifre başarıyla değiştirildi");
        } catch (Exception e) {
            ra.addFlashAttribute("pwError", e.getMessage());
        }
        return "redirect:/account";
    }
}
