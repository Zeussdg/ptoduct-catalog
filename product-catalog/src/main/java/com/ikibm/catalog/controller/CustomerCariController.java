package com.ikibm.catalog.controller;

import com.ikibm.catalog.entity.CariAccount;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.CariAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomerCariController {

    private final CariAccountService cariAccountService;

    public CustomerCariController(CariAccountService cariAccountService) {
        this.cariAccountService = cariAccountService;
    }

    @GetMapping("/cari-hesabim")
    public String detail(@AuthenticationPrincipal CatalogUserDetails principal, Model model) {
        CariAccount account;
        try {
            account = cariAccountService.getForUser(principal.getId());
        } catch (NotFoundException e) {
            account = null;
        }
        model.addAttribute("account", account);
        if (account != null) {
            model.addAttribute("transactions", cariAccountService.history(account.getId()));
            model.addAttribute("availableLimit", cariAccountService.availableLimit(account.getId()));
        }
        return "public/cari-detail";
    }
}
