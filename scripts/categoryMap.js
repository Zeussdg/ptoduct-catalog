// Kategori sınıflandırma kuralları ve slug yardımcıları.
//
// Excel'deki ham kategori metinleri güvenilir değildir (60+ farklı yazım,
// boş hücreler, serbest metin). Bu yüzden sınıflandırma; ham kategori +
// ürün adı üzerinde anahtar kelime eşlemesiyle yapılır. Kurallar sıralıdır:
// İLK eşleşen kural kazanır, bu nedenle daha spesifik kurallar üste yazılır.
//
// Yeni bir ürün tipi geldiğinde tek yapılması gereken buradaki listeye
// anahtar kelime eklemektir; import script'i değişmez.

const TR_CHAR_MAP = {
  ç: "c", Ç: "c",
  ğ: "g", Ğ: "g",
  ı: "i", I: "i", İ: "i",
  ö: "o", Ö: "o",
  ş: "s", Ş: "s",
  ü: "u", Ü: "u",
};

export function slugify(input) {
  return String(input || "")
    .replace(/[çÇğĞıIİöÖşŞüÜ]/g, (ch) => TR_CHAR_MAP[ch] || ch)
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

// Türkçe duyarlı fold: küçük harf + aksan sadeleştirme + iç boşlukları
// tekilleştirme. `trim=false` ile kenar boşlukları KORUNUR — bu, anahtar
// kelimelerdeki " ups " gibi kelime-sınırı korumalarının çalışması için
// gereklidir (aksi halde " ups " → "ups" olur ve "group" içinde eşleşir).
export function fold(input, { trim = true } = {}) {
  const out = String(input || "")
    .replace(/[çÇğĞıIİöÖşŞüÜ]/g, (ch) => TR_CHAR_MAP[ch] || ch)
    .toLowerCase()
    .replace(/\s+/g, " ");
  return trim ? out.trim() : out;
}

// Geriye dönük uyum: kenarları trim'leyen normalize (sütun başlığı eşleme,
// para birimi vb. için kullanılır).
export function normalizeText(input) {
  return fold(input, { trim: true });
}

// Ana kategori / alt kategori hiyerarşisi + eşleşme anahtar kelimeleri.
export const categoryRules = [
  {
    main: "Ağ & Network",
    subs: [
      { name: "PoE Switchler", keywords: ["poe switch", "switch poe", "poe port switch"] },
      { name: "Endüstriyel Switchler", keywords: ["endustriyel switch", "industrial switch", "din rail switch"] },
      { name: "Switchler", keywords: ["switch"] },
      { name: "Access Point", keywords: ["access point", "erisim noktasi", "wifi ap", "eap"] },
      { name: "Menzil Genişletici", keywords: ["range extender", "menzil", "repeater", "extender"] },
      { name: "Point to Point Radyolink", keywords: ["point to point", "ptp", "radyolink", "cpe", "outdoor bridge"] },
      { name: "Router & Gateway", keywords: ["router", "gateway", "load balance"] },
      { name: "Powerline Adaptörler", keywords: ["powerline", "plc adapter", "av1000", "av2000"] },
      { name: "Wireless Adaptörler", keywords: ["usb adapter", "wireless adapter", "wifi adaptor", "pico", "nano adapter", "usb wifi"] },
      { name: "SFP Modüller", keywords: ["sfp", "gbic", "transceiver"] },
      { name: "PoE Enjektörler", keywords: ["poe injector", "poe enjektor", "injector", "enjektor"] },
      { name: "Modemler", keywords: ["modem", "vdsl", "adsl", "gpon", " ont "] },
      { name: "Network Aksesuarları", keywords: ["anten", "antenna", "network"] },
    ],
  },
  {
    main: "Güvenlik Sistemleri",
    subs: [
      { name: "SpeedDome Kameralar", keywords: ["speed dome", "speeddome", "ptz"] },
      { name: "Dome Kameralar", keywords: ["dome"] },
      { name: "Bullet Kameralar", keywords: ["bullet"] },
      { name: "Kayıt Cihazları (DVR/NVR)", keywords: ["dvr", "nvr", "kayit cihaz", "recorder"] },
      { name: "Panel & Erişim Kontrol", keywords: ["access control", "erisim kontrol", "kartli gecis", "turnike", "interkom", "intercom", "panel"] },
      { name: "Alarm & Dedektör", keywords: ["alarm", "dedektor", "detector", "sensor", "pir", "siren"] },
      { name: "Ekranlar", keywords: ["monitor", "monitör", "ekran", "display", "led ekran"] },
      { name: "Güvenlik HDD", keywords: ["purple", "surveillance", "guvenlik hdd", "skyhawk", "harddisk", "hdd", "ssd"] },
      { name: "Diğer Kameralar", keywords: ["kamera", "camera", "ipc", "cctv", "fisheye"] },
      { name: "Kamera Aksesuarları", keywords: ["bracket", "aparat", "montaj", "kamera aksesuar"] },
    ],
  },
  {
    main: "Fiber Optik",
    subs: [
      { name: "Fiber Patch Cordlar", keywords: ["patch cord", "patchcord", "fiber patch cord"] },
      { name: "Fiber Patch Panel", keywords: ["fiber patch panel", "odf", "sonlandirma kutusu"] },
      { name: "SM & MM Pigtailler", keywords: ["pigtail"] },
      { name: "Fiber Adaptörler", keywords: ["fiber adaptor", "sc adapter", "lc adapter", "fc adapter", "st adapter", "kupler"] },
      { name: "Fiber Kablolar", keywords: ["fiber kablo", "fiber cable", "fiber optik kablo", "loose tube", "drop kablo"] },
      { name: "Fiber Aksesuar", keywords: ["fiber", "otdr", "makas", "cleaver"] },
    ],
  },
  {
    main: "Kablolama",
    subs: [
      { name: "Keystone & Patch Panel", keywords: ["keystone", "patch panel"] },
      { name: "Patch Kablolar", keywords: ["patch kablo", "patch cable", "patch cord cat", "utp patch"] },
      { name: "Data & Lan Kabloları", keywords: ["cat5", "cat6", "cat7", "cat 5", "cat 6", "utp kablo", "ftp kablo", "sftp", "lan kablo", "data kablo"] },
      { name: "Kablo Düzenleyiciler", keywords: ["kablo duzenle", "cable manager", "organizer", "kablo kanal", "spiral"] },
    ],
  },
  {
    main: "Kabinet Sistemleri",
    subs: [
      { name: "IP55 Outdoor Kabinler", keywords: ["ip55", "outdoor kabin", "dis ortam kabin", "duvar tipi outdoor"] },
      { name: "Raf Modülleri", keywords: ["raf modul", "sabit raf", "kizakli raf", "shelf"] },
      { name: "Fan Modülleri", keywords: ["fan modul", "fan unit", "fan"] },
      { name: "Prizler", keywords: ["pdu", "priz grup", "priz"] },
      { name: "Tekerlek & Ayak Grubu", keywords: ["tekerlek", "caster", "ayak grup", "ayak seti"] },
      { name: "Kabinetler", keywords: ["kabinet", "kabin", "rack cabinet", "server kabin"] },
      { name: "Kabin Aksesuarları", keywords: ["blank panel", "kabin aksesuar", "kor kapak"] },
    ],
  },
  {
    main: "Güç & Enerji",
    subs: [
      { name: "Online UPS", keywords: ["online ups", "online kesintisiz"] },
      { name: "Line Interactive UPS", keywords: ["line interactive", "ups", "kesintisiz guc"] },
      { name: "Aküler", keywords: ["aku", "akü", "battery", "batarya"] },
    ],
  },
  {
    main: "Yazıcı & Sarf",
    subs: [
      { name: "Çok Fonksiyonlu Lazer Yazıcılar", keywords: ["cok fonksiyon", "mfp", "multifunction", "tarayici yazici"] },
      { name: "Tanklı Yazıcılar", keywords: ["tank", "ecotank", "ink tank", "mürekkep tank"] },
      { name: "Mono Lazer Yazıcılar", keywords: ["lazer", "laser", "yazici", "printer", "toner", "kartus", "drum"] },
    ],
  },
];

// Hiçbir kurala uymayan ürünler buraya düşer; sidebar'da görünür ama
// import raporunda "sınıflandırılamayan" olarak sayılır.
export const FALLBACK = {
  main: "Diğer Ürünler",
  sub: "Sınıflandırılmamış",
};

// Bir ürünü ham kategori + ada göre sınıflandırır.
// Dönen: { mainCategory, mainCategorySlug, category, categorySlug, classified }
export function classify(name, rawCategory) {
  // Haystack kenarları boşlukla sarılır; böylece " ups " gibi kelime-sınırı
  // korumalı anahtarlar baştaki/sondaki kelimelerde de eşleşebilir.
  const haystack = " " + fold([rawCategory, name].filter(Boolean).join(" ")) + " ";

  for (const rule of categoryRules) {
    for (const sub of rule.subs) {
      // Anahtar kelime fold'unda trim YAPMA — kenar boşlukları koru.
      if (sub.keywords.some((kw) => haystack.includes(fold(kw, { trim: false })))) {
        const mainSlug = slugify(rule.main);
        return {
          mainCategory: rule.main,
          mainCategorySlug: mainSlug,
          category: sub.name,
          categorySlug: `${mainSlug}--${slugify(sub.name)}`,
          classified: true,
        };
      }
    }
  }

  const mainSlug = slugify(FALLBACK.main);
  return {
    mainCategory: FALLBACK.main,
    mainCategorySlug: mainSlug,
    category: FALLBACK.sub,
    categorySlug: `${mainSlug}--${slugify(FALLBACK.sub)}`,
    classified: false,
  };
}
