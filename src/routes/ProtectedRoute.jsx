import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

// Not: Bu guard yalnızca UX amaçlıdır (yetkisiz kullanıcıyı login'e yönlendirir).
// Gerçek yetkilendirme her zaman backend'de requireAuth/requireRole ile yapılır.
export default function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) return null;
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  return <Outlet />;
}
