// Kurumsal site bilgileri — Footer ve iletişim alanları buradan beslenir.
// Gerçek bilgiler netleştikçe bu dosyayı güncelleyin. Footer yalnızca DOLU
// olan alanları gösterir; boş bırakılan iletişim satırları ve sosyal medya
// bağlantıları arayüzde hiç oluşturulmaz (kırık/sahte link üretilmez).

export const site = {
  brand: {
    name: "B2B",
    tagline: "Ürün Kataloğu",
    logo: "/logo.jpeg",
    description:
      "Profesyonel teknoloji ürünleri, ağ çözümleri, güvenlik sistemleri ve elektronik ürünler için güvenilir ürün kataloğu.",
  },

  // Gerçek hesap adresleri eklenene kadar boş bırakın; boş linkler gösterilmez.
  social: {
    instagram: "",
    linkedin: "",
    facebook: "",
    youtube: "",
  },

  // Gerçek iletişim bilgileri tanımlandığında doldurun; boş alanlar gizlenir.
  contact: {
    address: "",
    phone: "",
    email: "",
    hours: "",
  },
};

export default site;
