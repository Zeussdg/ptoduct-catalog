package com.ikibm.catalog.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikibm.catalog.dto.OrderItemsUpdateRequest;
import com.ikibm.catalog.dto.OrderProductSearchResult;
import com.ikibm.catalog.entity.Order;
import com.ikibm.catalog.entity.OrderItem;
import com.ikibm.catalog.entity.PriceListItem;
import com.ikibm.catalog.entity.Product;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.AuditLogService;
import com.ikibm.catalog.service.OrderService;
import com.ikibm.catalog.service.PriceListService;
import com.ikibm.catalog.service.ProductService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final ProductService productService;
    private final PriceListService priceListService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AdminOrderController(OrderService orderService, ProductService productService,
                                PriceListService priceListService, AuditLogService auditLogService,
                                ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.productService = productService;
        this.priceListService = priceListService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status, Model model) {
        model.addAttribute("orders", orderService.listAll(status, 1).getContent());
        model.addAttribute("status", status);
        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        Order order = orderService.getByIdAdmin(id);
        model.addAttribute("order", order);

        Map<Integer, List<PriceListItem>> priceListMatches = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) continue;
            List<PriceListItem> matches = priceListService.matchesForProduct(item.getProduct().getId());
            if (!matches.isEmpty()) priceListMatches.put(item.getId(), matches);
        }
        model.addAttribute("priceListMatches", priceListMatches);
        return "admin/order-detail";
    }

    @GetMapping("/{id}/products/search")
    @ResponseBody
    public List<OrderProductSearchResult> searchProducts(@PathVariable Integer id, @RequestParam(required = false) String q) {
        orderService.getByIdAdmin(id);
        return productService.adminList(q).stream()
                .limit(20)
                .map(this::toSearchResult)
                .toList();
    }

    private OrderProductSearchResult toSearchResult(Product p) {
        List<OrderProductSearchResult.ListMatch> matches = priceListService.matchesForProduct(p.getId()).stream()
                .map(m -> new OrderProductSearchResult.ListMatch(m.getPriceList().getName(), m.getPrice(), m.getCurrency().name()))
                .toList();
        return new OrderProductSearchResult(p.getId(), p.getName(), p.getBrand(), p.getStockCode(),
                p.getEffectivePrice(), p.getCurrency().name(), matches);
    }

    @PostMapping("/{id}/items")
    public String updateItems(@PathVariable Integer id, @RequestParam("payload") String payload,
                              @AuthenticationPrincipal CatalogUserDetails me, RedirectAttributes ra) {
        try {
            OrderItemsUpdateRequest req = objectMapper.readValue(payload, OrderItemsUpdateRequest.class);
            orderService.updateItems(id, req.lines());
            auditLogService.record(me.getId(), "ORDER_ITEMS_UPDATED", "Order", String.valueOf(id), null);
            ra.addFlashAttribute("message", "Sipariş kalemleri güncellendi");
        } catch (ConflictException | NotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Sipariş kalemleri güncellenemedi: geçersiz veri");
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Integer id, @RequestParam String status,
                               @RequestParam(required = false) String adminNote,
                               @RequestParam(required = false) String carrierName,
                               @RequestParam(required = false) String trackingNumber,
                               @AuthenticationPrincipal CatalogUserDetails me) {
        orderService.updateStatus(id, status, adminNote, carrierName, trackingNumber);
        auditLogService.record(me.getId(), "ORDER_STATUS_CHANGED", "Order", String.valueOf(id), null);
        return "redirect:/admin/orders/" + id;
    }
}
