// Cari Hesap sayfası: "Ödeme Al" ve "PDF Olarak Çıkart" modallarının aç/kapa mekaniği, manuel hareket
// formunda işlem türüne göre vade tarihi alanının gösterilip/gizlenmesi.
(function () {
  "use strict";

  function bindModal(openId, modalId, backdropId, closeId) {
    var openBtn = document.getElementById(openId);
    var modal = document.getElementById(modalId);
    if (!openBtn || !modal) return;
    var backdrop = document.getElementById(backdropId);
    var closeBtn = document.getElementById(closeId);

    function openModal() { modal.hidden = false; }
    function closeModal() { modal.hidden = true; }

    openBtn.addEventListener("click", openModal);
    if (backdrop) backdrop.addEventListener("click", closeModal);
    if (closeBtn) closeBtn.addEventListener("click", closeModal);
    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && !modal.hidden) closeModal();
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    bindModal("cariPaymentOpenBtn", "cariPaymentModal", "cariPaymentModalBackdrop", "cariPaymentModalClose");
    bindModal("cariPdfOpenBtn", "cariPdfModal", "cariPdfModalBackdrop", "cariPdfModalClose");

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
