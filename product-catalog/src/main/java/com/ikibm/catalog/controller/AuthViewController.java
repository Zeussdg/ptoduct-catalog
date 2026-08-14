package com.ikibm.catalog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthViewController {

    /** Giriş sayfası. POST /login'i Spring Security işler. */
    @GetMapping("/login")
    public String login() {
        return "public/login";
    }
}
