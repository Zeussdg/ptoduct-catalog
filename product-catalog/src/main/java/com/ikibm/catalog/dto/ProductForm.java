package com.ikibm.catalog.dto;

import com.ikibm.catalog.entity.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Ürün ekle/düzenle form modeli (+ Bean Validation). */
public class ProductForm {
    @Size(max = 255)
    private String brand;

    @NotBlank(message = "Ürün adı zorunludur")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Stok kodu zorunludur")
    @Size(max = 191)
    private String stockCode;

    private Integer categoryId;
    private String description;

    @NotNull(message = "Fiyat zorunludur")
    @DecimalMin(value = "0.0", message = "Fiyat negatif olamaz")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "İndirimli fiyat negatif olamaz")
    private BigDecimal discountPrice;

    private String currency = "TRY";
    private Boolean isActive = true;

    public static ProductForm of(Product p) {
        ProductForm f = new ProductForm();
        f.brand = p.getBrand();
        f.name = p.getName();
        f.stockCode = p.getStockCode();
        f.categoryId = p.getCategory() != null ? p.getCategory().getId() : null;
        f.description = p.getDescription();
        f.price = p.getPrice();
        f.discountPrice = p.getDiscountPrice();
        f.currency = p.getCurrency() != null ? p.getCurrency().name() : "TRY";
        f.isActive = p.getIsActive();
        return f;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(BigDecimal discountPrice) { this.discountPrice = discountPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
