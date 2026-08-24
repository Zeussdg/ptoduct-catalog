package com.ikibm.catalog.service;

import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.Product;
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
 * Bir fiyat listesi için Excel'den toplu ürün fiyatı içe aktarma — CustomerPriceImportService'teki
 * esnek başlık tespiti/eşleme desenini taklit eder. Stok koduna göre ürünü bulur,
 * PriceListService.addItem (upsert) ile kaydeder. Fiyatlar KDV hariç (net) olarak girilir.
 */
@Service
public class PriceListItemImportService {

    private static final Map<String, List<String>> COLUMN_ALIASES = new LinkedHashMap<>();
    static {
        COLUMN_ALIASES.put("stockCode", List.of("stok kodu", "stok kod", "stokkodu", "urun kodu", "ürün kodu", "sku", "kod", "code"));
        COLUMN_ALIASES.put("price", List.of("fiyat", "kdv haric fiyat", "kdv hariç fiyat", "liste fiyat", "price"));
        COLUMN_ALIASES.put("currency", List.of("para birimi", "para birim", "doviz", "döviz", "currency"));
    }

    private final ProductRepository productRepository;
    private final PriceListService priceListService;
    private final CategoryClassifier classifier;

    public PriceListItemImportService(ProductRepository productRepository, PriceListService priceListService,
                                      CategoryClassifier classifier) {
        this.productRepository = productRepository;
        this.priceListService = priceListService;
        this.classifier = classifier;
    }

    public record Report(int rowsRead, int created, int updated, int notFound, int invalidPrice,
                         boolean ok, String message) {}

    @Transactional
    public Report importForPriceList(Integer priceListId, InputStream in) {
        List<List<String>> rows;
        try (Workbook wb = WorkbookFactory.create(in)) {
            rows = readRows(wb.getSheetAt(0));
        } catch (Exception e) {
            return new Report(0, 0, 0, 0, 0, false, "Excel okunamadı: " + e.getMessage());
        }

        int[] header = detectHeaderRow(rows);
        int headerIndex = header[0], score = header[1];
        if (headerIndex == -1 || score < 2) {
            return new Report(rows.size(), 0, 0, 0, 0, false,
                    "Başlık satırı tespit edilemedi (Stok Kodu ve Fiyat sütunları gerekli).");
        }

        Map<String, Integer> map = new HashMap<>();
        List<String> headers = rows.get(headerIndex);
        for (var e : COLUMN_ALIASES.entrySet()) {
            map.put(e.getKey(), matchColumn(headers, e.getValue()));
        }
        if (map.get("stockCode") == -1 || map.get("price") == -1) {
            return new Report(rows.size(), 0, 0, 0, 0, false,
                    "Stok Kodu ve Fiyat sütunları bulunamadı.");
        }

        int rowsRead = 0, created = 0, updated = 0, notFound = 0, invalidPrice = 0;

        for (int i = headerIndex + 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.stream().allMatch(s -> s == null || s.isBlank())) continue;
            rowsRead++;

            String stockCode = clean(get(row, map, "stockCode"));
            if (stockCode.isEmpty()) { notFound++; continue; }
            Product product = productRepository.findByStockCode(stockCode).orElse(null);
            if (product == null) { notFound++; continue; }

            BigDecimal price = parsePrice(get(row, map, "price"));
            if (price == null) { invalidPrice++; continue; }

            Currency currency = parseCurrency(get(row, map, "currency"), product.getCurrency());

            boolean existed = priceListService.exists(priceListId, product.getId());
            priceListService.addItem(priceListId, product.getId(), price, currency);
            if (existed) updated++; else created++;
        }

        String msg = String.format("%d ürün eklendi, %d güncellendi (%d ürün kodu bulunamadı, %d geçersiz fiyat).",
                created, updated, notFound, invalidPrice);
        return new Report(rowsRead, created, updated, notFound, invalidPrice, true, msg);
    }

    /** Doğru başlıklarla, örnek satırlar içeren indirilebilir şablon (aynı COLUMN_ALIASES'tan üretilir). */
    public byte[] buildTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Fiyat Listesi");
            String[] headers = {"Stok Kodu", "Fiyat", "Para Birimi"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

            Row example1 = sheet.createRow(1);
            String[] v1 = {"S105", "3.50", "USD"};
            for (int i = 0; i < v1.length; i++) example1.createCell(i).setCellValue(v1[i]);

            Row example2 = sheet.createRow(2);
            String[] v2 = {"S108", "5.00", ""};
            for (int i = 0; i < v2.length; i++) example2.createCell(i).setCellValue(v2[i]);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Şablon oluşturulamadı: " + e.getMessage(), e);
        }
    }

    // ---- Excel okuma (CustomerPriceImportService ile aynı desen) ----

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
                    cells.add(cellString(row.getCell(c), fmt));
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

    private Currency parseCurrency(String raw, Currency fallback) {
        String s = classifier.normalizeText(raw);
        String rawS = raw == null ? "" : raw;
        if (s.isBlank()) return fallback;
        if (s.contains("eur") || rawS.contains("€")) return Currency.EUR;
        if (s.contains("usd") || rawS.contains("$") || s.contains("dolar")) return Currency.USD;
        if (s.contains("try") || s.contains("tl") || rawS.contains("₺") || s.contains("lira")) return Currency.TRY;
        return fallback;
    }
}
