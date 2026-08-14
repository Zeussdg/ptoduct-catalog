import { useState } from "react";
import { useNavigate, useLocation, Navigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { ApiClientError } from "../services/apiClient";
import "./LoginPage.css";

export default function LoginPage() {
  const { user, loading: sessionLoading, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  if (!sessionLoading && user) {
    const redirectTo = user.role === "CUSTOMER" ? "/" : "/admin";
    return <Navigate to={location.state?.from?.pathname ?? redirectTo} replace />;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const loggedInUser = await login(identifier.trim(), password, rememberMe);
      const redirectTo = loggedInUser.role === "CUSTOMER" ? "/" : "/admin";
      navigate(location.state?.from?.pathname ?? redirectTo, { replace: true });
    } catch (err) {
      if (err instanceof ApiClientError) {
        setError(err.message);
      } else {
        setError("Giriş yapılamadı, lütfen tekrar deneyin");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="loginpg">
      <form className="loginpg__card" onSubmit={handleSubmit}>
        <h1 className="loginpg__title">Giriş Yap</h1>
        <p className="loginpg__subtitle">Hesabınıza erişmek için giriş yapın</p>

        {error && <div className="loginpg__error">{error}</div>}

        <label className="loginpg__field">
          <span>E-posta veya kullanıcı adı</span>
          <input
            type="text"
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            autoComplete="username"
            required
          />
        </label>

        <label className="loginpg__field">
          <span>Şifre</span>
          <div className="loginpg__password-wrap">
            <input
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
            <button
              type="button"
              className="loginpg__toggle-pw"
              onClick={() => setShowPassword((v) => !v)}
              aria-label={showPassword ? "Şifreyi gizle" : "Şifreyi göster"}
            >
              {showPassword ? "Gizle" : "Göster"}
            </button>
          </div>
        </label>

        <label className="loginpg__remember">
          <input type="checkbox" checked={rememberMe} onChange={(e) => setRememberMe(e.target.checked)} />
          <span>Beni hatırla</span>
        </label>

        <button type="submit" className="loginpg__submit" disabled={submitting}>
          {submitting ? "Giriş yapılıyor..." : "Giriş Yap"}
        </button>
      </form>
    </div>
  );
}
