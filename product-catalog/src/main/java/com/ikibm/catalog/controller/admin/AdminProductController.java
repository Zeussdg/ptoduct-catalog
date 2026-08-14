package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.dto.ProductForm;
import com.ikibm.catalog.entity.Product;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.AuditLogService;
import com.ikibm.catalog.service.CategoryService;
import com.ikibm.catalog.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final AuditLogService auditLogService;

    public AdminProductController(ProductService productService, CategoryService categoryService,
                                  AuditLogService auditLogService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("products", productService.adminList(q));
        model.addAttribute("q", q);
        return "admin/products";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ProductForm());
        model.addAttribute("isEdit", false);
        model.addAttribute("categories", categoryService.flatRows());
        return "admin/product-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProductForm form, BindingResult br,
                         @AuthenticationPrincipal CatalogUserDetails me, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("categories", categoryService.flatRows());
            return "admin/product-form";
        }
        try {
            Product p = productService.create(form);
            auditLogService.record(me.getId(), "PRODUCT_CREATED", "Product", String.valueOf(p.getId()), null);
            return "redirect:/admin/products/" + p.getId() + "/edit";
        } catch (Exception e) {
            model.addAttribute("isEdit", false);
            model.addAttribute("categories", categoryService.flatRows());
            model.addAttribute("error", e.getMessage());
            return "admin/product-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        Product p = productService.getById(id);
        model.addAttribute("form", ProductForm.of(p));
        model.addAttribute("product", p);
        model.addAttribute("images", p.getImages());
        model.addAttribute("isEdit", true);
        model.addAttribute("categories", categoryService.flatRows());
        return "admin/product-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id, @Valid @ModelAttribute("form") ProductForm form, BindingResult br,
                         @AuthenticationPrincipal CatalogUserDetails me, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            Product p = productService.getById(id);
            model.addAttribute("isEdit", true);
            model.addAttribute("product", p);
            model.addAttribute("images", p.getImages());
            model.addAttribute("categories", categoryService.flatRows());
            return "admin/product-form";
        }
        productService.update(id, form);
        auditLogService.record(me.getId(), "PRODUCT_UPDATED", "Product", String.valueOf(id), null);
        ra.addFlashAttribute("message", "Ürün güncellendi");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, @AuthenticationPrincipal CatalogUserDetails me,
                         RedirectAttributes ra) {
        try {
            productService.delete(id);
            auditLogService.record(me.getId(), "PRODUCT_DELETED", "Product", String.valueOf(id), null);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id) {
        productService.toggleActive(id);
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/images")
    public String uploadImage(@PathVariable Integer id, @RequestParam("image") MultipartFile image,
                              @RequestParam(defaultValue = "false") boolean isPrimary,
                              @AuthenticationPrincipal CatalogUserDetails me, RedirectAttributes ra) {
        try {
            if (image.isEmpty()) throw new IllegalArgumentException("Görsel dosyası zorunludur");
            productService.addImage(id, image.getBytes(), image.getContentType(), image.getOriginalFilename(), isPrimary);
            auditLogService.record(me.getId(), "PRODUCT_IMAGE_ADDED", "Product", String.valueOf(id), null);
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Görsel okunamadı");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable Integer id, @PathVariable Integer imageId,
                              @AuthenticationPrincipal CatalogUserDetails me, RedirectAttributes ra) {
        try {
            productService.deleteImage(id, imageId);
            auditLogService.record(me.getId(), "PRODUCT_IMAGE_DELETED", "Product", String.valueOf(id), null);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/{id}/images/{imageId}/primary")
    public String setPrimary(@PathVariable Integer id, @PathVariable Integer imageId) {
        productService.setPrimaryImage(id, imageId);
        return "redirect:/admin/products/" + id + "/edit";
    }
}
