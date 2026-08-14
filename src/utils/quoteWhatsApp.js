// Teklifi WhatsApp üzerinden paylaşma yardımcıları.
// CartDrawer tarafından DİNAMİK import edilir.
//
// İki yol vardır:
//  1) Web Share API (navigator.share) ile GERÇEK PDF dosyasını paylaşmak
//     (mobilde WhatsApp'ı da içeren OS paylaşım menüsü açılır).
//  2) Desteklenmiyorsa (masaüstü / eski tarayıcı) fallback: teklifin metin
//     özetini wa.me linkiyle açmak + PDF'i indirmek.

import { formatPrice } from "./format";

// Teklifin okunabilir metin özetini üretir (wa.me fallback ve share text).
export function buildQuoteText({
  items = [],
  totalsByCurrency = {},
  seller = {},
  contact = {},
  margin,
}) {
  const marginPct = Number(margin) || 0;
  const factor = 1 + marginPct / 100;
  const lines = [];

  lines.push("*FİYAT TEKLİFİ*");
  if (seller.firma) lines.push(`Teklifi veren: ${seller.firma}`);
  if (contact.firma || contact.yetkili) {
    const alan = [contact.firma, contact.yetkili].filter(Boolean).join(" / ");
    lines.push(`Teklifi alan: ${alan}`);
  }
  lines.push("");

  // Ürün satırları (kâr marjı eklenmiş birim fiyat üzerinden).
  lines.push("*Ürünler*");
  for (const { product, qty } of items) {
    const ad = [product.brand, product.name].filter(Boolean).join(" ");
    if (product.price == null) {
      lines.push(`• ${ad} — ${qty} adet (fiyat isteyin)`);
    } else {
      const lineTotal = product.price * factor * qty;
      lines.push(
        `• ${ad} — ${qty} adet — ${formatPrice(lineTotal, product.currency)}`,
      );
    }
  }
  lines.push("");

  // Para birimi bazında KDV dahil genel toplam (CartDrawer özetiyle aynı mantık).
  const currencies = Object.keys(totalsByCurrency);
  if (currencies.length > 0) {
    lines.push("*Toplam (KDV dahil)*");
    for (const cur of currencies) {
      const before = Number(totalsByCurrency[cur]) || 0;
      const after = before + before * (marginPct / 100);
      const grandTotal = after + after * 0.2;
      lines.push(`${cur}: ${formatPrice(grandTotal, cur)}`);
    }
    lines.push("");
  }

  lines.push(
    "Bu bir fiyat teklifidir; ödeme veya satın alma işlemi içermez.",
  );

  return lines.join("\n");
}

// wa.me uluslararası format ister: sadece rakam, başında + yok.
function normalizePhone(phone) {
  return String(phone || "").replace(/\D/g, "");
}

// Teklifi WhatsApp'tan paylaşır. Mobilde gerçek PDF dosyasını, masaüstünde
// yalnızca teklif metnini (wa.me) kullanır.
export async function shareQuoteOnWhatsApp({ blob, fileName, text, phone }) {
  // Masaüstü tarayıcılar (Chrome/Edge) artık dosya paylaşımını destekliyor ve
  // Web Share dalı OS'in genel paylaşım menüsünü açıyor; bu da wa.me'nin
  // uygulama/web yönlendirmesini atlıyor. Bu yüzden Web Share'i yalnızca
  // mobilde kullan, masaüstünde doğrudan wa.me'ye düş.
  const isMobile =
    typeof navigator !== "undefined" &&
    (navigator.userAgentData?.mobile ??
      /Mobi|Android|iPhone|iPad|iPod/i.test(navigator.userAgent));

  // 1) Web Share API — gerçek dosya paylaşımı (yalnızca mobil).
  if (isMobile && blob && typeof navigator !== "undefined" && navigator.canShare) {
    const file = new File([blob], fileName, { type: "application/pdf" });
    if (navigator.canShare({ files: [file] })) {
      try {
        await navigator.share({ files: [file], title: fileName, text });
        return { method: "share" };
      } catch (err) {
        // Kullanıcı iptal ettiyse sessizce geç, fallback'e düşme.
        if (err && err.name === "AbortError") return { method: "cancelled" };
        // Diğer hatalarda fallback'e devam.
      }
    }
  }

  // 2) Fallback (masaüstü) — indirme yok; yalnızca wa.me metin linki.
  const num = normalizePhone(phone);
  const base = num ? `https://wa.me/${num}` : "https://wa.me/";
  const url = `${base}?text=${encodeURIComponent(text)}`;
  window.open(url, "_blank", "noopener,noreferrer");
  return { method: "wa.me" };
}
