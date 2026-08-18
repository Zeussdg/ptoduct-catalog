package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.service.CustomerPriceImportService;
import com.ikibm.catalog.service.CustomerPriceService;
import com.ikibm.catalog.service.ProductService;
import com.ikibm.catalog.service.QuoteService;
import com.ikibm.catalog.service.UserService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final UserService userService;
    private final QuoteService quoteService;
    private final ProductService productService;
    private final CustomerPriceService customerPriceService;
    private final CustomerPriceImportService customerPriceImportService;

    public AdminCustomerController(UserService userService, QuoteService quoteService,
                                   ProductService productService, CustomerPriceService customerPriceService,
                                   CustomerPriceImportService customerPriceImportService) {
        this.userService = userService;
        this.quoteService = quoteService;
        this.productService = productService;
        this.customerPriceService = customerPriceService;
        this.customerPriceImportService = customerPriceImportService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("customers", userService.listCustomers());
        return "admin/customers";
    }

    @GetMapping("/price-import-template")
    public ResponseEntity<byte[]> priceImportTemplate() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("ozel-fiyat-import-sablonu.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(customerPriceImportService.buildTemplate());
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, @RequestParam(required = false) String q, Model model) {
        model.addAttribute("customer", userService.getById(id));
        model.addAttribute("quotes", quoteService.listForUser(id));
        model.addAttribute("customerPrices", customerPriceService.listForCustomer(id));
        model.addAttribute("q", q);
        model.addAttribute("productResults", (q == null || q.isBlank()) ? List.of() : productService.adminList(q));
        return "admin/customer-detail";
    }

    @PostMapping("/{id}/prices")
    public String createPrice(@PathVariable Integer id,
                              @RequestParam Integer productId,
                              @RequestParam BigDecimal price,
                              @RequestParam Currency currency,
                              @RequestParam(required = false) String validFrom,
                              @RequestParam(required = false) String validTo) {
        customerPriceService.create(id, productId, price, currency, toInstant(validFrom), toInstant(validTo));
        return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/{id}/prices/{priceId}/delete")
    public String deletePrice(@PathVariable Integer id, @PathVariable Integer priceId) {
        customerPriceService.delete(priceId);
        return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/{id}/prices/import")
    public String importPrices(@PathVariable Integer id, @RequestParam("file") MultipartFile file,
                               RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Excel dosyası seçilmedi");
            return "redirect:/admin/customers/" + id;
        }
        try {
            ra.addFlashAttribute("priceImportReport", customerPriceImportService.importForCustomer(id, file.getInputStream()));
        } catch (Exception e) {
            ra.addFlashAttribute("error", "İçe aktarma başarısız: " + e.getMessage());
        }
        return "redirect:/admin/customers/" + id;
    }

    private Instant toInstant(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return null;
        return LocalDate.parse(isoDate).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
