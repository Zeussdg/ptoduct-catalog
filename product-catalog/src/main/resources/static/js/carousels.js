// Hero ve kampanya slider'ları için sade carousel (otomatik oynatma, oklar,
// noktalar, hover'da durdurma, dokunma/swipe). React HeroSlider/CampaignSlider
// davranışının vanilla karşılığı.
(function () {
  "use strict";

  function initCarousel(root) {
    var prefix = root.getAttribute("data-carousel"); // "hero" | "camp"
    var track = root.querySelector("." + prefix + "__track");
    if (!track) return;
    var slides = Array.prototype.slice.call(track.children);
    var count = slides.length;
    if (count <= 1) return;

    var autoplayMs = prefix === "camp" ? 6000 : 5000;
    var index = 0;
    var timer = null;

    // Noktalar
    var dotsWrap = root.querySelector("[data-carousel-dots]");
    var dots = [];
    if (dotsWrap) {
      for (var i = 0; i < count; i++) {
        var d = document.createElement("button");
        d.type = "button";
        d.setAttribute("role", "tab");
        d.className = prefix + "__dot";
        (function (idx) {
          d.addEventListener("click", function () { goTo(idx); });
        })(i);
        dotsWrap.appendChild(d);
        dots.push(d);
      }
    }

    function render() {
      track.style.transition = "transform 0.5s cubic-bezier(0.4,0,0.2,1)";
      track.style.transform = "translateX(-" + (index * 100) + "%)";
      for (var i = 0; i < dots.length; i++) {
        dots[i].className = prefix + "__dot" + (i === index ? " " + prefix + "__dot--active" : "");
        dots[i].setAttribute("aria-selected", i === index ? "true" : "false");
      }
    }

    function goTo(i) { index = (i + count) % count; render(); }
    function next() { goTo(index + 1); }
    function prev() { goTo(index - 1); }

    function start() { stop(); timer = setInterval(next, autoplayMs); }
    function stop() { if (timer) { clearInterval(timer); timer = null; } }

    var prevBtn = root.querySelector("[data-carousel-prev]");
    var nextBtn = root.querySelector("[data-carousel-next]");
    if (prevBtn) prevBtn.addEventListener("click", prev);
    if (nextBtn) nextBtn.addEventListener("click", next);

    root.addEventListener("mouseenter", stop);
    root.addEventListener("mouseleave", start);
    root.addEventListener("focusin", stop);
    root.addEventListener("focusout", start);

    // Dokunma / swipe
    var startX = 0, active = false;
    var vp = root.querySelector("." + prefix + "__viewport") || root;
    vp.addEventListener("touchstart", function (e) { startX = e.touches[0].clientX; active = true; stop(); }, { passive: true });
    vp.addEventListener("touchend", function (e) {
      if (!active) return;
      active = false;
      var dx = e.changedTouches[0].clientX - startX;
      if (dx <= -45) next(); else if (dx >= 45) prev();
      start();
    });

    render();
    start();
  }

  document.addEventListener("DOMContentLoaded", function () {
    Array.prototype.forEach.call(document.querySelectorAll("[data-carousel]"), initCarousel);
  });
})();
