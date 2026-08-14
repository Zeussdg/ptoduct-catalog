import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

// Not: Bu guard yalnızca UX amaçlıdır. Gerçek yetkilendirme backend'de
// requireAuth/requireRole ile yapılır — buradaki kontrol atlatılabilir
// olsa bile API her zaman 401/403 döner.
export default function RoleBasedRoute({ roles, children }) {
  const { user, isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) return null;
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  if (!roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }
  return children ?? <Outlet />;
}
