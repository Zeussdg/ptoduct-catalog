package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.entity.Category;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.AuditLogService;
import com.ikibm.catalog.service.CategoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final AuditLogService auditLogService;

    public AdminCategoryController(CategoryService categoryService, AuditLogService auditLogService) {
        this.categoryService = categoryService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.flatRows());
        return "admin/categories";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam String slug,
                         @RequestParam(required = false) Integer parentId,
                         @AuthenticationPrincipal CatalogUserDetails me, RedirectAttributes ra) {
        if (name == null || name.isBlank()) {
            ra.addFlashAttribute("error", "Kategori adı zorunludur");
            return "redirect:/admin/categories";
        }
        if (slug == null || !slug.matches("[a-z0-9-]+")) {
            ra.addFlashAttribute("error", "Slug yalnızca küçük harf, rakam ve tire içerebilir (örn. ag-network)");
            return "redirect:/admin/categories";
        }
        try {
            Category c = categoryService.create(name, slug, parentId);
            auditLogService.record(me.getId(), "CATEGORY_CREATED", "Category", String.valueOf(c.getId()), null);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id) {
        categoryService.toggleActive(id);
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, @AuthenticationPrincipal CatalogUserDetails me,
                         RedirectAttributes ra) {
        try {
            categoryService.delete(id);
            auditLogService.record(me.getId(), "CATEGORY_DELETED", "Category", String.valueOf(id), null);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
