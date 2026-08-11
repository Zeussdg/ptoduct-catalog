import "./QuoteCards.css";

// Kırmızı vurgulu uyarı kartı. Metin karttan taşmaz (word-wrap).
export default function QuoteWarning() {
  return (
    <aside className="qwarn" role="note" aria-label="Uyarı">
      <h3 className="qwarn__title">
        <span aria-hidden="true">⚠</span> Uyarı
      </h3>
      <p className="qwarn__lead">Tüm bilgiler tavsiye niteliğindedir.</p>
      <p className="qwarn__text">
        Ürünlerin teknik özellikleri ve donanım uyumluluk bilgileri üretici
        verilerine göre değişiklik gösterebilir. Fiyatlar döviz bazında
        geçerlidir ve haber verilmeden değişebilir. Donanım uyumsuzluğundan
        doğabilecek hatalardan firmamız sorumlu değildir.
      </p>
    </aside>
  );
}
