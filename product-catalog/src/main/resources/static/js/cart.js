// İstemci sepeti (localStorage) + slide-in drawer + teklif modu.
// React CartContext + CartDrawer.jsx davranışının vanilla karşılığı. Girişsiz
// "sepete ekle", drawer, para birimi bazlı toplamlar, %20 KDV ve teklif formu
// (satıcı/alıcı/kâr marjı) → "Teklifi PDF'e geçir" POST /quote/pdf.
(function () {
  "use strict";

  var KEY = "catalog_cart_v1";
  var VAT = 0.2;
  var SYMBOLS = { USD: "$", EUR: "€", TRY: "₺" };

  // ---- durum ----
  var open = false;
  var quoteMode = false;
  var seller = { firma: "", yetkili: "", telefon: "", eposta: "" };
  var contact = { firma: "", yetkili: "", telefon: "" };
  var margin = "";
  var exporting = false;
  // Aynı sepet+form içeriği (imza) için üretilen PDF'i önbelleğe alır; "PDF'e geçir"
  // ve "WhatsApp'tan Paylaş" aynı sepeti hedefliyorsa tek /quote/pdf çağrısı (ve
  // dolayısıyla tek kayıtlı Quote) paylaşılsın diye — sepet değişince imza değişir,
  // önbellek doğal olarak geçersiz kalır ve yeni bir teklif haklı olarak oluşur.
  var lastQuoteExport = null;

  // ---- yardımcılar ----
  function read() { try { return JSON.parse(localStorage.getItem(KEY)) || []; } catch (e) { return []; } }
  function write(items) { localStorage.setItem(KEY, JSON.stringify(items)); }
  function esc(s) { return String(s == null ? "" : s).replace(/[&<>"]/g, function (c) {
    return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]; }); }

  function formatPrice(price, currency) {
    if (price == null || isNaN(price)) return "Fiyat isteyin";
    var sym = SYMBOLS[currency] || (currency + " ");
    return sym + Number(price).toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  function totalCount(items) { return items.reduce(function (s, it) { return s + it.qty; }, 0); }

  function totalsByCurrency(items) {
    var t = {};
    items.forEach(function (it) {
      if (it.price == null || isNaN(it.price)) return;
      t[it.currency] = (t[it.currency] || 0) + it.price * it.qty;
    });
    return t;
  }

  function updateBadge() {
    var badge = document.getElementById("cartBadge");
    if (!badge) return;
    var n = totalCount(read());
    badge.textContent = n;
    if (n > 0) badge.removeAttribute("hidden"); else badge.setAttribute("hidden", "");
  }

  // ---- mutasyonlar ----
  function addItem(p, qty) {
    var items = read();
    var ex = items.find(function (it) { return String(it.id) === String(p.id); });
    if (ex) ex.qty += (qty || 1);
    else items.push({ id: p.id, name: p.name, brand: p.brand, code: p.code,
                      price: p.price, currency: p.currency, qty: qty || 1 });
    write(items); updateBadge();
  }
  function setQty(id, qty) {
    var items = read();
    var it = items.find(function (x) { return String(x.id) === String(id); });
    if (!it) return;
    it.qty = Math.max(1, qty);
    write(items); updateBadge(); render();
  }
  // + / - gibi tam render tetiklemeden adedi günceller; klavyeden yazarken
  // (input olayı) her tuş vuruşunda tüm listeyi yeniden çizmemek için kullanılır,
  // aksi halde input focus/imleç konumu kaybolur.
  function setQtyNoRender(id, qty) {
    var items = read();
    var it = items.find(function (x) { return String(x.id) === String(id); });
    if (!it) return;
    it.qty = Math.max(1, qty);
    write(items); updateBadge();
  }
  function removeItem(id) {
    write(read().filter(function (x) { return String(x.id) !== String(id); }));
    updateBadge(); render();
  }
  function clearCart() { write([]); updateBadge(); render(); }

  // ---- drawer ----
  function openDrawer() { open = true; render(); }
  function closeDrawer() { open = false; quoteMode = false; render(); }

  function footerTotalsHtml(items) {
    var totals = totalsByCurrency(items);
    var curs = Object.keys(totals);
    return curs.map(function (cur) {
      return '<div class="cdrw__total-row"><span>Ara Toplam (' + cur + ')</span><span class="mono">' + formatPrice(totals[cur], cur) + '</span></div>';
    }).join("")
    + curs.map(function (cur) {
      return '<div class="cdrw__total-row"><span>KDV (%20) (' + cur + ')</span><span class="mono">' + formatPrice(totals[cur] * VAT, cur) + '</span></div>';
    }).join("")
    + curs.map(function (cur) {
      return '<div class="cdrw__total-row cdrw__total-row--grand"><span>Genel Toplam (' + cur + ')</span><span class="mono">' + formatPrice(totals[cur] * (1 + VAT), cur) + '</span></div>';
    }).join("");
  }

  function summaryHtml(items) {
    var totals = totalsByCurrency(items);
    var curs = Object.keys(totals);
    var pct = Number(margin) || 0;
    if (curs.length === 0) return '<p class="cdrw__summary-empty">Fiyatlandırılabilir ürün bulunmuyor.</p>';
    return curs.map(function (cur) {
      var before = totals[cur];
      var marginAmt = before * (pct / 100);
      var after = before + marginAmt;
      var vatAmt = after * VAT;
      var grand = after + vatAmt;
      return '<div class="cdrw__summary-group">'
        + '<div class="cdrw__summary-row"><span>Ara Toplam (' + cur + ')</span><span class="mono">' + formatPrice(before, cur) + '</span></div>'
        + '<div class="cdrw__summary-row cdrw__summary-row--muted"><span>Kâr Marjı (%' + pct + ')</span><span class="mono">+' + formatPrice(marginAmt, cur) + '</span></div>'
        + '<div class="cdrw__summary-row cdrw__summary-row--muted"><span>KDV (%20)</span><span class="mono">+' + formatPrice(vatAmt, cur) + '</span></div>'
        + '<div class="cdrw__summary-row cdrw__summary-row--total"><span>Teklif Toplamı (' + cur + ')</span><span class="mono">' + formatPrice(grand, cur) + '</span></div>'
        + '</div>';
    }).join("");
  }

  function render() {
    var aside = document.getElementById("cartDrawer");
    var overlay = document.getElementById("cartOverlay");
    var content = document.getElementById("cartContent");
    var title = document.getElementById("cartTitle");
    if (!aside || !content) return;

    aside.className = "cdrw" + (open ? " cdrw--open" : "") + (quoteMode ? " cdrw--quote" : "");
    if (overlay) overlay.className = "cdrw__overlay" + (open ? " cdrw__overlay--visible" : "");
    if (title) title.textContent = quoteMode ? "Teklif Oluştur" : "Sepet";

    var items = read();
    if (items.length === 0) {
      content.innerHTML = '<div class="cdrw__empty"><p>Sepetiniz boş.</p>'
        + '<span>Ürün listesinden "Sepete Ekle" ile ürün ekleyebilirsiniz.</span></div>';
      return;
    }

    var totals = totalsByCurrency(items);
    var curs = Object.keys(totals);

    var listHtml = items.map(function (it) {
      var line = (it.price == null || isNaN(it.price)) ? "—" : formatPrice(it.price * it.qty, it.currency);
      return '<li class="cdrw__item"><div class="cdrw__item-info">'
        + '<span class="cdrw__item-brand">' + esc(it.brand) + '</span>'
        + '<span class="cdrw__item-name">' + esc(it.name) + '</span>'
        + '<span class="cdrw__item-code mono">' + esc(it.code) + '</span></div>'
        + '<div class="cdrw__item-controls"><div class="cdrw__qty">'
        + '<button type="button" data-act="dec" data-id="' + esc(it.id) + '" aria-label="Azalt">−</button>'
        + '<input type="number" min="1" inputMode="numeric" class="cdrw__qty-input mono" '
        + 'data-act="qtyinput" data-id="' + esc(it.id) + '" value="' + it.qty + '" aria-label="Adet">'
        + '<button type="button" data-act="inc" data-id="' + esc(it.id) + '" aria-label="Artır">+</button></div>'
        + '<span class="cdrw__item-price mono" data-price-for="' + esc(it.id) + '">' + line + '</span>'
        + '<button type="button" class="cdrw__remove" data-act="rm" data-id="' + esc(it.id) + '" aria-label="Kaldır">Kaldır</button>'
        + '</div></li>';
    }).join("");

    var footerTotals = footerTotalsHtml(items);

    var quotePanel = "";
    if (quoteMode) {
      quotePanel = '<div class="cdrw__quote">'
        + '<div class="cdrw__form">'
        + '<div class="cdrw__form-section"><h3>Teklifi Veren</h3>'
        + field("Firma", "s_firma", seller.firma, "Firma adı")
        + field("Yetkili", "s_yetkili", seller.yetkili, "Yetkili kişi")
        + field("Telefon", "s_telefon", seller.telefon, "+90 ...")
        + field("E-posta", "s_eposta", seller.eposta, "satis@firma.com")
        + '</div>'
        + '<div class="cdrw__form-section"><h3>Teklifi Alan</h3>'
        + field("Firma", "c_firma", contact.firma, "Müşteri firma adı")
        + field("Yetkili", "c_yetkili", contact.yetkili, "İlgili kişi")
        + field("Telefon (WhatsApp)", "c_telefon", contact.telefon, "+90 ...", "tel")
        + '</div></div>'
        + '<label class="cdrw__field cdrw__margin"><span>Kâr Marjı (%)</span>'
        + '<input type="number" min="0" step="1" data-fld="margin" value="' + esc(margin) + '" placeholder="0"/></label>'
        + '<div class="cdrw__summary" id="cartSummary">' + summaryHtml(items) + '</div>'
        + '<div class="cdrw__actions">'
        + '<button type="button" class="cdrw__pdf" data-act="pdf"' + (curs.length === 0 || exporting ? " disabled" : "") + '>'
        + (exporting ? "Hazırlanıyor…" : "Teklifi PDF\'e geçir") + '</button>'
        + '<button type="button" class="cdrw__whatsapp" data-act="whatsapp"' + (curs.length === 0 || exporting ? " disabled" : "") + '>'
        + (exporting ? "Hazırlanıyor…" : "WhatsApp\'tan Paylaş") + '</button>'
        + '</div>'
        + '</div>';
    }

    content.innerHTML = '<div class="cdrw__body">' + quotePanel
      + '<div class="cdrw__cart"><ul class="cdrw__list">' + listHtml + '</ul>'
      + '<div class="cdrw__footer"><div class="cdrw__totals" id="cartTotals">' + footerTotals + '</div>'
      + (quoteMode ? "" : '<button type="button" class="cdrw__quote-btn" data-act="toquote">Teklif Oluştur</button>')
      + '<button type="button" class="cdrw__clear" data-act="clear">Sepeti Temizle</button>'
      + (quoteMode ? "" : '<button type="button" class="cdrw__order-btn" data-act="order">Sipariş Oluştur</button>')
      + '<p class="cdrw__note">Bu bir teklif listesidir; ödeme veya satın alma işlemi içermez.</p>'
      + '</div></div></div>';

    bindFormInputs();
  }

  function field(label, key, val, ph, type) {
    return '<label class="cdrw__field"><span>' + label + '</span>'
      + '<input type="' + (type || "text") + '" data-fld="' + key + '" value="' + esc(val) + '" placeholder="' + ph + '"/></label>';
  }

  function bindFormInputs() {
    Array.prototype.forEach.call(document.querySelectorAll("#cartContent [data-fld]"), function (inp) {
      inp.addEventListener("input", function () {
        var k = inp.getAttribute("data-fld");
        if (k === "margin") { margin = inp.value; }
        else if (k.indexOf("s_") === 0) { seller[k.slice(2)] = inp.value; }
        else if (k.indexOf("c_") === 0) { contact[k.slice(2)] = inp.value; }
        if (k === "margin") {
          var box = document.getElementById("cartSummary");
          if (box) box.innerHTML = summaryHtml(read());
        }
      });
    });

    // Adet kutusu: yazarken tam render tetiklemeden sadece ilgili satırın
    // fiyatını ve toplamları günceller (focus/imleç kaybolmasın diye).
    Array.prototype.forEach.call(document.querySelectorAll('#cartContent [data-act="qtyinput"]'), function (inp) {
      inp.addEventListener("input", function () {
        var id = inp.getAttribute("data-id");
        var n = parseInt(inp.value, 10);
        if (!Number.isFinite(n) || n <= 0) return;
        setQtyNoRender(id, n);
        var items = read();
        var it = items.find(function (x) { return String(x.id) === String(id); });
        var priceEl = document.querySelector('[data-price-for="' + id + '"]');
        if (priceEl && it) {
          priceEl.textContent = (it.price == null || isNaN(it.price)) ? "—" : formatPrice(it.price * it.qty, it.currency);
        }
        var totalsEl = document.getElementById("cartTotals");
        if (totalsEl) totalsEl.innerHTML = footerTotalsHtml(items);
        if (quoteMode) {
          var box = document.getElementById("cartSummary");
          if (box) box.innerHTML = summaryHtml(items);
        }
      });
      inp.addEventListener("blur", function () {
        var id = inp.getAttribute("data-id");
        var it = read().find(function (x) { return String(x.id) === String(id); });
        if (it) inp.value = it.qty;
      });
      inp.addEventListener("focus", function () { inp.select(); });
    });
  }

  // Aynı sepet+form imzası için daha önce üretilmiş bir PDF varsa (ör. az önce
  // "PDF'e geçir"e basıldıysa) onu yeniden kullanır — /quote/pdf her çağrıda bir
  // Quote kaydettiği için, aynı tekliften iki farklı dağıtım kanalı (PDF/WhatsApp)
  // kullanılınca mükerrer kayıt oluşmasın diye. Promise'in kendisi önbelleğe alınır
  // ki neredeyse eşzamanlı çift tıklamalar da tek isteğe düşsün.
  function getOrCreateQuotePdf(payload) {
    var sig = JSON.stringify(payload);
    if (lastQuoteExport && lastQuoteExport.signature === sig) return lastQuoteExport.promise;
    var csrfToken = document.querySelector('meta[name="_csrf"]');
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]');
    var headers = { "Content-Type": "application/x-www-form-urlencoded" };
    if (csrfToken && csrfHeader) headers[csrfHeader.getAttribute("content")] = csrfToken.getAttribute("content");
    var promise = fetch("/quote/pdf", {
      method: "POST", headers: headers, body: "payload=" + encodeURIComponent(JSON.stringify(payload))
    }).then(function (res) {
      if (!res.ok) throw new Error("PDF oluşturulamadı");
      var cd = res.headers.get("Content-Disposition");
      var filename = "teklif.pdf";
      var m = cd && /filename="?([^";]+)"?/.exec(cd);
      if (m) filename = m[1];
      return res.blob().then(function (blob) { return { blob: blob, filename: filename }; });
    });
    lastQuoteExport = { signature: sig, promise: promise };
    promise.catch(function () {
      if (lastQuoteExport && lastQuoteExport.signature === sig) lastQuoteExport = null;
    });
    return promise;
  }

  function downloadBlob(blob, filename) {
    var url = URL.createObjectURL(blob);
    var a = document.createElement("a");
    a.href = url; a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
  }

  // Sepetten tek adımda teklif + sipariş oluşturur (toptancıya tedarik siparişi).
  function handleCreateOrderFromCart() {
    var items = read();
    if (items.length === 0) return;
    if (!confirm("Sepetteki ürünler için toptancıya sipariş oluşturulacak. Emin misiniz?")) return;
    var form = document.createElement("form");
    form.method = "POST";
    form.action = "/orders/from-cart";
    form.style.display = "none";
    add(form, "payload", JSON.stringify({ items: items }));
    var token = document.querySelector('meta[name="_csrf"]');
    if (token) add(form, "_csrf", token.getAttribute("content"));
    document.body.appendChild(form);
    form.submit();
    function add(f, n, v) { var i = document.createElement("input"); i.type = "hidden"; i.name = n; i.value = v; f.appendChild(i); }
  }

  function submitPdf() {
    var items = read();
    if (items.length === 0 || exporting) return;
    var payload = { items: items, seller: seller, contact: contact, margin: Number(margin) || 0 };
    exporting = true; render();
    getOrCreateQuotePdf(payload).then(function (r) {
      downloadBlob(r.blob, r.filename);
    }).catch(function (err) {
      console.error("PDF oluşturulamadı:", err);
      alert("PDF oluşturulurken bir hata oluştu. Lütfen tekrar deneyin.");
    }).finally(function () {
      exporting = false; render();
    });
  }

  // ---- WhatsApp'tan paylaş ----
  // Teklifin okunabilir metin özetini üretir (wa.me fallback ve dosya paylaşım metni).
  function buildQuoteText(items, totals, sellerInfo, contactInfo, marginPct) {
    var factor = 1 + (Number(marginPct) || 0) / 100;
    var lines = [];
    lines.push("*FİYAT TEKLİFİ*");
    if (sellerInfo.firma) lines.push("Teklifi veren: " + sellerInfo.firma);
    if (contactInfo.firma || contactInfo.yetkili) {
      var alan = [contactInfo.firma, contactInfo.yetkili].filter(function (x) { return x; }).join(" / ");
      lines.push("Teklifi alan: " + alan);
    }
    lines.push("");

    lines.push("*Ürünler*");
    items.forEach(function (it) {
      var ad = [it.brand, it.name].filter(function (x) { return x; }).join(" ");
      if (it.price == null || isNaN(it.price)) {
        lines.push("• " + ad + " — " + it.qty + " adet (fiyat isteyin)");
      } else {
        var lineTotal = it.price * factor * it.qty;
        lines.push("• " + ad + " — " + it.qty + " adet — " + formatPrice(lineTotal, it.currency));
      }
    });
    lines.push("");

    var curs = Object.keys(totals);
    if (curs.length > 0) {
      lines.push("*Toplam (KDV dahil)*");
      curs.forEach(function (cur) {
        var before = Number(totals[cur]) || 0;
        var after = before + before * ((Number(marginPct) || 0) / 100);
        var grand = after + after * VAT;
        lines.push(cur + ": " + formatPrice(grand, cur));
      });
      lines.push("");
    }

    lines.push("Bu bir fiyat teklifidir; ödeme veya satın alma işlemi içermez.");
    return lines.join("\n");
  }

  // wa.me uluslararası format ister: sadece rakam, başında + yok.
  function normalizePhone(phone) {
    return String(phone || "").replace(/\D/g, "");
  }

  // Mobilde Web Share API ile gerçek PDF dosyasını paylaşır (OS paylaşım menüsü
  // WhatsApp'ı da içerir); desteklenmiyorsa (masaüstü) yalnızca metni wa.me ile açar.
  function shareOnWhatsApp(opts) {
    var blob = opts.blob, fileName = opts.fileName, text = opts.text, phone = opts.phone;
    var isMobile = typeof navigator !== "undefined"
      && ((navigator.userAgentData && navigator.userAgentData.mobile)
          || /Mobi|Android|iPhone|iPad|iPod/i.test(navigator.userAgent));

    if (isMobile && blob && typeof navigator !== "undefined" && navigator.canShare) {
      var file = new File([blob], fileName, { type: "application/pdf" });
      if (navigator.canShare({ files: [file] })) {
        return navigator.share({ files: [file], title: fileName, text: text })
          .then(function () { return { method: "share" }; })
          .catch(function (err) {
            if (err && err.name === "AbortError") return { method: "cancelled" };
            return openWaMe(phone, text);
          });
      }
    }
    return Promise.resolve(openWaMe(phone, text));
  }

  function openWaMe(phone, text) {
    var num = normalizePhone(phone);
    var base = num ? ("https://wa.me/" + num) : "https://wa.me/";
    window.open(base + "?text=" + encodeURIComponent(text), "_blank", "noopener,noreferrer");
    return { method: "wa.me" };
  }

  function handleWhatsApp() {
    var items = read();
    if (items.length === 0 || exporting) return;
    exporting = true;
    render();

    var payload = { items: items, seller: seller, contact: contact, margin: Number(margin) || 0 };
    getOrCreateQuotePdf(payload).then(function (r) {
      var text = buildQuoteText(items, totalsByCurrency(items), seller, contact, margin);
      return shareOnWhatsApp({ blob: r.blob, fileName: r.filename, text: text, phone: contact.telefon });
    }).catch(function (err) {
      console.error("WhatsApp paylaşımı başarısız:", err);
      alert("Teklif paylaşılırken bir hata oluştu. Lütfen tekrar deneyin.");
    }).finally(function () {
      exporting = false;
      render();
    });
  }

  // ---- olay bağlama ----
  document.addEventListener("DOMContentLoaded", function () {
    updateBadge();

    // sepete ekle butonları (kart + detay)
    Array.prototype.forEach.call(document.querySelectorAll(".js-add-to-cart"), function (btn) {
      btn.addEventListener("click", function () {
        var qty = 1;
        var target = btn.getAttribute("data-qty-target");
        if (target) { var el = document.getElementById(target); if (el) qty = parseInt(el.textContent, 10) || 1; }
        addItem({
          id: btn.getAttribute("data-id"), name: btn.getAttribute("data-name"),
          brand: btn.getAttribute("data-brand"), code: btn.getAttribute("data-code"),
          price: parseFloat(btn.getAttribute("data-price")), currency: btn.getAttribute("data-currency")
        }, qty);
        openDrawer();
      });
    });

    // qty stepper (ürün detay)
    Array.prototype.forEach.call(document.querySelectorAll("[data-qtybox]"), function (box) {
      var val = box.querySelector("[data-qtyval]");
      box.addEventListener("click", function (e) {
        var t = e.target.closest("[data-qtyminus],[data-qtyplus]");
        if (!t || !val) return;
        var n = parseInt(val.textContent, 10) || 1;
        n = t.hasAttribute("data-qtyplus") ? n + 1 : Math.max(1, n - 1);
        val.textContent = n;
      });
    });

    // drawer aç/kapa tetikleyicileri
    var openBtn = document.getElementById("cartOpenBtn");
    var openFooter = document.getElementById("cartOpenFooter");
    if (openBtn) openBtn.addEventListener("click", openDrawer);
    if (openFooter) openFooter.addEventListener("click", openDrawer);
    var closeBtn = document.getElementById("cartClose");
    var overlay = document.getElementById("cartOverlay");
    if (closeBtn) closeBtn.addEventListener("click", closeDrawer);
    if (overlay) overlay.addEventListener("click", closeDrawer);

    // drawer içi eylemler (delegasyon)
    var content = document.getElementById("cartContent");
    if (content) content.addEventListener("click", function (e) {
      var b = e.target.closest("[data-act]");
      if (!b) return;
      var act = b.getAttribute("data-act");
      var id = b.getAttribute("data-id");
      var items = read();
      var it = id ? items.find(function (x) { return String(x.id) === String(id); }) : null;
      if (act === "inc" && it) setQty(id, it.qty + 1);
      else if (act === "dec" && it) setQty(id, it.qty - 1);
      else if (act === "rm") removeItem(id);
      else if (act === "clear") clearCart();
      else if (act === "toquote") { quoteMode = true; render(); }
      else if (act === "pdf") submitPdf();
      else if (act === "whatsapp") handleWhatsApp();
      else if (act === "order") handleCreateOrderFromCart();
    });

    render();
  });

  window.CatalogCart = { addItem: addItem, read: read, updateBadge: updateBadge, open: openDrawer };
})();
