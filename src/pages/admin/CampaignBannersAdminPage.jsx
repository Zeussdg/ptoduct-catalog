import { useEffect, useState } from "react";
import { campaigns } from "../../data/campaigns";
import { campaignsApi } from "../../services/campaignsApi";
import "./adminPages.css";

export default function CampaignBannersAdminPage() {
  const [banners, setBanners] = useState({}); // campaignId -> banner
  const [busy, setBusy] = useState(""); // yükleme/silme sırasındaki campaignId
  const [error, setError] = useState("");

  useEffect(() => {
    campaignsApi
      .listBanners()
      .then(({ banners: list }) => {
        setBanners(Object.fromEntries(list.map((b) => [b.campaignId, b])));
      })
      .catch((err) => setError(err.message || "Görseller yüklenemedi"));
  }, []);

  async function handleUpload(campaignId, e) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setBusy(campaignId);
    setError("");
    try {
      const { banner } = await campaignsApi.uploadBanner(campaignId, file);
      setBanners((prev) => ({ ...prev, [campaignId]: banner }));
    } catch (err) {
      setError(err.message || "Görsel yüklenemedi");
    } finally {
      setBusy("");
    }
  }

  async function handleDelete(campaignId) {
    setBusy(campaignId);
    setError("");
    try {
      await campaignsApi.removeBanner(campaignId);
      setBanners((prev) => {
        const next = { ...prev };
        delete next[campaignId];
        return next;
      });
    } catch (err) {
      setError(err.message || "Görsel silinemedi");
    } finally {
      setBusy("");
    }
  }

  return (
    <div>
      <h1 className="adp__title">Kampanya Görselleri</h1>
      <p className="adp__subtitle">
        Ana sayfadaki kampanya slider'ında her kampanyanın arka plan görselini yönetin. Görsel yüklenmemişse
        kampanya degrade fonuyla gösterilir.
      </p>

      {error && <p style={{ color: "var(--danger-600)" }}>{error}</p>}

      <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
        {campaigns.map((c) => {
          const banner = banners[c.id];
          const isBusy = busy === c.id;
          return (
            <div
              key={c.id}
              className="adp__card"
              style={{ display: "flex", gap: 16, alignItems: "center", flexWrap: "wrap" }}
            >
              <div
                style={{
                  width: 220,
                  height: 96,
                  borderRadius: 8,
                  border: "1px solid var(--line-200)",
                  flexShrink: 0,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "var(--text-500, #64748b)",
                  fontSize: 13,
                  background: banner
                    ? `center / cover no-repeat url(${banner.url})`
                    : `linear-gradient(135deg, ${c.accent}, ${c.accent2})`,
                }}
              >
                {!banner && <span style={{ color: "#fff" }}>Görsel yok</span>}
              </div>

              <div style={{ flex: 1, minWidth: 200 }}>
                <div style={{ fontSize: 12, color: "var(--text-500, #64748b)" }}>{c.brand}</div>
                <div style={{ fontSize: 15, fontWeight: 600 }}>{c.title}</div>
                <div style={{ fontSize: 13, color: "var(--text-500, #64748b)" }}>{c.subtitle}</div>
              </div>

              <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                <label className={`adp__btn adp__btn--sm${isBusy ? " adp__btn--ghost" : ""}`} style={{ cursor: isBusy ? "default" : "pointer", margin: 0 }}>
                  {isBusy ? "İşleniyor..." : banner ? "Değiştir" : "Görsel Yükle"}
                  <input
                    type="file"
                    accept="image/*"
                    disabled={isBusy}
                    onChange={(e) => handleUpload(c.id, e)}
                    style={{ display: "none" }}
                  />
                </label>
                {banner && (
                  <button
                    type="button"
                    className="adp__btn adp__btn--sm adp__btn--danger"
                    disabled={isBusy}
                    onClick={() => handleDelete(c.id)}
                  >
                    Sil
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
