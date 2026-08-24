// Ürün detay sayfası: "Sepete Ekle" butonuna basılınca, müşterinin bağlı olduğu fiyat listelerinden
// (ör. Taksitli/Peşin) veya tek bir geçerli fiyattan hangisiyle ekleneceğini soran bir modal açar.
// Radyo butonlarından biri seçilip modal içindeki "Sepete Ekle" onaylanınca CatalogCart'a (cart.js) eklenir.
(function () {
  "use strict";

  document.addEventListener("DOMContentLoaded", function () {
    var addBtn = document.getElementById("pdetailAddBtn");
    var modal = document.getElementById("pdetailModal");
    if (!addBtn || !modal) return;

    var backdrop = document.getElementById("pdetailModalBackdrop");
    var closeBtn = document.getElementById("pdetailModalClose");
    var confirmBtn = document.getElementById("pdetailModalConfirm");
    var options = modal.querySelectorAll(".pmodal__option");

    function markSelected() {
      Array.prototype.forEach.call(options, function (opt) {
        var input = opt.querySelector('input[type="radio"]');
        opt.classList.toggle("pmodal__option--selected", !!input && input.checked);
      });
    }

    Array.prototype.forEach.call(options, function (opt) {
      var input = opt.querySelector('input[type="radio"]');
      if (input) input.addEventListener("change", markSelected);
    });
    markSelected();

    function openModal() { modal.hidden = false; }
    function closeModal() { modal.hidden = true; }

    addBtn.addEventListener("click", openModal);
    if (backdrop) backdrop.addEventListener("click", closeModal);
    if (closeBtn) closeBtn.addEventListener("click", closeModal);
    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && !modal.hidden) closeModal();
    });

    if (confirmBtn) {
      confirmBtn.addEventListener("click", function () {
        var checked = modal.querySelector('input[name="pdetailPriceOption"]:checked');
        if (!checked || !window.CatalogCart) { closeModal(); return; }

        var qty = 1;
        var qtyTarget = addBtn.getAttribute("data-qty-target");
        if (qtyTarget) {
          var el = document.getElementById(qtyTarget);
          if (el) qty = parseInt(el.textContent, 10) || 1;
        }

        // Fallback ("fiyat listesi yok") radyosunda data-name yok — sadece gerçek fiyat listesi
        // seçeneklerini (data-name'i olanları) sepetteki geçiş menüsüne aktarıyoruz.
        var allOptions = Array.prototype.filter.call(
          modal.querySelectorAll('input[name="pdetailPriceOption"]'),
          function (input) { return input.hasAttribute("data-name"); }
        ).map(function (input) {
          return { name: input.getAttribute("data-name"), price: parseFloat(input.getAttribute("data-price")),
                   currency: input.getAttribute("data-currency") };
        });

        window.CatalogCart.addItem({
          id: addBtn.getAttribute("data-id"),
          name: addBtn.getAttribute("data-name"),
          brand: addBtn.getAttribute("data-brand"),
          code: addBtn.getAttribute("data-code"),
          price: parseFloat(checked.getAttribute("data-price")),
          currency: checked.getAttribute("data-currency"),
          priceIncludesVat: false,
          priceListName: checked.getAttribute("data-name"),
          priceOptions: allOptions
        }, qty);
        window.CatalogCart.open();
        closeModal();
      });
    }
  });
})();
