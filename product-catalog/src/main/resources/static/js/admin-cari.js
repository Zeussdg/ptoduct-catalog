// Cari Hesap sayfası: "Ödeme Al" modalının aç/kapa mekaniği ve manuel hareket formunda
// işlem türüne göre vade tarihi alanının gösterilip/gizlenmesi.
(function () {
  "use strict";

  document.addEventListener("DOMContentLoaded", function () {
    var openBtn = document.getElementById("cariPaymentOpenBtn");
    var modal = document.getElementById("cariPaymentModal");
    if (openBtn && modal) {
      var backdrop = document.getElementById("cariPaymentModalBackdrop");
      var closeBtn = document.getElementById("cariPaymentModalClose");

      function openModal() { modal.hidden = false; }
      function closeModal() { modal.hidden = true; }

      openBtn.addEventListener("click", openModal);
      if (backdrop) backdrop.addEventListener("click", closeModal);
      if (closeBtn) closeBtn.addEventListener("click", closeModal);
      document.addEventListener("keydown", function (e) {
        if (e.key === "Escape" && !modal.hidden) closeModal();
      });
    }

    var typeSelect = document.getElementById("cariTxType");
    var dueDateField = document.getElementById("cariDueDateField");
    if (typeSelect && dueDateField) {
      function toggleDueDate() {
        dueDateField.hidden = typeSelect.value !== "DEBT";
      }
      typeSelect.addEventListener("change", toggleDueDate);
      toggleDueDate();
    }
  });
})();
