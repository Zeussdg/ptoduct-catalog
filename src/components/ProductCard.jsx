import { Link } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { formatPrice } from "../utils/format";
import "./ProductCard.css";

export default function ProductCard({ product }) {
  const { addItem } = useCart();

  return (
    <article className="pcard">
      <Link to={`/urun/${product.id}`} className="pcard__link">
        <div className="pcard__top">
          <span className="pcard__brand">{product.brand}</span>
          <span className="pcard__code mono">{product.stockCode}</span>
        </div>
        <h3 className="pcard__name">{product.name}</h3>
        <span className="pcard__category">{product.category}</span>
      </Link>

      <div className="pcard__bottom">
        <span className="pcard__price">
          {formatPrice(product.price, product.currency)}
        </span>
        <button
          type="button"
          className="pcard__add"
          onClick={() => addItem(product, 1)}
          aria-label={`${product.name} sepete ekle`}
        >
          + Sepete Ekle
        </button>
      </div>
    </article>
  );
}
