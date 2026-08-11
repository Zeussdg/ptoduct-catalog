// Düz alt kategori seçimi. Değer = alt kategori slug'ı (categorySlug).
// Etiket okunabilirlik için "Ana › Alt" biçiminde; kategori sırası korunur.
export default function CategorySelect({ options, value, onChange }) {
  return (
    <select
      className="qsel"
      value={value || ""}
      onChange={(e) => onChange(e.target.value)}
      aria-label="Kategori seç"
    >
      <option value="">Kategori Seç</option>
      {options.map((opt) => (
        <option key={opt.value} value={opt.value}>
          {opt.label}
        </option>
      ))}
    </select>
  );
}
