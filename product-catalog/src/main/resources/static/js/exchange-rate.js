// Header'daki "Güncel Kur" rozeti — SADECE görsel bilgi amaçlıdır, hiçbir hesaplamada kullanılmaz.
(function () {
  "use strict";

  function fmt(rate) {
    return rate.toFixed(2).replace(".", ",");
  }

  function render(data) {
    if (!data || !data.usd || !data.eur) return;
    var usdEl = document.getElementById("fxUsd");
    var eurEl = document.getElementById("fxEur");
    var wrap = document.getElementById("fxRates");
    if (!usdEl || !eurEl || !wrap) return;
    usdEl.textContent = fmt(data.usd.selling) + " ₺";
    eurEl.textContent = fmt(data.eur.selling) + " ₺";
    wrap.removeAttribute("hidden");
  }

  document.addEventListener("DOMContentLoaded", function () {
    fetch("/api/exchange-rates")
      .then(function (r) { return r.json(); })
      .then(render)
      .catch(function () { /* sessizce yok say — salt gösterim widget'ı */ });
  });
})();
