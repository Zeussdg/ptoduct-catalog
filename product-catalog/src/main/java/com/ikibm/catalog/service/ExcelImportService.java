package com.ikibm.catalog.service;

import com.ikibm.catalog.entity.Category;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.Product;
import com.ikibm.catalog.repository.CategoryRepository;
import com.ikibm.catalog.repository.ProductRepository;
import com.ikibm.catalog.util.CategoryClassifier;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * Excel'den ürün içe aktarma (scripts/importProducts.js portu, Apache POI).
 * Başlık satırını otomatik tespit eder, kolonları bulanık eşler, fiyat/para
 * birimini normalize eder, kategoriyi sınıflandırır, tekrarları ayıklar ve
 * kategori + ürünleri DB'ye upsert eder.
 */
@Service
public class ExcelImportService {

    private static final Map<String, List<String>> COLUMN_ALIASES = new LinkedHashMap<>();
    static {
        COLUMN_ALIASES.put("brand", List.of("marka", "brand", "uretici", "üretici", "firma"));
        COLUMN_ALIASES.put("stockCode", List.of("stok kodu", "stok kod", "stokkodu", "urun kodu", "ürün kodu", "malzeme kodu", "barkod", "sku", "kod", "code"));
        COLUMN_ALIASES.put("name", List.of("urun adi", "ürün adı", "urun ismi", "malzeme adi", "stok adi", "stok adı", "product name", "product", "urun", "ürün", "tanim", "tanım", "ad", "isim", "name"));
        COLUMN_ALIASES.put("category", List.of("alt kategori", "kategori", "urun grubu", "ürün grubu", "grup", "category", "tip", "sinif", "sınıf"));
        COLUMN_ALIASES.put("price", List.of("liste fiyat", "birim fiyat", "satis fiyat", "satış fiyat", "fiyat", "price", "tutar", "amount"));
        COLUMN_ALIASES.put("currency", List.of("para birimi", "para birim", "doviz", "döviz", "currency", "kur", "birim"));
        COLUMN_ALIASES.put("description", List.of("aciklama", "açıklama", "description", "detay", "ozellik", "özellik"));
    }

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryClassifier classifier;

