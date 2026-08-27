package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.service.CariAccountService;
import com.ikibm.catalog.service.CustomerPriceImportService;
import com.ikibm.catalog.service.CustomerPriceService;
import com.ikibm.catalog.service.PriceListService;
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
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final UserService userService;
    private final QuoteService quoteService;
    private final ProductService productService;
    private final CustomerPriceService customerPriceService;
    private final CustomerPriceImportService customerPriceImportService;
    private final PriceListService priceListService;
    private final CariAccountService cariAccountService;

    public AdminCustomerController(UserService userService, QuoteService quoteService,
                                   ProductService productService, CustomerPriceService customerPriceService,
                                   CustomerPriceImportService customerPriceImportService,
                                   PriceListService priceListService, CariAccountService cariAccountService) {
        this.userService = userService;
        this.quoteService = quoteService;
        this.productService = productService;
        this.customerPriceService = customerPriceService;
        this.customerPriceImportService = customerPriceImportService;
        this.priceListService = priceListService;
        this.cariAccountService = cariAccountService;
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
        var customer = userService.getById(id);
        model.addAttribute("customer", customer);
        model.addAttribute("quotes", quoteService.listForUser(id));
        model.addAttribute("customerPrices", customerPriceService.listForCustomer(id));
        model.addAttribute("q", q);
        model.addAttribute("productResults", (q == null || q.isBlank()) ? List.of() : productService.adminList(q));
        model.addAttribute("priceLists", priceListService.list());
        Set<Integer> selectedPriceListIds = customer.getPriceLists().stream()
                .map(com.ikibm.catalog.entity.PriceList::getId).collect(Collectors.toSet());
        model.addAttribute("selectedPriceListIds", selectedPriceListIds);
        model.addAttribute("cariAccount", cariAccountService.getOrCreateForUser(id));
        return "admin/customer-detail";
    }

    /** Fatura kesebilmek için gerekli bilgiler (InvoiceService fatura oluşturmadan önce bunların
     * doluluğunu kontrol eder) — mevcut kullanıcı akışlarına dokunmadan ayrı bir form/endpoint. */
    @PostMapping("/{id}/billing-info")
    public String updateBillingInfo(@PathVariable Integer id,
                                    @RequestParam(required = false) String taxNumber,
                                    @RequestParam(required = false) String taxOffice,
                                    @RequestParam(required = false) String billingAddress) {
        userService.updateBillingInfo(id, taxNumber, taxOffice, billingAddress);
        return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/{id}/price-list")
    public String setPriceList(@PathVariable Integer id, @RequestParam(required = false) List<Integer> priceListIds) {
        userService.assignPriceLists(id, priceListIds);
        return "redirect:/admin/customers/" + id;
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
