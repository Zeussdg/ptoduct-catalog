package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.ProductForm;
import com.ikibm.catalog.dto.PriceSource;
import com.ikibm.catalog.dto.ProductPriceBreakdown;
import com.ikibm.catalog.dto.ResolvedPrice;
import com.ikibm.catalog.entity.CustomerPrice;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.PriceListItem;
import com.ikibm.catalog.entity.Product;
import com.ikibm.catalog.entity.ProductImage;
import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.CategoryRepository;
import com.ikibm.catalog.repository.CustomerPriceRepository;
import com.ikibm.catalog.repository.PriceListItemRepository;
import com.ikibm.catalog.repository.ProductImageRepository;
import com.ikibm.catalog.repository.ProductRepository;
import com.ikibm.catalog.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    public static final int PAGE_SIZE = 20;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final CustomerPriceRepository customerPriceRepository;
    private final PriceListItemRepository priceListItemRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          ProductImageRepository productImageRepository, CustomerPriceRepository customerPriceRepository,
                          PriceListItemRepository priceListItemRepository, UserRepository userRepository,
                          StorageService storageService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
        this.customerPriceRepository = customerPriceRepository;
        this.priceListItemRepository = priceListItemRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    // ---- vitrin (okuma) ----

    public Page<Product> catalog(String q, String marka, String kategori, String altkategori, String sirala, int page) {
        Specification<Product> spec = Specification.where(ProductSpecifications.activeOnly());
        // Spring Data JPA 4.x'te Specification.and(null) artık IllegalArgumentException fırlatıyor,
        // bu yüzden boş filtreler için and() sadece Specification null değilse çağrılıyor.
        Specification<Product> searchSpec = ProductSpecifications.search(q);
        if (searchSpec != null) spec = spec.and(searchSpec);
        Specification<Product> brandSpec = ProductSpecifications.brand(marka);
        if (brandSpec != null) spec = spec.and(brandSpec);
        if (altkategori != null && !altkategori.isBlank()) {
            spec = spec.and(ProductSpecifications.subCategory(altkategori));
        } else if (kategori != null && !kategori.isBlank()) {
            spec = spec.and(ProductSpecifications.mainCategory(kategori));
        }
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), PAGE_SIZE, sortOf(sirala));
        return productRepository.findAll(spec, pageable);
    }

    private Sort sortOf(String sirala) {
        if (sirala == null) return Sort.by("id").ascending();
        return switch (sirala) {
            case "price-asc" -> Sort.by("price").ascending();
            case "price-desc" -> Sort.by("price").descending();
            case "name-asc" -> Sort.by("name").ascending();
            default -> Sort.by("id").ascending();
        };
    }

    public List<String> brands() { return productRepository.findDistinctBrands(); }

    public Product getById(Integer id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Ürün bulunamadı"));
    }

    public List<Product> related(Product product) {
        if (product.getCategory() == null) return List.of();
        return productRepository.findTop4ByCategory_SlugAndIsActiveTrueAndIdNot(
                product.getCategory().getSlug(), product.getId());
    }

    public long activeCount() { return productRepository.countByIsActiveTrue(); }

    /** Bir ürünün, verilen kullanıcı için geçerli fiyatını çözer — öncelik sırası:
     * müşteriye özel fiyat → müşterinin bağlı olduğu fiyat listeleri (birden fazla olabilir, en ucuzu
     * kazanır) → özel (indirimli) fiyat → bayi fiyatı → liste fiyatı. İlk uygun olan kazanır. */
    public ResolvedPrice resolvePrice(Product product, Integer userId) {
        CustomerPrice cp = userId != null
                ? customerPriceRepository.findByUser_IdAndProduct_Id(userId, product.getId()).orElse(null)
                : null;
        List<PriceListItem> pliMatches = !isValid(cp) ? findAllAssignedListItems(userId, product.getId()) : List.of();
        Winner w = resolveWinner(product, cp, pliMatches);
        return new ResolvedPrice(w.price(), w.currency(), w.source() == PriceSource.CUSTOMER, false);
    }

    /** {@link #resolvePrice} metodunun toplu hali — liste sayfalarında ürün başına ayrı sorgu atmamak için. */
    public Map<Integer, ResolvedPrice> resolvePrices(List<Product> products, Integer userId) {
        Map<Integer, CustomerPrice> customerPriceByProductId = new HashMap<>();
        Map<Integer, List<PriceListItem>> listItemsByProductId = new HashMap<>();
        if (userId != null) {
            for (CustomerPrice cp : customerPriceRepository.findByUser_Id(userId)) {
                customerPriceByProductId.put(cp.getProduct().getId(), cp);
            }
            List<Integer> priceListIds = assignedActivePriceListIds(userId);
            if (!priceListIds.isEmpty()) {
                for (PriceListItem pli : priceListItemRepository.findByPriceList_IdIn(priceListIds)) {
                    listItemsByProductId.computeIfAbsent(pli.getProduct().getId(), k -> new ArrayList<>()).add(pli);
                }
            }
        }
        Map<Integer, ResolvedPrice> result = new HashMap<>();
        for (Product p : products) {
            CustomerPrice cp = customerPriceByProductId.get(p.getId());
            List<PriceListItem> pliMatches = !isValid(cp)
                    ? listItemsByProductId.getOrDefault(p.getId(), List.of()) : List.of();
            Winner w = resolveWinner(p, cp, pliMatches);
            result.put(p.getId(), new ResolvedPrice(w.price(), w.currency(),
                    w.source() == PriceSource.CUSTOMER, false));
        }
        return result;
    }

    /** {@link #priceBreakdown} metodunun toplu hali — katalog listesindeki "Sepete Ekle" butonunun,
     * ürün detay sayfasına girmeden de fiyat seçim modalı gösterebilmesi için (ör. Taksitli/Peşin).
     * Müşteriye özel geçerli bir fiyat varsa (tek değer zaten kazandığı için) boş liste döner. */
    public Map<Integer, List<ProductPriceBreakdown.PriceListOption>> priceListOptions(List<Product> products, Integer userId) {
        Map<Integer, CustomerPrice> customerPriceByProductId = new HashMap<>();
        Map<Integer, List<PriceListItem>> listItemsByProductId = new HashMap<>();
        if (userId != null) {
            for (CustomerPrice cp : customerPriceRepository.findByUser_Id(userId)) {
                customerPriceByProductId.put(cp.getProduct().getId(), cp);
            }
            List<Integer> priceListIds = assignedActivePriceListIds(userId);
            if (!priceListIds.isEmpty()) {
                for (PriceListItem pli : priceListItemRepository.findByPriceList_IdIn(priceListIds)) {
                    listItemsByProductId.computeIfAbsent(pli.getProduct().getId(), k -> new ArrayList<>()).add(pli);
                }
            }
        }
        Map<Integer, List<ProductPriceBreakdown.PriceListOption>> result = new HashMap<>();
        for (Product p : products) {
            CustomerPrice cp = customerPriceByProductId.get(p.getId());
            List<PriceListItem> pliMatches = !isValid(cp)
                    ? listItemsByProductId.getOrDefault(p.getId(), List.of()) : List.of();
            result.put(p.getId(), pliMatches.stream()
                    .sorted(Comparator.comparing(PriceListItem::getPrice))
                    .map(pli -> new ProductPriceBreakdown.PriceListOption(
                            pli.getPriceList().getId(), pli.getPriceList().getName(), pli.getPrice(), pli.getCurrency()))
                    .toList());
        }
        return result;
    }

    /** Ürün detay sayfasının fiyat kartı için: vurgulanan fiyat + müşterinin bağlı olduğu fiyat
     * listelerinden gelen seçenekler (ör. Taksitli/Peşin) + Özel/Bayi/Liste karşılaştırma kademeleri. */
    public ProductPriceBreakdown priceBreakdown(Product product, Integer userId) {
        CustomerPrice cp = userId != null
                ? customerPriceRepository.findByUser_IdAndProduct_Id(userId, product.getId()).orElse(null)
                : null;
        List<PriceListItem> pliMatches = !isValid(cp) ? findAllAssignedListItems(userId, product.getId()) : List.of();
        Winner w = resolveWinner(product, cp, pliMatches);

        List<ProductPriceBreakdown.PriceListOption> options = pliMatches.stream()
                .sorted(Comparator.comparing(PriceListItem::getPrice))
                .map(pli -> new ProductPriceBreakdown.PriceListOption(
                        pli.getPriceList().getId(), pli.getPriceList().getName(), pli.getPrice(), pli.getCurrency()))
                .toList();

        var special = product.getDiscountPrice() != null
                ? new ProductPriceBreakdown.TierAmount(product.getDiscountPrice(), product.getCurrency()) : null;
        var dealer = product.getDealerPrice() != null
                ? new ProductPriceBreakdown.TierAmount(product.getDealerPrice(), product.getCurrency()) : null;
        var list = new ProductPriceBreakdown.TierAmount(product.getPrice(), product.getCurrency());

        return new ProductPriceBreakdown(w.price(), w.currency(), w.source(),
                false, options, special, dealer, list);
    }

    private record Winner(java.math.BigDecimal price, Currency currency, PriceSource source, String priceListName) {}

    private Winner resolveWinner(Product product, CustomerPrice cp, List<PriceListItem> pliMatches) {
        if (isValid(cp)) {
            return new Winner(cp.getPrice(), cp.getCurrency(), PriceSource.CUSTOMER, null);
        }
        if (pliMatches != null && !pliMatches.isEmpty()) {
            PriceListItem cheapest = pliMatches.stream().min(Comparator.comparing(PriceListItem::getPrice)).orElseThrow();
            return new Winner(cheapest.getPrice(), cheapest.getCurrency(), PriceSource.PRICE_LIST, cheapest.getPriceList().getName());
        }
        if (product.getDiscountPrice() != null) {
            return new Winner(product.getDiscountPrice(), product.getCurrency(), PriceSource.SPECIAL, null);
        }
        if (product.getDealerPrice() != null) {
            return new Winner(product.getDealerPrice(), product.getCurrency(), PriceSource.DEALER, null);
        }
        return new Winner(product.getPrice(), product.getCurrency(), PriceSource.LIST, null);
    }

    /** Kullanıcının bağlı olduğu (aktif) fiyat listelerinden, verilen ürün için bulunan tüm eşleşmeler
     * — bir ürün, müşterinin bağlı olduğu birden fazla listede (ör. Taksitli + Peşin) olabilir. */
    private List<PriceListItem> findAllAssignedListItems(Integer userId, Integer productId) {
        if (userId == null) return List.of();
        List<Integer> priceListIds = assignedActivePriceListIds(userId);
        if (priceListIds.isEmpty()) return List.of();
        return priceListItemRepository.findByPriceList_IdInAndProduct_Id(priceListIds, productId);
    }

    private List<Integer> assignedActivePriceListIds(Integer userId) {
        User u = userRepository.findById(userId).orElse(null);
        if (u == null || u.getPriceLists() == null) return List.of();
        return u.getPriceLists().stream()
                .filter(pl -> Boolean.TRUE.equals(pl.getIsActive()))
                .map(com.ikibm.catalog.entity.PriceList::getId)
                .toList();
    }

    private boolean isValid(CustomerPrice cp) {
        if (cp == null) return false;
        Instant now = Instant.now();
        if (cp.getValidFrom() != null && now.isBefore(cp.getValidFrom())) return false;
        if (cp.getValidTo() != null && now.isAfter(cp.getValidTo())) return false;
        return true;
    }

    // ---- admin ----

    /** Admin ürün listesi: tüm ürünler (aktif + pasif), opsiyonel arama (ad/stok kodu/marka). */
    public List<Product> adminList(String q) {
        Specification<Product> spec = ProductSpecifications.search(q);
        // Spring Data JPA 4.x'te findAll(Specification, Sort) de null Specification kabul etmiyor.
        return spec != null
                ? productRepository.findAll(spec, Sort.by("id").ascending())
                : productRepository.findAll(Sort.by("id").ascending());
    }

    /** Admin ürün listesi, sayfalanmış (545 ürünü tek sayfada basmamak için, 20/sayfa). */
    public Page<Product> adminList(String q, int page) {
        Specification<Product> spec = ProductSpecifications.search(q);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), PAGE_SIZE, Sort.by("id").ascending());
        return spec != null
                ? productRepository.findAll(spec, pageable)
                : productRepository.findAll(pageable);
    }

    public long totalCount() { return productRepository.count(); }

    @Transactional
    public Product create(ProductForm f) {
        if (productRepository.existsByStockCode(f.getStockCode())) {
            throw new ConflictException("Bu stok kodu zaten kayıtlı");
        }
        Product p = new Product();
        apply(p, f);
        return productRepository.save(p);
    }

    @Transactional
    public Product update(Integer id, ProductForm f) {
        Product p = getById(id);
        apply(p, f);
        return productRepository.save(p);
    }

    private void apply(Product p, ProductForm f) {
        p.setBrand(f.getBrand());
        p.setName(f.getName());
        p.setStockCode(f.getStockCode());
        p.setDescription(f.getDescription());
        p.setPrice(f.getPrice());
        p.setDiscountPrice(f.getDiscountPrice());
        p.setDealerPrice(f.getDealerPrice());
        p.setCurrency(parseCurrency(f.getCurrency()));
        p.setIsActive(f.getIsActive() != null ? f.getIsActive() : Boolean.TRUE);
        p.setCategory(f.getCategoryId() != null ? categoryRepository.getReferenceById(f.getCategoryId()) : null);
    }

    private Currency parseCurrency(String c) {
        try { return c == null ? Currency.TRY : Currency.valueOf(c); }
        catch (IllegalArgumentException e) { return Currency.TRY; }
    }

    @Transactional
    public void delete(Integer id) {
        Product p = getById(id);
        try {
            productRepository.delete(p);
            productRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Bu ürün sepetlerde kullanılıyor, silmek yerine pasif yapabilirsiniz");
        }
    }

    @Transactional
    public void toggleActive(Integer id) {
        Product p = getById(id);
        p.setIsActive(!Boolean.TRUE.equals(p.getIsActive()));
        productRepository.save(p);
    }

    @Transactional
    public ProductImage addImage(Integer productId, byte[] bytes, String contentType, String originalName, boolean primary) {
        Product p = getById(productId);
        StorageService.Uploaded up = storageService.uploadProductImage(productId, bytes, contentType, originalName);
        if (primary) productImageRepository.clearPrimary(productId);
        ProductImage img = new ProductImage();
        img.setProduct(p);
        img.setKey(up.key());
        img.setUrl(up.url());
        img.setIsPrimary(primary);
        img.setSortOrder(productImageRepository.maxSortOrder(productId) + 1);
        return productImageRepository.save(img);
    }

    @Transactional
    public void deleteImage(Integer productId, Integer imageId) {
        ProductImage img = productImageRepository.findByIdAndProduct_Id(imageId, productId)
                .orElseThrow(() -> new NotFoundException("Görsel bulunamadı"));
        storageService.deleteObject(img.getKey());
        productImageRepository.delete(img);
    }

    @Transactional
    public void setPrimaryImage(Integer productId, Integer imageId) {
        ProductImage img = productImageRepository.findByIdAndProduct_Id(imageId, productId)
                .orElseThrow(() -> new NotFoundException("Görsel bulunamadı"));
        productImageRepository.clearPrimary(productId);
        img.setIsPrimary(true);
        productImageRepository.save(img);
    }
}