    public ExcelImportService(ProductRepository productRepository, CategoryRepository categoryRepository,
                              CategoryClassifier classifier) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.classifier = classifier;
    }

    public record Report(int rowsRead, int skipped, int duplicates, int created, int updated,
                         int unclassified, boolean ok, String message) {}

    @Transactional
    public Report importFrom(InputStream in, String originalName) {
        List<List<String>> rows;
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            rows = readRows(sheet);
        } catch (Exception e) {
            return new Report(0, 0, 0, 0, 0, 0, false, "Excel okunamadı: " + e.getMessage());
        }

        int[] header = detectHeaderRow(rows);
        int headerIndex = header[0], score = header[1];
        if (headerIndex == -1 || score < 1) {
            return new Report(rows.size(), 0, 0, 0, 0, 0, false,
                    "Başlık satırı tespit edilemedi (en az bir Ürün Adı/Marka/Stok Kodu sütunu gerekli).");
        }

        Map<String, Integer> map = new HashMap<>();
        List<String> headers = rows.get(headerIndex);
        for (var e : COLUMN_ALIASES.entrySet()) {
            map.put(e.getKey(), matchColumn(headers, e.getValue()));
        }

        int rowsRead = 0, skipped = 0, duplicates = 0, created = 0, updated = 0, unclassified = 0;
        Set<String> seen = new HashSet<>();
        Map<String, Category> categoryCache = new HashMap<>();

        for (int i = headerIndex + 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.stream().allMatch(s -> s == null || s.isBlank())) continue;
            rowsRead++;

            String name = clean(get(row, map, "name"));
            String brand = clean(get(row, map, "brand"));
            String rawCategory = clean(get(row, map, "category"));
            String stockCode = clean(get(row, map, "stockCode"));
            String description = clean(get(row, map, "description"));

            if (name.isEmpty()) name = description;
            if (name.isEmpty()) { skipped++; continue; }

            if (brand.isEmpty()) {
                String first = name.split(" ")[0];
                brand = first.matches("[A-Za-zÇĞİÖŞÜ0-9-]{2,}") ? first.toUpperCase(Locale.ROOT) : "GENEL";
            } else {
                brand = brand.toUpperCase(Locale.ROOT);
            }

            BigDecimal price = parsePrice(get(row, map, "price"));
            Currency currency = normalizeCurrency(get(row, map, "currency"), brand);
            if (description.isEmpty()) description = name;
            CategoryClassifier.Result cat = classifier.classify(name, rawCategory);
            if (!cat.classified()) unclassified++;

            String code = stockCode.isEmpty()
                    ? "IMP-" + classifier.slugify(brand + "-" + name)
                    : stockCode;

            String dedupeKey = stockCode.isEmpty()
                    ? "nm:" + classifier.normalizeText(brand) + "|" + classifier.normalizeText(name)
                    : "sk:" + classifier.normalizeText(stockCode);
            if (!seen.add(dedupeKey)) { duplicates++; continue; }

            Category category = ensureCategory(categoryCache, cat);

            Product existing = productRepository.findByStockCode(code).orElse(null);
            if (existing == null) {
                Product p = new Product();
                p.setStockCode(code);
                apply(p, brand, name, description, price, currency, category);
                productRepository.save(p);
                created++;
            } else {
                apply(existing, brand, name, description, price, currency, category);
                productRepository.save(existing);
                updated++;
            }
        }

        String msg = String.format("%d ürün eklendi, %d güncellendi (%d tekrar, %d boş atlandı, %d sınıflandırılamadı).",
                created, updated, duplicates, skipped, unclassified);
        return new Report(rowsRead, skipped, duplicates, created, updated, unclassified, true, msg);
    }

    private void apply(Product p, String brand, String name, String desc, BigDecimal price, Currency cur, Category cat) {
        p.setBrand(brand);
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(price != null ? price : BigDecimal.ZERO);
        p.setCurrency(cur);
        p.setCategory(cat);
        p.setIsActive(true);
    }

    private Category ensureCategory(Map<String, Category> cache, CategoryClassifier.Result cat) {
        Category main = cache.computeIfAbsent(cat.mainCategorySlug(), slug ->
                categoryRepository.findBySlug(slug).orElseGet(() -> {
                    Category c = new Category();
                    c.setName(cat.mainCategory());
                    c.setSlug(slug);
                    c.setIsActive(true);
                    return categoryRepository.save(c);
                }));
        return cache.computeIfAbsent(cat.categorySlug(), slug ->
                categoryRepository.findBySlug(slug).orElseGet(() -> {
                    Category c = new Category();
                    c.setName(cat.category());
                    c.setSlug(slug);
                    c.setIsActive(true);
                    c.setParent(main);
                    return categoryRepository.save(c);
                }));
    }

    /** Doğru başlıklarla, örnek 1 satır içeren indirilebilir şablon (aynı COLUMN_ALIASES'tan üretilir). */
    public byte[] buildTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Ürünler");
            String[] headers = {"Marka", "Stok Kodu", "Ürün Adı", "Kategori", "Fiyat", "Para Birimi", "Açıklama"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

            Row example = sheet.createRow(1);
            String[] values = {"TENDA", "S999", "Örnek 5 Port Switch", "Ağ Ürünleri", "199.90", "USD", "Örnek açıklama metni"};
            for (int i = 0; i < values.length; i++) example.createCell(i).setCellValue(values[i]);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Şablon oluşturulamadı: " + e.getMessage(), e);
        }
    }

    // ---- Excel okuma ----

    private List<List<String>> readRows(Sheet sheet) {
        DataFormatter fmt = new DataFormatter(new Locale("tr", "TR"));
        List<List<String>> rows = new ArrayList<>();
        int last = sheet.getLastRowNum();
        for (int r = 0; r <= last; r++) {
            Row row = sheet.getRow(r);
            List<String> cells = new ArrayList<>();
            if (row != null) {
                int lastCell = row.getLastCellNum();
                for (int c = 0; c < lastCell; c++) {
                    Cell cell = row.getCell(c);
                    cells.add(cellString(cell, fmt));
                }
            }
            rows.add(cells);
        }
        return rows;
    }

    private String cellString(Cell cell, DataFormatter fmt) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        return fmt.formatCellValue(cell);
    }

    private int[] detectHeaderRow(List<List<String>> rows) {
        int bestIndex = -1, bestScore = -1;
        int limit = Math.min(rows.size(), 15);
        for (int i = 0; i < limit; i++) {
            List<String> headers = rows.get(i);
            if (headers.stream().allMatch(h -> h == null || h.isBlank())) continue;
            int score = 0;
            for (var e : COLUMN_ALIASES.entrySet()) {
                if (matchColumn(headers, e.getValue()) != -1) score++;
            }
            if (score > bestScore) { bestScore = score; bestIndex = i; }
        }
        return new int[]{bestIndex, bestScore};
    }

    private int matchColumn(List<String> headers, List<String> aliases) {
        List<String> norm = headers.stream().map(classifier::normalizeText).toList();
        for (String alias : aliases) {
            String a = classifier.normalizeText(alias);
            for (int i = 0; i < norm.size(); i++) if (norm.get(i).equals(a)) return i;
        }
        for (String alias : aliases) {
            String a = classifier.normalizeText(alias);
            for (int i = 0; i < norm.size(); i++) if (!norm.get(i).isEmpty() && norm.get(i).contains(a)) return i;
        }
        return -1;
    }

    private String get(List<String> row, Map<String, Integer> map, String field) {
        Integer idx = map.get(field);
        if (idx == null || idx == -1 || idx >= row.size()) return "";
        return row.get(idx);
    }

    private String clean(String raw) {
        return raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
    }

    private BigDecimal parsePrice(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().replaceAll("[^\\d.,-]", "");
        if (s.isEmpty()) return null;
        boolean hasComma = s.contains(","), hasDot = s.contains(".");
        if (hasComma && hasDot) {
            if (s.lastIndexOf(",") > s.lastIndexOf(".")) s = s.replace(".", "").replace(",", ".");
            else s = s.replace(",", "");
        } else if (hasComma) {
            s = s.replace(",", ".");
        }
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return null; }
    }

    private Currency normalizeCurrency(String raw, String brand) {
        String s = classifier.normalizeText(raw);
        String rawS = raw == null ? "" : raw;
        if (s.contains("eur") || rawS.contains("€")) return Currency.EUR;
        if (s.contains("usd") || rawS.contains("$") || s.contains("dolar")) return Currency.USD;
        if (s.contains("try") || s.contains("tl") || rawS.contains("₺") || s.contains("lira")) return Currency.TRY;
        if (classifier.normalizeText(brand).contains("huawei")) return Currency.EUR;
        return Currency.USD;
    }
}
