import { useState } from "react";
import { useAuth } from "../hooks/useAuth";
import { userApi } from "../services/userApi";
import { ApiClientError } from "../services/apiClient";
import "./customerPages.css";

export default function AccountPage() {
  const { user, refreshSession } = useAuth();
  const [form, setForm] = useState({
    name: user?.name ?? "",
    surname: user?.surname ?? "",
    companyName: user?.companyName ?? "",
    phone: user?.phone ?? "",
  });
  const [savingProfile, setSavingProfile] = useState(false);
  const [profileMessage, setProfileMessage] = useState("");

  const [pwForm, setPwForm] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [savingPw, setSavingPw] = useState(false);
  const [pwMessage, setPwMessage] = useState("");
  const [pwError, setPwError] = useState("");

  async function handleProfileSubmit(e) {
    e.preventDefault();
    setSavingProfile(true);
    setProfileMessage("");
    try {
      await userApi.updateMe(form);
      await refreshSession();
      setProfileMessage("Profil güncellendi");
    } catch {
      setProfileMessage("Güncelleme başarısız oldu");
    } finally {
      setSavingProfile(false);
    }
  }

  async function handlePasswordSubmit(e) {
    e.preventDefault();
    setPwError("");
    setPwMessage("");
    if (pwForm.newPassword !== pwForm.confirmPassword) {
      setPwError("Yeni şifreler eşleşmiyor");
      return;
    }
    setSavingPw(true);
    try {
      await userApi.changePassword(pwForm.currentPassword, pwForm.newPassword);
      setPwMessage("Şifre başarıyla değiştirildi");
      setPwForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
    } catch (err) {
      setPwError(err instanceof ApiClientError ? err.message : "Şifre değiştirilemedi");
    } finally {
      setSavingPw(false);
    }
  }

  return (
    <div className="custpg">
      <h1 className="custpg__title">Hesabım</h1>

      <form className="custpg__card" onSubmit={handleProfileSubmit}>
        <label className="custpg__field">
          <span>E-posta</span>
          <input type="text" value={user?.email ?? ""} disabled />
        </label>
        <label className="custpg__field">
          <span>Ad</span>
          <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </label>
        <label className="custpg__field">
          <span>Soyad</span>
          <input value={form.surname} onChange={(e) => setForm({ ...form, surname: e.target.value })} />
        </label>
        <label className="custpg__field">
          <span>Firma</span>
          <input value={form.companyName} onChange={(e) => setForm({ ...form, companyName: e.target.value })} />
        </label>
        <label className="custpg__field">
          <span>Telefon</span>
          <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        </label>
        {profileMessage && <p>{profileMessage}</p>}
        <button className="custpg__btn" type="submit" disabled={savingProfile}>
          {savingProfile ? "Kaydediliyor..." : "Profili Kaydet"}
        </button>
      </form>

      <form className="custpg__card" onSubmit={handlePasswordSubmit}>
        <h2 style={{ fontSize: 15, margin: "0 0 12px" }}>Şifre Değiştir</h2>
        <label className="custpg__field">
          <span>Mevcut Şifre</span>
          <input
            type="password"
            value={pwForm.currentPassword}
            onChange={(e) => setPwForm({ ...pwForm, currentPassword: e.target.value })}
            required
          />
        </label>
        <label className="custpg__field">
          <span>Yeni Şifre</span>
          <input
            type="password"
            value={pwForm.newPassword}
            onChange={(e) => setPwForm({ ...pwForm, newPassword: e.target.value })}
            minLength={8}
            required
          />
        </label>
        <label className="custpg__field">
          <span>Yeni Şifre (Tekrar)</span>
          <input
            type="password"
            value={pwForm.confirmPassword}
            onChange={(e) => setPwForm({ ...pwForm, confirmPassword: e.target.value })}
            minLength={8}
            required
          />
        </label>
        {pwError && <p style={{ color: "var(--danger-600)" }}>{pwError}</p>}
        {pwMessage && <p>{pwMessage}</p>}
        <button className="custpg__btn" type="submit" disabled={savingPw}>
          {savingPw ? "Kaydediliyor..." : "Şifreyi Değiştir"}
        </button>
      </form>
    </div>
  );
}
