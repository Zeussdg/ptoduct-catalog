// Hero slaytları için sade, çizgisel ürün-grubu ikonları.
// Tümü tek renk (currentColor) çizer; renklendirme slayt degradesinden gelir.

const base = {
  viewBox: "0 0 64 64",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 2,
  strokeLinecap: "round",
  strokeLinejoin: "round",
};

const icons = {
  network: (
    <svg {...base}>
      <circle cx="32" cy="12" r="6" />
      <circle cx="12" cy="50" r="6" />
      <circle cx="32" cy="50" r="6" />
      <circle cx="52" cy="50" r="6" />
      <path d="M32 18v14M32 32l-18 12M32 32l18 12M32 32v12" />
    </svg>
  ),
  switch: (
    <svg {...base}>
      <rect x="8" y="24" width="48" height="16" rx="2" />
      <path d="M16 32h2M22 32h2M28 32h2M34 32h2M40 32h2M46 32h2" />
      <path d="M14 24v-6M50 24v-6" />
    </svg>
  ),
  router: (
    <svg {...base}>
      <rect x="10" y="34" width="44" height="14" rx="2" />
      <path d="M18 41h.5M24 41h14" />
      <path d="M22 26a10 10 0 0 1 20 0M28 26a4 4 0 0 1 8 0" />
    </svg>
  ),
  camera: (
    <svg {...base}>
      <path d="M10 26l34-10 4 12-34 10z" />
      <circle cx="27" cy="27" r="6" />
      <path d="M14 38l-3 12M44 30l8 4" />
    </svg>
  ),
  fiber: (
    <svg {...base}>
      <path d="M8 44c14 0 14-24 28-24s14 24 20 24" />
      <circle cx="52" cy="44" r="3" fill="currentColor" />
      <path d="M8 52h48" opacity="0.4" />
    </svg>
  ),
  cable: (
    <svg {...base}>
      <path d="M20 8v10a6 6 0 0 0 6 6h12a6 6 0 0 1 6 6v10" />
      <rect x="14" y="4" width="12" height="8" rx="1.5" />
      <rect x="38" y="46" width="12" height="12" rx="1.5" />
      <path d="M17 4v-0M23 4v0" />
    </svg>
  ),
  ups: (
    <svg {...base}>
      <rect x="16" y="10" width="32" height="44" rx="3" />
      <path d="M34 20l-8 12h7l-3 10 9-14h-7z" fill="currentColor" stroke="none" />
      <path d="M24 48h16" />
    </svg>
  ),
  printer: (
    <svg {...base}>
      <path d="M18 24V10h28v14" />
      <rect x="10" y="24" width="44" height="18" rx="2" />
      <rect x="18" y="40" width="28" height="12" rx="1.5" />
      <path d="M46 31h2" />
    </svg>
  ),
};

export function HeroIcon({ name }) {
  return icons[name] || icons.network;
}
