package com.ikibm.catalog.controller;

import com.ikibm.catalog.config.PresentationData;
import com.ikibm.catalog.entity.Product;
import com.ikibm.catalog.service.CampaignBannerService;
import com.ikibm.catalog.service.CategoryService;
import com.ikibm.catalog.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class CatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CampaignBannerService campaignBannerService;
    private final PresentationData presentationData;

    public CatalogController(ProductService productService, CategoryService categoryService,
                             CampaignBannerService campaignBannerService, PresentationData presentationData) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.campaignBannerService = campaignBannerService;
        this.presentationData = presentationData;
    }

    @GetMapping("/")
    public String catalog(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String marka,
            @RequestParam(required = false) String kategori,
            @RequestParam(required = false) String altkategori,
            @RequestParam(required = false) String sirala,
            @RequestParam(required = false, defaultValue = "1") int sayfa,
            Model model) {

        Page<Product> page = productService.catalog(q, marka, kategori, altkategori, sirala, sayfa);

        int currentPage = page.getNumber() + 1;
        long total = page.getTotalElements();
        int from = total == 0 ? 0 : page.getNumber() * ProductService.PAGE_SIZE + 1;
        int to = (int) Math.min(total, (long) page.getNumber() * ProductService.PAGE_SIZE + page.getNumberOfElements());

        boolean noFilters = isBlank(q) && isBlank(marka) && isBlank(kategori) && isBlank(altkategori);
        boolean showHero = noFilters && currentPage == 1;

        model.addAttribute("products", page.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", Math.max(1, page.getTotalPages()));
        model.addAttribute("totalElements", total);
        model.addAttribute("fromIndex", from);
        model.addAttribute("toIndex", to);

        model.addAttribute("brands", productService.brands());
        model.addAttribute("sidebar", categoryService.sidebar());
        model.addAttribute("allCount", productService.activeCount());
        model.addAttribute("activeCategoryLabel", categoryService.label(kategori, altkategori));

        model.addAttribute("q", q);
        model.addAttribute("marka", marka);
        model.addAttribute("kategori", kategori);
        model.addAttribute("altkategori", altkategori);
        model.addAttribute("sirala", sirala);
        model.addAttribute("baseQuery", buildBaseQuery(q, marka, kategori, altkategori, sirala));

        model.addAttribute("showHero", showHero);
        if (showHero) {
            model.addAttribute("heroSlides", presentationData.getHeroSlides());
            model.addAttribute("campaigns", campaignBannerService.campaignsWithImages());
        }

        return "public/products";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Filtre parametrelerini "&" ile biten (ya da boş) bir sorgu dizesine çevirir; sayfalama linkleri buna 'sayfa=N' ekler. */
    private String buildBaseQuery(String q, String marka, String kategori, String altkategori, String sirala) {
        StringBuilder sb = new StringBuilder();
        append(sb, "q", q);
        append(sb, "marka", marka);
        append(sb, "kategori", kategori);
        append(sb, "altkategori", altkategori);
        append(sb, "sirala", sirala);
        return sb.toString();
    }

    private void append(StringBuilder sb, String key, String value) {
        if (value == null || value.isBlank()) return;
        sb.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8)).append('&');
    }
}
