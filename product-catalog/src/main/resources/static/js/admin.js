// Admin: silme onayı (data-confirm'li formlar) + mobilde sidebar aç/kapa.
(function () {
  "use strict";
  document.addEventListener("DOMContentLoaded", function () {
    Array.prototype.forEach.call(document.querySelectorAll("form[data-confirm]"), function (f) {
      f.addEventListener("submit", function (e) {
        if (!window.confirm(f.getAttribute("data-confirm"))) e.preventDefault();
      });
    });

    var toggle = document.getElementById("admToggle");
    var backdrop = document.getElementById("admBackdrop");
    var shell = document.querySelector(".adm");
    if (!toggle || !backdrop || !shell) return;

    function closeSidebar() { shell.classList.remove("adm--sidebar-open"); }
    function toggleSidebar() { shell.classList.toggle("adm--sidebar-open"); }

    toggle.addEventListener("click", toggleSidebar);
    backdrop.addEventListener("click", closeSidebar);
    Array.prototype.forEach.call(document.querySelectorAll(".adm__navlink"), function (link) {
      link.addEventListener("click", closeSidebar);
    });
  });
})();
