import { useState } from "react";
import CategoryItem from "./CategoryItem";
import "./CategorySidebar.css";

export default function CategorySidebar({
  categories,
  counts,
  totalCount,
  activeMain,
  activeSub,
  onSelectMain,
  onSelectSub,
  onSelectAll,
}) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const isAll = !activeMain;

  return (
    <>
      <button
        type="button"
        className="catsb__mobile-toggle"
        onClick={() => setMobileOpen((o) => !o)}
        aria-expanded={mobileOpen}
      >
        <span>{isAll ? "Tüm Kategoriler" : activeMain}</span>
        <span className={"catsb__mobile-chevron" + (mobileOpen ? " catsb__mobile-chevron--open" : "")}>
          ›
        </span>
      </button>

      <nav className={"catsb" + (mobileOpen ? " catsb--open" : "")}>
        <div className="catsb__label">Kategoriler</div>

        <button
          type="button"
          className={"catsb__all" + (isAll ? " catsb__all--active" : "")}
          onClick={() => {
            onSelectAll();
            setMobileOpen(false);
          }}
        >
          <span>Tüm Ürünler</span>
          <span className="catg__count">{totalCount}</span>
        </button>

        <div className="catsb__list" onClick={(e) => {
          if (e.target.closest(".catg__sub")) setMobileOpen(false);
        }}>
          {categories.map((c) => (
            <CategoryItem
              key={c.slug}
              category={c}
              counts={counts}
              activeMain={activeMain}
              activeSub={activeSub}
              onSelectMain={onSelectMain}
              onSelectSub={onSelectSub}
            />
          ))}
        </div>
      </nav>
    </>
  );
}
