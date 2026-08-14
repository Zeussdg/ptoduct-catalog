import { verifyAccessToken } from "../utils/jwt.js";
import { ACCESS_COOKIE_NAME } from "../utils/cookies.js";
import { ApiError } from "../utils/apiError.js";

// Access token cookie'sini doğrular, req.user = { id, role } atar.
// Yetki (rol) kontrolü YAPMAZ — sadece "kim" sorusunu cevaplar.
export function requireAuth(req, res, next) {
  const token = req.cookies?.[ACCESS_COOKIE_NAME];
  if (!token) {
    return next(ApiError.unauthorized());
  }
  try {
    const payload = verifyAccessToken(token);
    req.user = { id: payload.sub, role: payload.role };
    return next();
  } catch {
    return next(new ApiError(401, "TOKEN_INVALID", "Oturum süresi doldu veya geçersiz"));
  }
}

// requireAuth'tan SONRA kullanılır. Route-level'de kompoze edilir, örn:
//   router.post('/products', requireAuth, requireRole('ADMIN','SUPER_ADMIN'), ...)
export function requireRole(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user) {
      return next(ApiError.unauthorized());
    }
    if (!allowedRoles.includes(req.user.role)) {
      return next(ApiError.forbidden());
    }
    return next();
  };
}
