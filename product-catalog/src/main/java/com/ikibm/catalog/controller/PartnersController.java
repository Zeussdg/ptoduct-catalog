package com.ikibm.catalog.controller;

import com.ikibm.catalog.config.PresentationData;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PartnersController {

    private final PresentationData presentationData;

    public PartnersController(PresentationData presentationData) {
        this.presentationData = presentationData;
    }

    @GetMapping("/is-ortaklarimiz")
    public String partners(Model model) {
        model.addAttribute("partners", presentationData.getPartners());
        return "public/partners";
    }
}
