// Admin: silme onayı (data-confirm'li formlar).
(function () {
  "use strict";
  document.addEventListener("DOMContentLoaded", function () {
    Array.prototype.forEach.call(document.querySelectorAll("form[data-confirm]"), function (f) {
      f.addEventListener("submit", function (e) {
        if (!window.confirm(f.getAttribute("data-confirm"))) e.preventDefault();
      });
    });
  });
})();
