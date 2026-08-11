// Teklif PDF üreticisi (jsPDF + jspdf-autotable).
// CartDrawer tarafından DİNAMİK import edilir → jspdf ve gömülü font ana
// bundle'a girmez, yalnızca "Teklifi PDF'e geçir" tıklanınca yüklenir.

import { jsPDF } from "jspdf";
import autoTable from "jspdf-autotable";
import { formatPrice } from "./format";
import { robotoRegularBase64, robotoBoldBase64 } from "./robotoFont";

// ---- Gönderen firma bilgisi -------------------------------------------------
// Kendi firma bilgilerinizi buradan düzenleyin; teklifin başlığında görünür.
const COMPANY = {
  name: "Ürün Kataloğu Ticaret A.Ş.",
  line1: "TENDA · TELESIS · HUAWEI · HIKVISION Yetkili Satıcısı",
  phone: "+90 (000) 000 00 00",
  email: "satis@firmaniz.com",
};

const FONT = "Roboto";
const SIGNAL = [31, 95, 209]; // --signal-600
const INK = [16, 21, 29]; // --ink-900
const INK_500 = [107, 118, 132];

function ensureFont(doc) {
  doc.addFileToVFS("Roboto-Regular.ttf", robotoRegularBase64);
  doc.addFont("Roboto-Regular.ttf", FONT, "normal");
  doc.addFileToVFS("Roboto-Bold.ttf", robotoBoldBase64);
  doc.addFont("Roboto-Bold.ttf", FONT, "bold");
  doc.setFont(FONT, "normal");
}

function two(n) {
  return String(n).padStart(2, "0");
}

function quoteMeta() {
  const d = new Date();
  const date = `${two(d.getDate())}.${two(d.getMonth() + 1)}.${d.getFullYear()}`;
  const no = `TKLF-${d.getFullYear()}${two(d.getMonth() + 1)}${two(d.getDate())}-${two(d.getHours())}${two(d.getMinutes())}`;
  const fileDate = `${d.getFullYear()}-${two(d.getMonth() + 1)}-${two(d.getDate())}`;
  return { date, no, fileDate };
}

