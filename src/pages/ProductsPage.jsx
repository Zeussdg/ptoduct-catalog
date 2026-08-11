import { useEffect, useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import CategorySidebar from "../components/CategorySidebar";
import ProductToolbar from "../components/ProductToolbar";
import ProductGrid from "../components/ProductGrid";
import HeroSlider from "../components/HeroSlider";
import CampaignSlider from "../components/CampaignSlider";
import Pagination from "../components/Pagination";
import { products } from "../data/products";
import { categories } from "../data/categories";
import "./ProductsPage.css";

const TR_LOWER = (s) => (s || "").toLocaleLowerCase("tr-TR");
const PAGE_SIZE = 20;

export default function ProductsPage() {
  const [params, setParams] = useSearchParams();

  const activeMain = params.get("kategori") || "";
  const activeSub = params.get("altkategori") || "";
  const query = params.get("q") || "";
  const brand = params.get("marka") || "";
  const sort = params.get("sirala") || "relevance";
  const page = Math.max(1, parseInt(params.get("sayfa"), 10) || 1);

  const update = (patch) => {
    const next = new URLSearchParams(params);
    Object.entries(patch).forEach(([k, v]) => {
      if (v) next.set(k, v);
      else next.delete(k);
    });
    // Filtre / arama / sıralama değişince ilk sayfaya dön.
    next.delete("sayfa");
    setParams(next, { replace: true });
  };

  const goToPage = (p) => {
    const next = new URLSearchParams(params);
    if (p <= 1) next.delete("sayfa");
    else next.set("sayfa", String(p));
    setParams(next, { replace: false });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const brands = useMemo(
    () => Array.from(new Set(products.map((p) => p.brand))).sort(),
    []
  );

  // Search + brand filtered set, independent of category selection —
  // drives the counts shown in the sidebar so users see what's available
  // for their current search before narrowing by category.
  const searchFiltered = useMemo(() => {
    const q = TR_LOWER(query.trim());
    return products.filter((p) => {
      if (brand && p.brand !== brand) return false;
      if (!q) return true;
      return (
        TR_LOWER(p.name).includes(q) ||
        TR_LOWER(p.brand).includes(q) ||
        TR_LOWER(p.stockCode).includes(q) ||
        TR_LOWER(p.category).includes(q) ||
        TR_LOWER(p.mainCategory).includes(q)
      );
    });
  }, [query, brand]);

  const counts = useMemo(() => {
    const c = {};
    for (const p of searchFiltered) {
      c[p.categorySlug] = (c[p.categorySlug] || 0) + 1;
    }
    return c;
  }, [searchFiltered]);

  const totalCount = searchFiltered.length;

  const categoryFiltered = useMemo(() => {
    return searchFiltered.filter((p) => {
      if (activeSub) return p.categorySlug === activeSub;
      if (activeMain) return p.mainCategorySlug === activeMain;
      return true;
    });
  }, [searchFiltered, activeMain, activeSub]);

  const sorted = useMemo(() => {
    const arr = [...categoryFiltered];
    switch (sort) {
      case "price-asc":
        return arr.sort((a, b) => (a.price ?? Infinity) - (b.price ?? Infinity));
      case "price-desc":
        return arr.sort((a, b) => (b.price ?? -Infinity) - (a.price ?? -Infinity));
      case "name-asc":
        return arr.sort((a, b) => a.name.localeCompare(b.name, "tr"));
      default:
        return arr;
    }
  }, [categoryFiltered, sort]);

  // ---- Sayfalama (sayfa başına 50 ürün) ----
  const totalPages = Math.max(1, Math.ceil(sorted.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);

  // URL'deki sayfa, sonuç sayısına göre aralık dışına düşerse düzelt
  // (örn. 5. sayfadayken arama sonuçları 1 sayfaya inince).
  useEffect(() => {
    if (page > totalPages) {
      const next = new URLSearchParams(params);
      next.delete("sayfa");
      setParams(next, { replace: true });
    }
  }, [page, totalPages, params, setParams]);

  const paged = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return sorted.slice(start, start + PAGE_SIZE);
  }, [sorted, currentPage]);

  const rangeStart = sorted.length === 0 ? 0 : (currentPage - 1) * PAGE_SIZE + 1;
  const rangeEnd = Math.min(currentPage * PAGE_SIZE, sorted.length);

  const activeCategoryLabel = useMemo(() => {
    if (activeSub) {
      for (const c of categories) {
        const s = c.subCategories.find((s) => s.slug === activeSub);
        if (s) return s.name;
      }
    }
    if (activeMain) {
      const c = categories.find((c) => c.slug === activeMain);
      if (c) return c.name;
    }
    return "Tüm Ürünler";
  }, [activeMain, activeSub]);

  // Hero yalnızca "ana sayfa" görünümünde (filtre/arama yokken) gösterilir;
  // kullanıcı bir kategori seçtiğinde veya arama yaptığında gizlenir.
  const showHero = !activeMain && !activeSub && !query && !brand;

  return (
    <div className="ppage">
      {showHero && <HeroSlider />}
      <div className="ppage__inner">
        <CategorySidebar
          categories={categories}
          counts={counts}
          totalCount={totalCount}
          activeMain={activeMain}
          activeSub={activeSub}
          onSelectAll={() => update({ kategori: "", altkategori: "" })}
          onSelectMain={(slug) =>
            update({ kategori: slug === activeMain ? "" : slug, altkategori: "" })
          }
          onSelectSub={(mainSlug, subSlug) =>
            update({ kategori: mainSlug, altkategori: subSlug })
          }
        />

        <main className="ppage__main">
          {showHero && (
            <div className="ppage__campaigns">
              <CampaignSlider />
            </div>
          )}

          <ProductToolbar
            title={activeCategoryLabel}
            count={sorted.length}
            query={query}
            onQueryChange={(v) => update({ q: v })}
            brands={brands}
            brand={brand}
            onBrandChange={(v) => update({ marka: v })}
            sort={sort}
            onSortChange={(v) => update({ sirala: v })}
          />

          {sorted.length > PAGE_SIZE && (
            <p className="ppage__range">
              {sorted.length} ürün içinden{" "}
              <strong>
                {rangeStart}–{rangeEnd}
              </strong>{" "}
              arası gösteriliyor · Sayfa {currentPage}/{totalPages}
            </p>
          )}

          <ProductGrid products={paged} />

          <Pagination current={currentPage} total={totalPages} onChange={goToPage} />
        </main>
      </div>
    </div>
  );
}
