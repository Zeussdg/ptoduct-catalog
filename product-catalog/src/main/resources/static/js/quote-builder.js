// Teklif sihirbazı — React QuoteBuilderPage + QuoteRow + ProductCombo + QuantityControl
// davranışının vanilla karşılığı. Kategoriye göre ürünleri /api/catalog/products'tan
// çeker, aranabilir combobox sunar, para birimi bazlı KDV'li toplamları hesaplar,
// "Sepete Taşı" ile istemci sepetine ekler.
(function () {
  "use strict";

  var VAT = 0.2;
  var SYMBOLS = { USD: "$", EUR: "€", TRY: "₺" };
  var INITIAL_ROWS = 6;

  var rowsEl, tmpl, summaryEl;
  var productCache = {};

  function trlower(s) { return (s || "").toLocaleLowerCase("tr-TR"); }
  function formatPrice(price, currency) {
    if (price == null || isNaN(price)) return "—";
    var sym = SYMBOLS[currency] || (currency + " ");
    return sym + Number(price).toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  function initials(text) {
    var t = (text || "").trim(); if (!t) return "";
    var w = t.split(/\s+/);
    return (w.length === 1 ? w[0].slice(0, 2) : (w[0][0] + w[1][0])).toUpperCase();
  }
  function esc(s) { return String(s == null ? "" : s).replace(/[&<>"]/g, function (c) {
    return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]; }); }

  function addRow() {
    var frag = tmpl.content.cloneNode(true);
    var row = frag.querySelector(".qrow");
    rowsEl.appendChild(row);
    bindRow(row);
    return row;
  }

  function bindRow(row) {
    row._products = [];
    row._product = null;
    row._qty = 1;

    var cat = row.querySelector(".js-cat");
    var trigger = row.querySelector(".js-combo-trigger");
    var panel = row.querySelector(".js-combo-panel");
    var search = row.querySelector(".js-combo-search");
    var list = row.querySelector(".js-combo-list");
    var qtyBox = row.querySelector(".js-qty");
    var qtyInput = qtyBox.querySelector("input");
    var qm = qtyBox.querySelector("[data-qm]");
    var qp = qtyBox.querySelector("[data-qp]");

    cat.addEventListener("change", function () {
      var slug = cat.value;
      row._product = null;
      resetProductUI(row);
      if (!slug) { disableCombo(row); recompute(); return; }
      fetchProducts(slug).then(function (items) {
        row._products = items || [];
        enableCombo(row);
        recompute();
      });
    });

    trigger.addEventListener("click", function () {
      if (trigger.disabled) return;
      var open = panel.hasAttribute("hidden");
      closeAllCombos();
      if (open) {
        panel.removeAttribute("hidden");
        row.querySelector(".js-combo").classList.add("pcombo--open");
        trigger.setAttribute("aria-expanded", "true");
        renderList(row, "");
        search.value = ""; search.focus();
      }
    });

    search.addEventListener("input", function () { renderList(row, search.value); });

    list.addEventListener("click", function (e) {
      var b = e.target.closest(".pcombo__opt");
      if (!b) return;
      var id = b.getAttribute("data-id");
      var p = row._products.find(function (x) { return String(x.id) === String(id); });
      if (p) pick(row, p);
    });

    qm.addEventListener("click", function () { setQty(row, row._qty - 1); });
    qp.addEventListener("click", function () { setQty(row, row._qty + 1); });
    qtyInput.addEventListener("change", function () { setQty(row, parseInt(qtyInput.value, 10) || 1); });

    row.querySelector(".js-remove").addEventListener("click", function () {
      row.remove();
      if (!rowsEl.children.length) addRow();
      recompute();
    });
  }

  function disableCombo(row) {
    var t = row.querySelector(".js-combo-trigger");
    t.disabled = true; t.classList.add("pcombo__trigger--empty");
    t.querySelector(".pcombo__label").textContent = "Lütfen önce kategori seçiniz";
    row.querySelector(".js-combo-panel").setAttribute("hidden", "");
  }
  function enableCombo(row) {
    var t = row.querySelector(".js-combo-trigger");
    t.disabled = false; t.classList.add("pcombo__trigger--empty");
    t.querySelector(".pcombo__label").textContent = "Ürün Seçiniz";
  }
  function resetProductUI(row) {
    row._qty = 1;
    row.querySelector(".js-thumb").textContent = "";
    row.querySelector(".js-price").textContent = "—";
    row.querySelector(".js-total").textContent = "—";
    var qtyBox = row.querySelector(".js-qty");
    qtyBox.classList.add("qqty--disabled");
    qtyBox.querySelector("input").value = 1;
    qtyBox.querySelector("input").disabled = true;
    qtyBox.querySelector("[data-qm]").disabled = true;
    qtyBox.querySelector("[data-qp]").disabled = true;
  }

  function renderList(row, query) {
    var list = row.querySelector(".js-combo-list");
    var q = trlower(query.trim());
    var items = !q ? row._products : row._products.filter(function (p) {
      return trlower(p.name).indexOf(q) >= 0 || trlower(p.brand).indexOf(q) >= 0 || trlower(p.stockCode).indexOf(q) >= 0;
    });
    if (items.length === 0) { list.innerHTML = '<li class="pcombo__empty">Ürün bulunamadı.</li>'; return; }
    list.innerHTML = items.map(function (p) {
      var active = row._product && String(row._product.id) === String(p.id);
      return '<li><button type="button" class="pcombo__opt' + (active ? " pcombo__opt--active" : "") + '" data-id="' + esc(p.id) + '" role="option">'
        + '<span class="pcombo__opt-name"><span class="pcombo__opt-brand">' + esc(p.brand) + '</span> ' + esc(p.name) + '</span>'
        + '<span class="pcombo__opt-code mono">' + esc(p.stockCode) + '</span></button></li>';
    }).join("");
  }

  function pick(row, p) {
    row._product = p;
    var t = row.querySelector(".js-combo-trigger");
    t.classList.remove("pcombo__trigger--empty");
    t.querySelector(".pcombo__label").textContent = p.brand + " " + p.name;
    t.setAttribute("aria-expanded", "false");
    row.querySelector(".js-combo-panel").setAttribute("hidden", "");
    row.querySelector(".js-combo").classList.remove("pcombo--open");
    row.querySelector(".js-thumb").textContent = initials(p.brand);
    var qtyBox = row.querySelector(".js-qty");
    qtyBox.classList.remove("qqty--disabled");
    qtyBox.querySelector("input").disabled = false;
    qtyBox.querySelector("[data-qp]").disabled = false;
    setQty(row, row._qty);
    updateRowPrice(row);
    recompute();
  }

  function setQty(row, q) {
    q = Math.max(1, q);
    row._qty = q;
    var qtyBox = row.querySelector(".js-qty");
    qtyBox.querySelector("input").value = q;
    qtyBox.querySelector("[data-qm]").disabled = (q <= 1) || !row._product;
    updateRowPrice(row);
    recompute();
  }

  function updateRowPrice(row) {
    var p = row._product;
    if (!p || p.price == null) {
      row.querySelector(".js-price").textContent = "—";
      row.querySelector(".js-total").textContent = "—";
      return;
    }
    row.querySelector(".js-price").textContent = formatPrice(p.price, p.currency);
    row.querySelector(".js-total").textContent = formatPrice(p.price * row._qty, p.currency);
  }

  function recompute() {
    var totals = {};
    Array.prototype.forEach.call(rowsEl.children, function (row) {
      var p = row._product;
      if (!p || p.price == null) return;
      var cur = p.currency || "USD";
      totals[cur] = (totals[cur] || 0) + p.price * row._qty;
    });
    renderSummary(totals);
  }

  function renderSummary(totals) {
    var curs = Object.keys(totals);
    var html = '<h2 class="qsum__title">Toplam</h2>';
    if (curs.length === 0) {
      html += '<p class="qsum__empty">Henüz ürün seçilmedi.</p>';
    } else {
      html += curs.map(function (cur) {
        var base = totals[cur], vat = base * VAT, grand = base + vat;
        return '<div class="qsum__group">'
          + '<div class="qsum__row"><span>Ara Toplam (' + cur + ')</span><span class="mono">' + formatPrice(base, cur) + '</span></div>'
          + '<div class="qsum__row qsum__row--muted"><span>KDV %20 (' + cur + ')</span><span class="mono">' + formatPrice(vat, cur) + '</span></div>'
          + '<div class="qsum__row qsum__row--total"><span>Genel Toplam (' + cur + ')</span><span class="mono">' + formatPrice(grand, cur) + '</span></div>'
          + '</div>';
      }).join("");
    }
    summaryEl.innerHTML = html;
  }

  function fetchProducts(slug) {
    if (productCache[slug]) return Promise.resolve(productCache[slug]);
    return fetch("/api/catalog/products?categorySlug=" + encodeURIComponent(slug))
      .then(function (r) { return r.json(); })
      .then(function (items) { productCache[slug] = items; return items; })
      .catch(function () { return []; });
  }

  function closeAllCombos() {
    Array.prototype.forEach.call(document.querySelectorAll(".js-combo"), function (c) {
      c.classList.remove("pcombo--open");
      c.querySelector(".js-combo-panel").setAttribute("hidden", "");
      c.querySelector(".js-combo-trigger").setAttribute("aria-expanded", "false");
    });
  }

  function toCart() {
    var selected = [];
    Array.prototype.forEach.call(rowsEl.children, function (row) {
      if (row._product) selected.push({ p: row._product, qty: row._qty });
    });
    if (selected.length === 0) { alert("Lütfen sepete eklemek için en az bir ürün seçiniz."); return; }
    selected.forEach(function (s) {
      window.CatalogCart.addItem({ id: s.p.id, name: s.p.name, brand: s.p.brand,
        code: s.p.stockCode, price: s.p.price, currency: s.p.currency }, s.qty);
    });
    if (window.CatalogCart.open) window.CatalogCart.open();
  }

  function clearAll() {
    var hasSel = Array.prototype.some.call(rowsEl.children, function (r) { return r._product; });
    if (hasSel && !window.confirm("Seçilen tüm ürünler temizlensin mi?")) return;
    rowsEl.innerHTML = "";
    addRow();
    recompute();
  }

  document.addEventListener("DOMContentLoaded", function () {
    rowsEl = document.getElementById("qrows");
    tmpl = document.getElementById("qrowTemplate");
    summaryEl = document.getElementById("quoteSummary");
    if (!rowsEl || !tmpl) return;

    for (var i = 0; i < INITIAL_ROWS; i++) addRow();

    document.getElementById("qbpAddRow").addEventListener("click", addRow);
    document.getElementById("qbpClear").addEventListener("click", clearAll);
    document.getElementById("qbpToCart").addEventListener("click", toCart);

    var stock = document.getElementById("qbpStock");
    stock.addEventListener("click", function () {
      var on = stock.getAttribute("aria-pressed") === "true";
      stock.setAttribute("aria-pressed", on ? "false" : "true");
      stock.classList.toggle("qbp__stock--on", !on);
      stock.querySelector(".qbp__stock-box").textContent = on ? "" : "✓";
    });

    document.addEventListener("mousedown", function (e) {
      if (!e.target.closest(".js-combo")) closeAllCombos();
    });
  });
})();