export async function generateQuotePdf({ items, totalsByCurrency, contact, margin }) {
  const marginPct = Number(margin) || 0;
  const factor = 1 + marginPct / 100;
  const meta = quoteMeta();

  const doc = new jsPDF({ unit: "mm", format: "a4" });
  ensureFont(doc);
  const pageW = doc.internal.pageSize.getWidth();
  const M = 14; // sayfa kenar boşluğu

  // ---- Başlık: gönderen firma (sol) ----
  doc.setFont(FONT, "bold");
  doc.setFontSize(15);
  doc.setTextColor(...INK);
  doc.text(COMPANY.name, M, 20);

  doc.setFont(FONT, "normal");
  doc.setFontSize(9);
  doc.setTextColor(...INK_500);
  doc.text(COMPANY.line1, M, 26);
  doc.text(`Tel: ${COMPANY.phone}`, M, 31);
  doc.text(`E-posta: ${COMPANY.email}`, M, 36);

  // ---- Başlık: teklif bilgisi (sağ) ----
  doc.setFont(FONT, "bold");
  doc.setFontSize(20);
  doc.setTextColor(...SIGNAL);
  doc.text("FİYAT TEKLİFİ", pageW - M, 20, { align: "right" });
  doc.setFont(FONT, "normal");
  doc.setFontSize(9.5);
  doc.setTextColor(...INK);
  doc.text(`Tarih: ${meta.date}`, pageW - M, 27, { align: "right" });
  doc.text(`Teklif No: ${meta.no}`, pageW - M, 32, { align: "right" });

  // Ayraç çizgi
  doc.setDrawColor(...SIGNAL);
  doc.setLineWidth(0.6);
  doc.line(M, 41, pageW - M, 41);

  // ---- Müşteri bloğu ----
  let y = 50;
  doc.setFont(FONT, "bold");
  doc.setFontSize(10);
  doc.setTextColor(...INK_500);
  doc.text("MÜŞTERİ", M, y);
  y += 6;
  doc.setFontSize(10.5);
  doc.setTextColor(...INK);
  const cRows = [
    ["Firma", contact.firma],
    ["Yetkili", contact.yetkili],
    ["Telefon", contact.telefon],
    ["E-posta", contact.eposta],
  ].filter(([, v]) => v && String(v).trim());

  if (cRows.length === 0) {
    doc.setFont(FONT, "normal");
    doc.setTextColor(...INK_500);
    doc.text("—", M, y);
    y += 6;
  } else {
    for (const [label, val] of cRows) {
      doc.setFont(FONT, "bold");
      doc.text(`${label}:`, M, y);
      doc.setFont(FONT, "normal");
      doc.text(String(val), M + 24, y);
      y += 5.5;
    }
  }

  // ---- Kalem tablosu ----
  const body = items.map((it) => {
    const { product, qty } = it;
    const unit = product.price != null ? formatPrice(product.price, product.currency) : "Fiyat isteyin";
    const line = product.price != null ? formatPrice(product.price * qty, product.currency) : "—";
    return [
      product.brand || "",
      product.name || "",
      product.stockCode || "",
      String(qty),
      unit,
      product.currency || "",
      line,
    ];
  });

  autoTable(doc, {
    startY: y + 4,
    head: [["Marka", "Ürün Adı", "Stok Kodu", "Adet", "Birim Fiyat", "Döviz", "Tutar"]],
    body,
    theme: "striped",
    styles: { font: FONT, fontSize: 8.5, cellPadding: 2, textColor: INK, lineColor: [227, 231, 236] },
    headStyles: { font: FONT, fontStyle: "bold", fillColor: SIGNAL, textColor: [255, 255, 255] },
    alternateRowStyles: { fillColor: [247, 248, 250] },
    columnStyles: {
      0: { cellWidth: 22 },
      2: { cellWidth: 24 },
      3: { cellWidth: 12, halign: "right" },
      4: { cellWidth: 24, halign: "right" },
      5: { cellWidth: 14, halign: "center" },
      6: { cellWidth: 24, halign: "right", fontStyle: "bold" },
    },
    margin: { left: M, right: M },
  });

  // ---- Özet: para birimi bazında marj öncesi / sonrası ----
  let sy = doc.lastAutoTable.finalY + 10;
  const currencies = Object.keys(totalsByCurrency);

  const boxW = 82;
  const boxX = pageW - M - boxW;

  const drawSummaryLine = (label, value, opts = {}) => {
    doc.setFont(FONT, opts.bold ? "bold" : "normal");
    doc.setFontSize(opts.size || 9.5);
    doc.setTextColor(...(opts.color || INK));
    doc.text(label, boxX, sy);
    doc.text(value, pageW - M, sy, { align: "right" });
    sy += opts.gap || 5.5;
  };

  if (currencies.length === 0) {
    doc.setFont(FONT, "normal");
    doc.setFontSize(9.5);
    doc.setTextColor(...INK_500);
    doc.text("Fiyatlandırılabilir ürün bulunmuyor.", boxX, sy);
  }

  currencies.forEach((cur, idx) => {
    const before = totalsByCurrency[cur];
    const marginAmt = before * (marginPct / 100);
    const after = before * factor;

    if (idx > 0) sy += 3;
    drawSummaryLine(`Ara Toplam (${cur})`, formatPrice(before, cur), { color: INK_500 });
    drawSummaryLine(`Kâr Marjı (%${marginPct})`, `+${formatPrice(marginAmt, cur)}`, { color: INK_500 });

    // sonrası vurgulu satır
    doc.setDrawColor(227, 231, 236);
    doc.setLineWidth(0.3);
    doc.line(boxX, sy - 2, pageW - M, sy - 2);
    drawSummaryLine(`Teklif Toplamı (${cur})`, formatPrice(after, cur), {
      bold: true,
      size: 11,
      color: SIGNAL,
      gap: 7,
    });
  });

  // ---- Alt not ----
  const pageH = doc.internal.pageSize.getHeight();
  doc.setFont(FONT, "normal");
  doc.setFontSize(8);
  doc.setTextColor(...INK_500);
  doc.text(
    "Bu bir fiyat teklifidir; ödeme veya satın alma işlemi içermez. Fiyatlar döviz bazında geçerlidir; farklı para birimleri toplanmaz.",
    M,
    pageH - 12,
    { maxWidth: pageW - 2 * M }
  );

  doc.save(`teklif-${meta.fileDate}.pdf`);
}
