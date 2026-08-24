package com.ikibm.catalog.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikibm.catalog.dto.PriceListItemsUpdateRequest;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.service.PriceListItemImportService;
import com.ikibm.catalog.service.PriceListService;
import com.ikibm.catalog.service.ProductService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/price-lists")
public class AdminPriceListController {

    private final PriceListService priceListService;
    private final ProductService productService;
    private final PriceListItemImportService priceListItemImportService;
    private final ObjectMapper objectMapper;

    public AdminPriceListController(PriceListService priceListService, ProductService productService,
                                    PriceListItemImportService priceListItemImportService, ObjectMapper objectMapper) {
        this.priceListService = priceListService;
        this.productService = productService;
        this.priceListItemImportService = priceListItemImportService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("priceLists", priceListService.list());
        return "admin/price-lists";
    }

    @PostMapping
    public String create(@RequestParam String name) {
        priceListService.create(name);
        return "redirect:/admin/price-lists";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, @RequestParam(required = false) String q, Model model) {
        model.addAttribute("priceList", priceListService.get(id));
        model.addAttribute("items", priceListService.items(id));
        model.addAttribute("q", q);
        model.addAttribute("productResults", (q == null || q.isBlank()) ? List.of() : productService.adminList(q));
        return "admin/price-list-detail";
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("fiyat-listesi-import-sablonu.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(priceListItemImportService.buildTemplate());
    }

    @PostMapping("/{id}/items/import")
    public String importItems(@PathVariable Integer id, @RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Excel dosyası seçilmedi");
            return "redirect:/admin/price-lists/" + id;
        }
        try {
            ra.addFlashAttribute("priceImportReport", priceListItemImportService.importForPriceList(id, file.getInputStream()));
        } catch (Exception e) {
            ra.addFlashAttribute("error", "İçe aktarma başarısız: " + e.getMessage());
        }
        return "redirect:/admin/price-lists/" + id;
    }

    @PostMapping("/{id}/items")
    public String addItems(@PathVariable Integer id, @RequestParam("payload") String payload, RedirectAttributes ra) {
        try {
            PriceListItemsUpdateRequest req = objectMapper.readValue(payload, PriceListItemsUpdateRequest.class);
            if (req.items() != null) {
                for (PriceListItemsUpdateRequest.Item it : req.items()) {
                    priceListService.addItem(id, it.productId(), it.price(), Currency.valueOf(it.currency()));
                }
            }
            ra.addFlashAttribute("message", "Ürünler fiyat listesine eklendi");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Eklenemedi: geçersiz veri");
        }
        return "redirect:/admin/price-lists/" + id;
    }

    @PostMapping("/{id}/items/{itemId}/delete")
    public String deleteItem(@PathVariable Integer id, @PathVariable Integer itemId) {
        priceListService.removeItem(itemId);
        return "redirect:/admin/price-lists/" + id;
    }
}
