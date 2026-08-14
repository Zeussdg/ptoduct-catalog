import { useEffect, useState } from "react";
import { categoriesApi } from "../../services/categoriesApi";
import "./adminPages.css";

function flatten(categories, depth = 0) {
  return categories.flatMap((c) => [{ ...c, depth }, ...flatten(c.children ?? [], depth + 1)]);
}

const EMPTY_FORM = { name: "", slug: "", parentId: "" };

export default function CategoriesAdminPage() {
  const [tree, setTree] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  function load() {
    categoriesApi.list().then(({ categories }) => setTree(categories));
  }

  useEffect(load, []);

  const flat = flatten(tree);

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    try {
      await categoriesApi.create({
        name: form.name,
        slug: form.slug,
        parentId: form.parentId ? Number(form.parentId) : null,
      });
      setForm(EMPTY_FORM);
      load();
    } catch (err) {
      alert(err.message || "Kategori oluşturulamadı");
    } finally {
      setSaving(false);
    }
  }

  async function toggleActive(cat) {
    await categoriesApi.update(cat.id, { isActive: !cat.isActive });
    load();
  }

  async function handleDelete(cat) {
    if (!window.confirm(`"${cat.name}" kategorisini silmek istediğinize emin misiniz?`)) return;
    try {
      await categoriesApi.remove(cat.id);
      load();
    } catch (err) {
      alert(err.message || "Silinemedi");
    }
  }

  return (
    <div>
      <h1 className="adp__title">Kategoriler</h1>

      <form className="adp__card" onSubmit={handleSubmit}>
        <div className="adp__form-grid">
          <label className="adp__field">
            <span>Kategori Adı</span>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </label>
          <label className="adp__field">
            <span>Slug</span>
            <input value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} required />
          </label>
          <label className="adp__field">
            <span>Üst Kategori</span>
            <select value={form.parentId} onChange={(e) => setForm({ ...form, parentId: e.target.value })}>
              <option value="">— (Ana kategori)</option>
              {flat.map((c) => (
                <option key={c.id} value={c.id}>
                  {"— ".repeat(c.depth)}
                  {c.name}
                </option>
              ))}
            </select>
          </label>
        </div>
        <button className="adp__btn" type="submit" disabled={saving}>
          {saving ? "Ekleniyor..." : "Kategori Ekle"}
        </button>
      </form>

      <div className="adp__card">
        <table className="adp__table">
          <thead>
            <tr>
              <th>Kategori</th>
              <th>Slug</th>
              <th>Durum</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {flat.map((c) => (
              <tr key={c.id}>
                <td>
                  {"— ".repeat(c.depth)}
                  {c.name}
                </td>
                <td className="mono">{c.slug}</td>
                <td>
                  <button
                    className={`adp__badge ${c.isActive ? "" : "adp__badge--muted"}`}
                    style={{ border: "none", cursor: "pointer" }}
                    onClick={() => toggleActive(c)}
                  >
                    {c.isActive ? "Aktif" : "Pasif"}
                  </button>
                </td>
                <td>
                  <button className="adp__btn adp__btn--sm adp__btn--danger" onClick={() => handleDelete(c)}>
                    Sil
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
