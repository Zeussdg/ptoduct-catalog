import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { campaigns } from "../data/campaigns";
import "./CampaignSlider.css";

const AUTOPLAY_MS = 6000;
const SWIPE_THRESHOLD = 45; // px

// Sonsuz döngü: gerçek slaytların iki ucuna klon eklenir
//   [klon(son), s0 … sn-1, klon(ilk)]
// başlangıç konumu 1, klona ulaşınca geçişsiz karşı uca sarılır.
export default function CampaignSlider() {
  const slides = campaigns;
  const count = slides.length;
  const extended = [slides[count - 1], ...slides, slides[0]];

  const [pos, setPos] = useState(1);
  const [animate, setAnimate] = useState(true);
  const [paused, setPaused] = useState(false);
  const [drag, setDrag] = useState(0); // aktif swipe sırasında px kayma
  const timer = useRef(null);
  const touch = useRef({ x: 0, active: false });

  const activeDot = (pos - 1 + count) % count;

  const goNext = useCallback(() => setPos((p) => p + 1), []);
  const goPrev = useCallback(() => setPos((p) => p - 1), []);
  const goTo = useCallback((i) => setPos(i + 1), []);

  useEffect(() => {
    if (paused) return undefined;
    timer.current = setInterval(goNext, AUTOPLAY_MS);
    return () => clearInterval(timer.current);
  }, [paused, goNext]);

  const handleTransitionEnd = () => {
    if (pos === extended.length - 1) {
      setAnimate(false);
      setPos(1);
    } else if (pos === 0) {
      setAnimate(false);
      setPos(count);
    }
  };

  useEffect(() => {
    if (animate) return undefined;
    const id = requestAnimationFrame(() => setAnimate(true));
    return () => cancelAnimationFrame(id);
  }, [animate]);

  // ---- Touch / swipe ----
  const onTouchStart = (e) => {
    touch.current = { x: e.touches[0].clientX, active: true };
    setPaused(true);
  };
  const onTouchMove = (e) => {
    if (!touch.current.active) return;
    setDrag(e.touches[0].clientX - touch.current.x);
  };
  const onTouchEnd = () => {
    const d = drag;
    touch.current.active = false;
    setDrag(0);
    setPaused(false);
    if (d <= -SWIPE_THRESHOLD) goNext();
    else if (d >= SWIPE_THRESHOLD) goPrev();
  };

  return (
    <div
      className="camp"
      aria-roledescription="carousel"
      aria-label="Kampanyalar"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocusCapture={() => setPaused(true)}
      onBlurCapture={() => setPaused(false)}
    >
      <div
        className="camp__viewport"
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
        onTouchEnd={onTouchEnd}
      >
        <div
          className="camp__track"
          style={{
            transform: `translateX(calc(-${pos * 100}% + ${drag}px))`,
            transition: animate && drag === 0 ? "transform 0.55s cubic-bezier(0.4, 0, 0.2, 1)" : "none",
          }}
          onTransitionEnd={handleTransitionEnd}
        >
          {extended.map((slide, i) => (
            <div
              className="camp__slide"
              key={`${slide.id}-${i}`}
              aria-hidden={i !== pos}
              style={{ "--accent": slide.accent, "--accent-2": slide.accent2 }}
            >
              {/* Temsili görsel paneli (tam genişlik, tıklanabilir) */}
              <Link
                to={slide.to}
                className="camp__banner"
                aria-label={`${slide.title} — ${slide.brand}`}
                tabIndex={i === pos ? 0 : -1}
                draggable="false"
                style={slide.image ? { backgroundImage: `url(${slide.image})` } : undefined}
              >
                {!slide.image && <span className="camp__glow" aria-hidden="true" />}
              </Link>
            </div>
          ))}
        </div>

        <button type="button" className="camp__arrow camp__arrow--prev" onClick={goPrev} aria-label="Önceki kampanya">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M15 6l-6 6 6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
        <button type="button" className="camp__arrow camp__arrow--next" onClick={goNext} aria-label="Sonraki kampanya">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M9 6l6 6-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      </div>

      <div className="camp__dots" role="tablist" aria-label="Kampanya seçimi">
        {slides.map((slide, i) => (
          <button
            key={slide.id}
            type="button"
            role="tab"
            aria-selected={i === activeDot}
            aria-label={`${slide.title} kampanyası`}
            className={`camp__dot${i === activeDot ? " camp__dot--active" : ""}`}
            onClick={() => goTo(i)}
          />
        ))}
      </div>
    </div>
  );
}
