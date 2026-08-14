// Kategori kenar çubuğu mobil aç/kapa. React CategorySidebar mobileOpen karşılığı.
(function () {
  "use strict";
  document.addEventListener("DOMContentLoaded", function () {
    var toggle = document.getElementById("catsbToggle");
    var nav = document.getElementById("catsbNav");
    if (!toggle || !nav) return;
    toggle.addEventListener("click", function () {
      var open = nav.classList.toggle("catsb--open");
      toggle.setAttribute("aria-expanded", open ? "true" : "false");
      var chevron = toggle.querySelector(".catsb__mobile-chevron");
      if (chevron) chevron.classList.toggle("catsb__mobile-chevron--open", open);
    });
  });
})();
