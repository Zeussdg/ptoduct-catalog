package com.ikibm.catalog.web;

import com.ikibm.catalog.config.SiteInfo;
import com.ikibm.catalog.entity.Category;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Her Thymeleaf görünümüne ortak veri ekler: site bilgisi ve navigasyon için
 * ana kategoriler.
 */
@ControllerAdvice(basePackages = "com.ikibm.catalog.controller")
public class GlobalModelAttributes {

    private final SiteInfo siteInfo;
    private final CategoryService categoryService;

    public GlobalModelAttributes(SiteInfo siteInfo, CategoryService categoryService) {
        this.siteInfo = siteInfo;
        this.categoryService = categoryService;
    }

    @ModelAttribute("site")
    public SiteInfo site() {
        return siteInfo;
    }

    @ModelAttribute("navCategories")
    public List<Category> navCategories() {
        return categoryService.allOrdered().stream()
                .filter(c -> c.getParent() == null)
                .limit(6)
                .toList();
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("currentUser")
    public CatalogUserDetails currentUser(@AuthenticationPrincipal CatalogUserDetails principal) {
        return principal;
    }
}
