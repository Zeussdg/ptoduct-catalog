import ProductCard from "./ProductCard";
import "./ProductGrid.css";

export default function ProductGrid({ products }) {
  if (products.length === 0) {
    return (
      <div className="pgrid__empty">
        <p>Aramanızla eşleşen ürün bulunamadı.</p>
        <span>Farklı bir anahtar kelime veya kategori deneyin.</span>
      </div>
    );
  }

  return (
    <div className="pgrid">
      {products.map((p) => (
        <ProductCard key={p.id} product={p} />
      ))}
    </div>
  );
}
