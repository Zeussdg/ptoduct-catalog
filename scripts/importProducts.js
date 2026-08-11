#!/usr/bin/env node
/**
 * Excel → JSON ürün import pipeline'ı (build-time).
 *
 *   data/products.xlsx  →  node scripts/importProducts.js  →  src/data/products.json
 *                                                          →  src/data/categories.json
 *
 * Adımlar:
 *   1. Excel dosyasını bulur ve okur (data/products.xlsx ya da data/products/*.xlsx)
 *   2. Başlık satırını ve sütunları otomatik tespit eder (Türkçe başlık eşleme)
 *   3. Satırları normalize eder (fiyat / para birimi / metin temizliği)
 *   4. Ham kategori + ürün adına göre kategori hiyerarşisini üretir
 *   5. Aynı ürünlerin tekrarını (stok kodu / ada göre) ayıklar
 *   6. products.json ve categories.json dosyalarını yazar
 *   7. Konsola analiz raporu basar
 *
 * Excel bulunamazsa mevcut JSON dosyalarına DOKUNMAZ (uygulama çalışmaya
 * devam eder) ve nasıl kullanılacağını açıklayan bir mesaj gösterir.
 */

import { readFileSync, writeFileSync, existsSync, readdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";
import * as XLSX from "xlsx";
import { classify, categoryRules, FALLBACK, slugify, normalizeText } from "./categoryMap.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "..");
const DATA_DIR = path.join(ROOT, "data");
const OUT_PRODUCTS = path.join(ROOT, "src", "data", "products.json");
const OUT_CATEGORIES = path.join(ROOT, "src", "data", "categories.json");

// ---- Sütun eşleme sözlüğü ------------------------------------------------
// Her mantıksal alan için Excel başlığında aranacak aday kelimeler.
// Normalize edilmiş (küçük harf, aksansız) başlıklarda "içerir" mantığıyla
// eşleşir; ilk eşleşen sütun kazanır.
const COLUMN_ALIASES = {
  brand: ["marka", "brand", "uretici", "üretici", "firma"],
  stockCode: ["stok kodu", "stok kod", "stokkodu", "urun kodu", "ürün kodu", "malzeme kodu", "barkod", "sku", "kod", "code"],
  name: ["urun adi", "ürün adı", "urun ismi", "malzeme adi", "stok adi", "stok adı", "product name", "product", "urun", "ürün", "tanim", "tanım", "ad", "isim", "name"],
  category: ["alt kategori", "kategori", "urun grubu", "ürün grubu", "grup", "category", "tip", "sinif", "sınıf"],
  price: ["liste fiyat", "birim fiyat", "satis fiyat", "satış fiyat", "fiyat", "price", "tutar", "amount"],
  currency: ["para birimi", "para birim", "doviz", "döviz", "currency", "kur", "birim"],
  description: ["aciklama", "açıklama", "description", "detay", "ozellik", "özellik"],
};

// Bir alan için başlık satırındaki en uygun sütun indeksini bulur.
function matchColumn(headers, aliases) {
  const norm = headers.map((h) => normalizeText(h));
  // Önce tam eşleşme, sonra "içerir" eşleşmesi (daha güvenilir sıralama).
  for (const alias of aliases) {
    const a = normalizeText(alias);
    const exact = norm.findIndex((h) => h === a);
    if (exact !== -1) return exact;
  }
  for (const alias of aliases) {
    const a = normalizeText(alias);
    const partial = norm.findIndex((h) => h && h.includes(a));
    if (partial !== -1) return partial;
  }
  return -1;
}

// Başlık satırını bulur: ilk 15 satır içinde, bilinen alanlardan en çok
// eşleşeni sağlayan satır başlık kabul edilir.
function detectHeaderRow(rows) {
  let best = { index: -1, score: -1, map: null };
  const limit = Math.min(rows.length, 15);
  for (let i = 0; i < limit; i++) {
    const headers = (rows[i] || []).map((c) => (c == null ? "" : String(c)));
    if (headers.every((h) => !h.trim())) continue;
    const map = {};
    let score = 0;
    for (const [field, aliases] of Object.entries(COLUMN_ALIASES)) {
      const idx = matchColumn(headers, aliases);
      map[field] = idx;
      if (idx !== -1) score++;
    }
    if (score > best.score) best = { index: i, score, map, headers };
  }
  return best;
}

// "1.234,56", "1,234.56", "1234,5", "$12.5" gibi biçimleri sayıya çevirir.
function parsePrice(raw) {
  if (raw == null || raw === "") return null;
  if (typeof raw === "number") return Number.isFinite(raw) ? raw : null;
  let s = String(raw).trim().replace(/[^\d.,-]/g, "");
  if (!s) return null;
  const hasComma = s.includes(",");
  const hasDot = s.includes(".");
  if (hasComma && hasDot) {
    // Son görünen ayraç ondalık ayracıdır.
    if (s.lastIndexOf(",") > s.lastIndexOf(".")) {
      s = s.replace(/\./g, "").replace(",", "."); // TR: 1.234,56
    } else {
      s = s.replace(/,/g, ""); // EN: 1,234.56
    }
  } else if (hasComma) {
    s = s.replace(",", "."); // 1234,56 → 1234.56
  }
  const n = Number.parseFloat(s);
  return Number.isFinite(n) ? n : null;
}

// Para birimini sembol / metin / marka üzerinden normalize eder.
function normalizeCurrency(raw, brand) {
  const s = normalizeText(raw);
  if (s.includes("eur") || s.includes("€")) return "EUR";
  if (s.includes("usd") || s.includes("$") || s.includes("dolar")) return "USD";
  if (s.includes("try") || s.includes("tl") || s.includes("₺") || s.includes("lira")) return "TRY";
  if (String(raw).includes("€")) return "EUR";
  if (String(raw).includes("$")) return "USD";
  if (String(raw).includes("₺")) return "TRY";
  // Bilinen marka bazlı varsayılan: HUAWEI fiyatları çoğunlukla EUR.
  if (normalizeText(brand).includes("huawei")) return "EUR";
  return "USD";
}

function cleanText(raw) {
  if (raw == null) return "";
  return String(raw).replace(/\s+/g, " ").trim();
}

// ---- Ana akış ------------------------------------------------------------

function findExcelFile() {
  const candidates = [];
  const direct = ["products.xlsx", "urunler.xlsx", "products.xls", "urunler.xls"];
  for (const f of direct) {
    const p = path.join(DATA_DIR, f);
    if (existsSync(p)) candidates.push(p);
  }
  const subDir = path.join(DATA_DIR, "products");
  for (const dir of [DATA_DIR, subDir]) {
    if (existsSync(dir)) {
      for (const f of readdirSync(dir)) {
        if (/\.(xlsx|xls)$/i.test(f)) {
          const p = path.join(dir, f);
          if (!candidates.includes(p)) candidates.push(p);
        }
      }
    }
  }
  return candidates[0] || null;
}

function main() {
  const excelPath = findExcelFile();

  if (!excelPath) {
    console.log("\n⚠️  Excel dosyası bulunamadı.");
    console.log("   Ürünleri içe aktarmak için bir Excel dosyası ekleyin:\n");
    console.log("     data/products.xlsx");
    console.log("   veya");
    console.log("     data/products/urunler.xlsx\n");
    console.log("   Ardından tekrar çalıştırın:  npm run import-products");
    console.log("   (Mevcut src/data/*.json dosyalarına dokunulmadı.)\n");
    return;
  }

  console.log(`\n📄 Excel okunuyor: ${path.relative(ROOT, excelPath)}`);
  const wb = XLSX.read(readFileSync(excelPath), { type: "buffer" });
  const sheetName = wb.SheetNames[0];
  const sheet = wb.Sheets[sheetName];
  const rows = XLSX.utils.sheet_to_json(sheet, { header: 1, raw: true, defval: "" });
  console.log(`   Sayfa: "${sheetName}"  •  ${rows.length} satır`);

  const header = detectHeaderRow(rows);
  if (header.index === -1 || header.score < 1) {
    console.error("\n❌ Başlık satırı tespit edilemedi. Excel'de en azından bir");
    console.error("   'Ürün Adı' / 'Marka' / 'Stok Kodu' sütunu bulunmalı.\n");
    process.exitCode = 1;
    return;
  }

  const { map, headers } = header;
  console.log(`\n🔎 Başlık satırı: ${header.index + 1}. satır  •  eşleşen alan: ${header.score}/7`);
  console.log("   Sütun eşleşmeleri:");
  for (const field of Object.keys(COLUMN_ALIASES)) {
    const idx = map[field];
    const label = idx === -1 ? "— (yok)" : `"${cleanText(headers[idx])}"`;
    console.log(`     ${field.padEnd(12)} → ${label}`);
  }

  // ---- Satırları normalize et ----
  const dataRows = rows.slice(header.index + 1);
  const normalized = [];
  let skippedEmpty = 0;

  for (const row of dataRows) {
    const get = (field) => (map[field] === -1 ? "" : row[map[field]]);

    let name = cleanText(get("name"));
    let brand = cleanText(get("brand"));
    const rawCategory = cleanText(get("category"));
    const stockCode = cleanText(get("stockCode"));

    // Ürün adı zorunlu; yoksa açıklamadan / stok kodundan türet.
    if (!name) name = cleanText(get("description"));
    if (!name) {
      skippedEmpty++;
      continue;
    }

    // Marka yoksa ürün adının ilk kelimesinden tahmin et.
    if (!brand) {
      const first = name.split(" ")[0];
      brand = /^[A-Za-zÇĞİÖŞÜ0-9-]{2,}$/.test(first) ? first.toUpperCase() : "GENEL";
    } else {
      brand = brand.toUpperCase();
    }

    const price = parsePrice(get("price"));
    const currency = normalizeCurrency(get("currency"), brand);
    const description = cleanText(get("description")) || name;
    const cat = classify(name, rawCategory);

    normalized.push({
      brand,
      name,
      stockCode,
      mainCategory: cat.mainCategory,
      mainCategorySlug: cat.mainCategorySlug,
      category: cat.category,
      categorySlug: cat.categorySlug,
      price,
      currency,
      description,
      image: null,
      _classified: cat.classified,
    });
  }

  // ---- Tekrarları ayıkla ----
  // Anahtar: stok kodu (varsa) yoksa marka + normalize edilmiş ad.
  const seen = new Map();
  let duplicates = 0;
  const unique = [];
  for (const p of normalized) {
    const key = p.stockCode
      ? `sk:${normalizeText(p.stockCode)}`
      : `nm:${normalizeText(p.brand)}|${normalizeText(p.name)}`;
    if (seen.has(key)) {
      duplicates++;
      continue;
    }
    seen.set(key, true);
    unique.push(p);
  }

  // ---- id ata + iç alanları temizle ----
  const products = unique.map((p, i) => {
    const { _classified, ...rest } = p;
    return { id: i + 1, ...rest };
  });

  // ---- categories.json üret (yalnızca ürünü olan kategoriler) ----
  const usedSubSlugs = new Set(products.map((p) => p.categorySlug));
  const usedMainSlugs = new Set(products.map((p) => p.mainCategorySlug));

  const allRules = [
    ...categoryRules,
    { main: FALLBACK.main, subs: [{ name: FALLBACK.sub, keywords: [] }] },
  ];

  const categories = [];
  for (const rule of allRules) {
    const mainSlug = slugify(rule.main);
    if (!usedMainSlugs.has(mainSlug)) continue;
    const subCategories = rule.subs
      .map((s) => ({ name: s.name, slug: `${mainSlug}--${slugify(s.name)}` }))
      .filter((s) => usedSubSlugs.has(s.slug));
    if (subCategories.length === 0) continue;
    categories.push({ name: rule.main, slug: mainSlug, subCategories });
  }

  // ---- Dosyaları yaz ----
  writeFileSync(OUT_PRODUCTS, JSON.stringify(products, null, 2) + "\n");
  writeFileSync(OUT_CATEGORIES, JSON.stringify(categories, null, 2) + "\n");

  // ---- Rapor ----
  const brands = [...new Set(products.map((p) => p.brand))].sort();
  const unclassified = normalized.filter((p) => !p._classified).length;
  const withPrice = products.filter((p) => p.price != null).length;
  const byCurrency = products.reduce((acc, p) => {
    acc[p.currency] = (acc[p.currency] || 0) + 1;
    return acc;
  }, {});
  const byMain = products.reduce((acc, p) => {
    acc[p.mainCategory] = (acc[p.mainCategory] || 0) + 1;
    return acc;
  }, {});

  console.log("\n📊 Analiz raporu");
  console.log("   ─────────────────────────────────────────");
  console.log(`   Okunan satır          : ${dataRows.length}`);
  console.log(`   Boş / atlanan         : ${skippedEmpty}`);
  console.log(`   Tekrar (ayıklanan)    : ${duplicates}`);
  console.log(`   Toplam ürün           : ${products.length}`);
  console.log(`   Fiyatlı ürün          : ${withPrice}`);
  console.log(`   Sınıflandırılamayan   : ${unclassified}  → "${FALLBACK.main}"`);
  console.log(`   Marka sayısı          : ${brands.length}  (${brands.slice(0, 8).join(", ")}${brands.length > 8 ? " …" : ""})`);
  console.log(`   Para birimi           : ${Object.entries(byCurrency).map(([k, v]) => `${k}:${v}`).join("  ")}`);
  console.log("   Kategori dağılımı:");
  for (const [k, v] of Object.entries(byMain).sort((a, b) => b[1] - a[1])) {
    console.log(`     ${String(v).padStart(4)}  ${k}`);
  }
  console.log("   ─────────────────────────────────────────");
  console.log(`\n✅ Yazıldı: ${path.relative(ROOT, OUT_PRODUCTS)}  (${products.length} ürün)`);
  console.log(`✅ Yazıldı: ${path.relative(ROOT, OUT_CATEGORIES)}  (${categories.length} ana kategori)\n`);

  if (unclassified > 0) {
    console.log(`ℹ️  ${unclassified} ürün sınıflandırılamadı. Bunları uygun kategoriye`);
    console.log("   çekmek için scripts/categoryMap.js dosyasına anahtar kelime ekleyin.\n");
  }
}

main();
