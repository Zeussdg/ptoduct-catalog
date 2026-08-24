// Admin fiyat listesi detayı: aranan ürünlerden çoklu seçim yapıp her birine ayrı
// KDV hariç fiyat girerek listeye ekleme.
(function () {
  "use strict";

  function esc(s) {
    return String(s == null ? "" : s).replace(/[&<>"]/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c];
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    var root = document.getElementById("pliRoot");
    if (!root) return;

    var pickBtn = document.getElementById("pliPickBtn");
    var rowsEl = document.getElementById("pliPriceRows");
    var saveArea = document.getElementById("pliSaveArea");
    var saveBtn = document.getElementById("pliSave");
    var form = document.getElementById("pliForm");
    var payloadInput = document.getElementById("pliPayload");

    if (pickBtn) {
      pickBtn.addEventListener("click", function () {
        var checked = Array.prototype.filter.call(document.querySelectorAll(".js-pli-check"), function (c) { return c.checked; });
        if (checked.length === 0) { alert("Lütfen en az bir ürün seçin."); return; }
        rowsEl.innerHTML = "";
        checked.forEach(function (c) {
          var row = document.createElement("div");
          row.className = "adp__field pli-row";
          row.setAttribute("data-product-id", c.value);
          row.style.display = "flex";
          row.style.gap = "8px";
          row.style.alignItems = "center";
          row.style.marginBottom = "8px";
          row.innerHTML =
            '<span style="min-width:260px">' + esc(c.getAttribute("data-name")) + ' <span class="mono" style="color:var(--ink-500)">(' + esc(c.getAttribute("data-code")) + ')</span>'
              + ' <span style="color:var(--ink-500)">(eski liste fiyatı: ' + esc(c.getAttribute("data-old-price")) + ' + KDV)</span></span>' +
            '<input type="number" step="0.01" min="0" class="js-pli-price" placeholder="KDV hariç fiyat" style="width:140px"/>' +
            '<select class="js-pli-currency" style="width:90px">' +
            ["TRY", "USD", "EUR"].map(function (c2) {
              var sel = c2 === c.getAttribute("data-currency") ? " selected" : "";
              return '<option value="' + c2 + '"' + sel + '>' + c2 + '</option>';
            }).join("") +
            '</select>';
          rowsEl.appendChild(row);
        });
        saveArea.style.display = "block";
      });
    }

    if (saveBtn) {
      saveBtn.addEventListener("click", function () {
        var items = [];
        Array.prototype.forEach.call(rowsEl.querySelectorAll(".pli-row"), function (row) {
          var price = parseFloat(row.querySelector(".js-pli-price").value);
          if (isNaN(price) || price < 0) return;
          items.push({
            productId: parseInt(row.getAttribute("data-product-id"), 10),
            price: price,
            currency: row.querySelector(".js-pli-currency").value
          });
        });
        if (items.length === 0) { alert("Lütfen eklenecek ürünler için geçerli fiyat girin."); return; }
        payloadInput.value = JSON.stringify({ items: items });
        form.submit();
      });
    }
  });
})();
