import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { heroSlides } from "../data/heroSlides";
import { HeroIcon } from "./HeroIcons";
import "./HeroSlider.css";

const AUTOPLAY_MS = 5000;

// Sonsuz döngü için gerçek slaytların iki ucuna klon eklenir:
//   [klon(son), s0, s1, … sn-1, klon(ilk)]
// Başlangıç konumu 1'dir (ilk gerçek slayt). Bir klona ulaşıldığında
// geçiş kapatılıp karşı uçtaki gerçek slayta sessizce atlanır.
export default function HeroSlider() {
  const slides = heroSlides;
  const count = slides.length;
  const extended = [slides[count - 1], ...slides, slides[0]];

  const [pos, setPos] = useState(1);
  const [animate, setAnimate] = useState(true);
  const [paused, setPaused] = useState(false);
  const timer = useRef(null);

  const activeDot = (pos - 1 + count) % count;

  const goNext = useCallback(() => setPos((p) => p + 1), []);
  const goPrev = useCallback(() => setPos((p) => p - 1), []);
  const goTo = useCallback((i) => setPos(i + 1), []);

  // Otomatik geçiş — hover/focus sırasında durur.
  useEffect(() => {
    if (paused) return undefined;
    timer.current = setInterval(goNext, AUTOPLAY_MS);
    return () => clearInterval(timer.current);
  }, [paused, goNext]);

  // Klona ulaşınca geçişsiz olarak karşı uca sar.
  const handleTransitionEnd = () => {
    if (pos === extended.length - 1) {
      setAnimate(false);
      setPos(1);
    } else if (pos === 0) {
      setAnimate(false);
      setPos(count);
    }
  };

  // Geçiş kapatıldıktan bir kare sonra tekrar aç (sarma tamamlanınca).
  useEffect(() => {
    if (animate) return;
    const id = requestAnimationFrame(() => setAnimate(true));
    return () => cancelAnimationFrame(id);
  }, [animate]);

  return (
    <section
      className="hero"
      aria-roledescription="carousel"
      aria-label="Öne çıkan ürün kategorileri"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocusCapture={() => setPaused(true)}
      onBlurCapture={() => setPaused(false)}
    >
      <div className="hero__viewport">
        <div
          className="hero__track"
          style={{
            transform: `translateX(-${pos * 100}%)`,
            transition: animate ? "transform 0.5s cubic-bezier(0.4, 0, 0.2, 1)" : "none",
          }}
          onTransitionEnd={handleTransitionEnd}
        >
          {extended.map((slide, i) => (
            <div
              className="hero__slide"
              key={`${slide.slug}-${i}`}
              aria-hidden={i !== pos}
              style={{ "--accent": slide.accent, "--accent-2": slide.accent2 }}
            >
              <Link to={slide.to} className="hero__card" tabIndex={i === pos ? 0 : -1}>
                <div className="hero__card-text">
                  <span className="hero__eyebrow">{slide.eyebrow}</span>
                  <h2 className="hero__title">{slide.title}</h2>
                  <p className="hero__desc">{slide.desc}</p>
                  <span className="hero__cta">
                    Ürünleri gör
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <path d="M5 12h14M13 6l6 6-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </span>
                </div>
                <div className="hero__card-art" aria-hidden="true">
                  <HeroIcon name={slide.icon} />
                </div>
              </Link>
            </div>
          ))}
        </div>

        <button type="button" className="hero__arrow hero__arrow--prev" onClick={goPrev} aria-label="Önceki">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M15 6l-6 6 6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
        <button type="button" className="hero__arrow hero__arrow--next" onClick={goNext} aria-label="Sonraki">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M9 6l6 6-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      </div>

      <div className="hero__dots" role="tablist" aria-label="Slayt seçimi">
        {slides.map((slide, i) => (
          <button
            key={slide.slug}
            type="button"
            role="tab"
            aria-selected={i === activeDot}
            aria-label={`${slide.title} slaytı`}
            className={`hero__dot${i === activeDot ? " hero__dot--active" : ""}`}
            onClick={() => goTo(i)}
          />
        ))}
      </div>
    </section>
  );
}
