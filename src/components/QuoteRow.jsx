import CategorySelect from "./CategorySelect";
import ProductCombo from "./ProductCombo";
import QuantityControl from "./QuantityControl";
import ProductThumb from "./ProductThumb";
import { formatPrice } from "../utils/format";

// Tek teklif satırı: kategori + ürün + adet + birim fiyat + tutar + sil.
export default function QuoteRow({
  row,
  categoryOptions,
  products,
  product,
  onCategoryChange,
  onProductChange,
  onQtyChange,
  onRemove,
}) {
  const hasProduct = !!product;
  const lineTotal =
    hasProduct && product.price != null ? product.price * row.qty : null;

  return (
    <div className="qrow">
      <div className="qrow__cat" data-label="Kategori">
        <CategorySelect
          options={categoryOptions}
          value={row.categorySlug}
          onChange={(slug) => onCategoryChange(row.uid, slug)}
        />
      </div>

      <div className="qrow__thumb" data-label="Görsel">
        <ProductThumb product={product} />
      </div>

      <div className="qrow__product" data-label="Ürün">
        <ProductCombo
          options={products}
          value={row.productId}
          onChange={(id) => onProductChange(row.uid, id)}
          disabled={!row.categorySlug}
        />
      </div>

      <div className="qrow__qty" data-label="Adet">
        <QuantityControl
          value={row.qty}
          onChange={(q) => onQtyChange(row.uid, q)}
          disabled={!hasProduct}
        />
      </div>

      <div className="qrow__price mono" data-label="B.Fiyat">
        {hasProduct && product.price != null
          ? formatPrice(product.price, product.currency)
          : "—"}
      </div>

      <div className="qrow__total mono" data-label="Tutar">
        {lineTotal != null ? formatPrice(lineTotal, product.currency) : "—"}
      </div>

      <div className="qrow__remove">
        <button type="button" onClick={() => onRemove(row.uid)} aria-label="Satırı sil">
          ×
        </button>
      </div>
    </div>
  );
}
