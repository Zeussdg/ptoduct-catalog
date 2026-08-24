// Admin sipariş detayı: kalem düzenleme (adet/birim fiyat değiştirme, özel fiyat uygulama,
// ürün ekleme/silme). ProductCombo/qqty markup ve davranışı teklif sihirbazı (quote-builder.js)
// ile aynı desende — burada ayrıca birim fiyat girilebilir ve arama admin'e özel JSON uç noktasına gider.
(function () {
  "use strict";

  var SYMBOLS = { USD: "$", EUR: "€", TRY: "₺" };

  function formatPrice(price, currency) {
    if (price == null || isNaN(price)) return "—";
    var sym = SYMBOLS[currency] || (currency + " ");
    return sym + Number(price).toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  function esc(s) {
    return String(s == null ? "" : s).replace(/[&<>"]/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c];
    });
  }

  var orderId, existingBody, newBody, rowTemplate;
  var toggleBtn, saveBtn, cancelBtn, addBtn, actionsEl;
  var form, payloadInput;
  var searchTimer = null;
  var editing = false;

  function setEditing(on) {
    editing = on;
    Array.prototype.forEach.call(document.querySelectorAll(".oed-view"), function (el) { el.style.display = on ? "none" : ""; });
    Array.prototype.forEach.call(document.querySelectorAll(".oed-edit"), function (el) { el.style.display = on ? "" : "none"; });
    Array.prototype.forEach.call(document.querySelectorAll(".oed-only"), function (el) { el.style.display = on ? "" : "none"; });
    if (actionsEl) actionsEl.style.display = on ? "flex" : "none";
    toggleBtn.textContent = on ? "Düzenlemeyi Bırak" : "Düzenle";
    if (!on) { newBody.innerHTML = ""; }
  }

  function bindExistingRow(row) {
    var qtyInput = row.querySelector(".oed-qty");
    var priceInput = row.querySelector(".oed-price");
    var listSelect = row.querySelector(".oed-list-select");
    row._priceIncludesVat = row.getAttribute("data-price-includes-vat") === "true";

    function recompute() {
      var qty = parseInt(qtyInput.value, 10) || 0;
      var price = parseFloat(priceInput.value);
      var currency = row.getAttribute("data-currency");
      row.querySelector(".oed-row-total").textContent = isNaN(price) ? "—" : formatPrice(qty * price, currency);
    }

    qtyInput.addEventListener("input", recompute);
    priceInput.addEventListener("input", function () {
      row._priceIncludesVat = false;
      recompute();
    });

    if (listSelect) {
      listSelect.addEventListener("change", function () {
        if (!listSelect.value) return;
        priceInput.value = listSelect.value;
        row.setAttribute("data-currency", listSelect.selectedOptions[0].getAttribute("data-currency"));
        row._priceIncludesVat = true;
        recompute();
      });
    }

    row.querySelector(".oed-remove").addEventListener("click", function () {
      if (!window.confirm("Bu kalemi siparişten kaldırmak istediğinize emin misiniz?")) return;
      row.remove();
    });
  }

  function addNewRow() {
    var frag = rowTemplate.content.cloneNode(true);
    var row = frag.querySelector(".oed-new-row");
    newBody.appendChild(row);
    bindNewRow(row);
  }

  function bindNewRow(row) {
    row._product = null;
    row._qty = 1;
    row._products = [];

    var trigger = row.querySelector(".js-combo-trigger");
    var panel = row.querySelector(".js-combo-panel");
    var search = row.querySelector(".js-combo-search");
    var list = row.querySelector(".js-combo-list");
    var qtyBox = row.querySelector(".js-qty");
    var qtyInput = qtyBox.querySelector("input");
    var qm = qtyBox.querySelector("[data-qm]");
    var qp = qtyBox.querySelector("[data-qp]");
    var priceInput = row.querySelector(".js-new-price");

    trigger.addEventListener("click", function () {
      var open = panel.hasAttribute("hidden");
      closeAllCombos();
      if (open) {
        panel.removeAttribute("hidden");
        row.querySelector(".js-combo").classList.add("pcombo--open");
        trigger.setAttribute("aria-expanded", "true");
        search.value = "";
        list.innerHTML = '<li class="pcombo__empty">Aramak için yazın…</li>';
        search.focus();
      }
    });

    search.addEventListener("input", function () {
      var term = search.value.trim();
      if (searchTimer) clearTimeout(searchTimer);
      if (!term) { list.innerHTML = '<li class="pcombo__empty">Aramak için yazın…</li>'; return; }
      searchTimer = setTimeout(function () {
        fetch("/admin/orders/" + orderId + "/products/search?q=" + encodeURIComponent(term))
          .then(function (r) { return r.json(); })
          .then(function (items) { renderList(row, list, items); })
          .catch(function () { list.innerHTML = '<li class="pcombo__empty">Arama başarısız.</li>'; });
      }, 300);
    });

    list.addEventListener("click", function (e) {
      var b = e.target.closest(".pcombo__opt");
      if (!b) return;
      var id = b.getAttribute("data-id");
      var p = row._products.filter(function (x) { return String(x.id) === String(id); })[0];
      if (p) pickProduct(row, p);
    });

    qm.addEventListener("click", function () { setRowQty(row, row._qty - 1); });
    qp.addEventListener("click", function () { setRowQty(row, row._qty + 1); });
    qtyInput.addEventListener("change", function () { setRowQty(row, parseInt(qtyInput.value, 10) || 1); });
    priceInput.addEventListener("input", function () {
      row._priceIncludesVat = false;
      updateNewRowTotal(row);
    });

    var listSelect = row.querySelector(".js-new-list-select");
    listSelect.addEventListener("change", function () {
      if (!listSelect.value) return;
      priceInput.value = listSelect.value;
      row.setAttribute("data-currency", listSelect.selectedOptions[0].getAttribute("data-currency"));
      row._priceIncludesVat = true;
      updateNewRowTotal(row);
    });

    row.querySelector(".js-remove").addEventListener("click", function () { row.remove(); });
  }

  function renderList(row, list, items) {
    row._products = items || [];
    if (row._products.length === 0) { list.innerHTML = '<li class="pcombo__empty">Ürün bulunamadı.</li>'; return; }
    list.innerHTML = row._products.map(function (p) {
      var badge = (p.listMatches && p.listMatches.length > 0) ? ' <span class="adp__badge">Fiyat Listesinde</span>' : "";
      return '<li><button type="button" class="pcombo__opt" data-id="' + esc(p.id) + '" role="option">'
        + '<span class="pcombo__opt-name"><span class="pcombo__opt-brand">' + esc(p.brand) + '</span> ' + esc(p.name) + badge + '</span>'
        + '<span class="pcombo__opt-code mono">' + esc(p.stockCode) + '</span></button></li>';
    }).join("");
  }

  function pickProduct(row, p) {
    row._product = p;
    row._priceIncludesVat = false;
    var t = row.querySelector(".js-combo-trigger");
    t.classList.remove("pcombo__trigger--empty");
    t.querySelector(".pcombo__label").textContent = p.brand + " " + p.name;
    t.setAttribute("aria-expanded", "false");
    row.querySelector(".js-combo-panel").setAttribute("hidden", "");
    row.querySelector(".js-combo").classList.remove("pcombo--open");

    var qtyBox = row.querySelector(".js-qty");
    qtyBox.classList.remove("qqty--disabled");
    qtyBox.querySelector("input").disabled = false;
    qtyBox.querySelector("[data-qp]").disabled = false;
    var priceInput = row.querySelector(".js-new-price");
    priceInput.disabled = false;
    priceInput.value = p.price;
    row.setAttribute("data-currency", p.currency);

    var listSelect = row.querySelector(".js-new-list-select");
    var matches = p.listMatches || [];
    if (matches.length > 0) {
      listSelect.innerHTML = '<option value="" selected>Fiyat listesinden seç…</option>' + matches.map(function (m) {
        return '<option value="' + esc(m.price) + '" data-currency="' + esc(m.currency) + '">'
          + esc(m.priceListName) + ' — ' + esc(formatPrice(m.price, m.currency)) + '</option>';
      }).join("");
      listSelect.style.display = "";
    } else {
      listSelect.innerHTML = '<option value="" selected>Fiyat listesinden seç…</option>';
      listSelect.style.display = "none";
    }

    setRowQty(row, 1);
  }

  function setRowQty(row, q) {
    q = Math.max(1, q);
    row._qty = q;
    var qtyBox = row.querySelector(".js-qty");
    qtyBox.querySelector("input").value = q;
    qtyBox.querySelector("[data-qm]").disabled = (q <= 1) || !row._product;
    updateNewRowTotal(row);
  }

  function updateNewRowTotal(row) {
    if (!row._product) { row.querySelector(".js-new-total").textContent = "—"; return; }
    var priceInput = row.querySelector(".js-new-price");
    var price = parseFloat(priceInput.value) || 0;
    var currency = row.getAttribute("data-currency") || row._product.currency;
    row.querySelector(".js-new-total").textContent = formatPrice(price * row._qty, currency);
  }

  function closeAllCombos() {
    Array.prototype.forEach.call(document.querySelectorAll(".js-combo"), function (c) {
      c.classList.remove("pcombo--open");
      c.querySelector(".js-combo-panel").setAttribute("hidden", "");
      c.querySelector(".js-combo-trigger").setAttribute("aria-expanded", "false");
    });
  }

  function collectLines() {
    var lines = [];
    Array.prototype.forEach.call(existingBody.querySelectorAll("tr"), function (row) {
      lines.push({
        itemId: parseInt(row.getAttribute("data-item-id"), 10),
        qty: parseInt(row.querySelector(".oed-qty").value, 10),
        unitPrice: parseFloat(row.querySelector(".oed-price").value),
        currency: row.getAttribute("data-currency"),
        priceIncludesVat: !!row._priceIncludesVat
      });
    });
    Array.prototype.forEach.call(newBody.querySelectorAll(".oed-new-row"), function (row) {
      if (!row._product) return;
      lines.push({
        productId: row._product.id,
        qty: row._qty,
        unitPrice: parseFloat(row.querySelector(".js-new-price").value),
        currency: row.getAttribute("data-currency") || row._product.currency,
        priceIncludesVat: !!row._priceIncludesVat
      });
    });
    return lines;
  }

  function save() {
    var lines = collectLines();
    if (lines.length === 0) { alert("Siparişte en az bir ürün kalemi kalmalı."); return; }
    for (var i = 0; i < lines.length; i++) {
      if (!lines[i].qty || lines[i].qty < 1) { alert("Adet en az 1 olmalıdır."); return; }
      if (isNaN(lines[i].unitPrice) || lines[i].unitPrice < 0) { alert("Birim fiyat geçersiz."); return; }
    }
    payloadInput.value = JSON.stringify({ lines: lines });
    form.submit();
  }

  document.addEventListener("DOMContentLoaded", function () {
    var root = document.getElementById("oedRoot");
    if (!root) return;

    var parts = window.location.pathname.split("/").filter(Boolean);
    orderId = parts[parts.length - 1];

    existingBody = document.getElementById("oedExistingRows");
    newBody = document.getElementById("oedNewRows");
    rowTemplate = document.getElementById("oedRowTemplate");
    toggleBtn = document.getElementById("oedToggle");
    saveBtn = document.getElementById("oedSave");
    cancelBtn = document.getElementById("oedCancel");
    addBtn = document.getElementById("oedAddRow");
    actionsEl = document.getElementById("oedActions");
    form = document.getElementById("oedForm");
    payloadInput = document.getElementById("oedPayload");
    if (!existingBody || !rowTemplate) return;

    Array.prototype.forEach.call(existingBody.querySelectorAll("tr"), bindExistingRow);

    toggleBtn.addEventListener("click", function () { setEditing(!editing); });
    addBtn.addEventListener("click", addNewRow);
    saveBtn.addEventListener("click", save);
    cancelBtn.addEventListener("click", function () { window.location.reload(); });

    document.addEventListener("mousedown", function (e) {
      if (!e.target.closest(".js-combo")) closeAllCombos();
    });
  });
})();
