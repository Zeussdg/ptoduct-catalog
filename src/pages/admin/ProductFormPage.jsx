import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { productsApi } from "../../services/productsApi";
import { categoriesApi } from "../../services/categoriesApi";
import "./adminPages.css";

function flattenCategories(categories, depth = 0) {
  return categories.flatMap((c) => [
    { id: c.id, label: `${"— ".repeat(depth)}${c.name}` },
    ...flattenCategories(c.children ?? [], depth + 1),
  ]);
}

const EMPTY_FORM = {
  brand: "",
  name: "",
  stockCode: "",
  categoryId: "",
  description: "",
  price: "",
  discountPrice: "",
  currency: "TRY",
  isActive: true,
};

export default function ProductFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY_FORM);
  const [categories, setCategories] = useState([]);
  const [images, setImages] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    categoriesApi.list().then(({ categories }) => setCategories(flattenCategories(categories)));
  }, []);

  useEffect(() => {
    if (!isEdit) return;
    productsApi.getById(id).then(({ product }) => {
      setForm({
        brand: product.brand ?? "",
        name: product.name,
        stockCode: product.stockCode,
        categoryId: product.categoryId ?? "",
        description: product.description ?? "",
        price: product.price,
        discountPrice: product.discountPrice ?? "",
        currency: product.currency,
        isActive: product.isActive,
      });
      setImages(product.images ?? []);
    });
  }, [id, isEdit]);

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError("");
    const payload = {
      ...form,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      discountPrice: form.discountPrice === "" ? null : form.discountPrice,
    };
    try {
      if (isEdit) {
        await productsApi.update(id, payload);
      } else {
        const { product } = await productsApi.create(payload);
        navigate(`/admin/products/${product.id}/edit`, { replace: true });
        return;
      }
      navigate("/admin/products");
    } catch (err) {
      setError(err.message || "Kaydedilemedi");
    } finally {
      setSaving(false);
    }
  }

  async function handleImageUpload(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    const { image } = await productsApi.uploadImage(id, file, images.length === 0);
    setImages((prev) => [...prev, image]);
    e.target.value = "";
  }

  async function handleSetPrimary(imageId) {
    await productsApi.updateImage(id, imageId, { isPrimary: true });
    setImages((prev) => prev.map((img) => ({ ...img, isPrimary: img.id === imageId })));
  }

  async function handleDeleteImage(imageId) {
    await productsApi.deleteImage(id, imageId);
    setImages((prev) => prev.filter((img) => img.id !== imageId));
  }

  return (
    <div>
      <h1 className="adp__title">{isEdit ? "Ürünü Düzenle" : "Yeni Ürün"}</h1>

      <form className="adp__card" onSubmit={handleSubmit}>
        <div className="adp__form-grid">
          <label className="adp__field">
            <span>Ürün Adı</span>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </label>
          <label className="adp__field">
            <span>Ürün Kodu</span>
            <input
              value={form.stockCode}
              onChange={(e) => setForm({ ...form, stockCode: e.target.value })}
              required
            />
          </label>
          <label className="adp__field">
            <span>Marka</span>
            <input value={form.brand} onChange={(e) => setForm({ ...form, brand: e.target.value })} />
          </label>
          <label className="adp__field">
            <span>Kategori</span>
            <select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
              <option value="">Kategorisiz</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.label}
                </option>
              ))}
            </select>
          </label>
          <label className="adp__field">
            <span>Fiyat</span>
            <input
              type="number"
              step="0.01"
              value={form.price}
              onChange={(e) => setForm({ ...form, price: e.target.value })}
              required
            />
          </label>
          <label className="adp__field">
            <span>İndirimli Fiyat</span>
            <input
              type="number"
              step="0.01"
              value={form.discountPrice}
              onChange={(e) => setForm({ ...form, discountPrice: e.target.value })}
            />
          </label>
          <label className="adp__field">
            <span>Para Birimi</span>
            <select value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })}>
              <option value="TRY">TRY</option>
              <option value="USD">USD</option>
              <option value="EUR">EUR</option>
            </select>
          </label>
          <label className="adp__field">
            <span>Durum</span>
            <select
              value={form.isActive ? "active" : "inactive"}
              onChange={(e) => setForm({ ...form, isActive: e.target.value === "active" })}
            >
              <option value="active">Aktif</option>
              <option value="inactive">Pasif</option>
            </select>
          </label>
        </div>
        <label className="adp__field">
          <span>Açıklama</span>
          <textarea
            rows={4}
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </label>

        {error && <p style={{ color: "var(--danger-600)" }}>{error}</p>}
        <button className="adp__btn" type="submit" disabled={saving}>
          {saving ? "Kaydediliyor..." : "Kaydet"}
        </button>
      </form>

      {isEdit && (
        <div className="adp__card">
          <h2 style={{ fontSize: 15, margin: "0 0 12px" }}>Görseller</h2>
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginBottom: 12 }}>
            {images.map((img) => (
              <div key={img.id} style={{ textAlign: "center" }}>
                <img
                  src={img.url}
                  alt=""
                  style={{ width: 96, height: 96, objectFit: "cover", borderRadius: 6, border: "1px solid var(--line-200)" }}
                />
                <div style={{ display: "flex", gap: 4, marginTop: 6, justifyContent: "center" }}>
                  <button
                    type="button"
                    className={`adp__badge ${img.isPrimary ? "" : "adp__badge--muted"}`}
                    style={{ border: "none", cursor: "pointer" }}
                    onClick={() => handleSetPrimary(img.id)}
                  >
                    {img.isPrimary ? "Ana" : "Ana Yap"}
                  </button>
                  <button
                    type="button"
                    className="adp__badge adp__badge--danger"
                    style={{ border: "none", cursor: "pointer" }}
                    onClick={() => handleDeleteImage(img.id)}
                  >
                    Sil
                  </button>
                </div>
              </div>
            ))}
          </div>
          <input type="file" accept="image/*" onChange={handleImageUpload} />
        </div>
      )}
    </div>
  );
}
