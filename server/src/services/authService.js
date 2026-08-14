import { prisma } from "../config/prismaClient.js";
import { comparePassword } from "../utils/bcryptHelpers.js";
import {
  signAccessToken,
  generateRefreshToken,
  hashRefreshToken,
  getRefreshTokenExpiry,
} from "../utils/jwt.js";
import { ApiError } from "../utils/apiError.js";
import { toSafeUser } from "../utils/serializers.js";

// Bu servis YALNIZCA Product Catalog'un kendi `users` tablosuna karşı çalışır.
// Harici müşteri veritabanına bağımlı değildir (bkz. plan §5, §6). Kullanıcı
// bulunamazsa jenerik "hatalı giriş" döner — mevcut olmayan bir hesabın var
// olup olmadığı sızdırılmaz (user enumeration koruması).
export async function login({ identifier, password, rememberMe, userAgent, ipAddress }) {
  const user = await prisma.user.findFirst({
    where: { OR: [{ email: identifier }, { username: identifier }] },
  });

  if (!user) {
    throw ApiError.invalidCredentials();
  }
  if (user.status !== "ACTIVE") {
    throw new ApiError(403, "ACCOUNT_DISABLED", "Hesabınız aktif değil");
  }
  if (!user.passwordHash) {
    // Harici kaynaktan onboard edilmiş ama henüz yerel şifresi belirlenmemiş
    // kullanıcı — gerçek authentication entegrasyon modeli discovery sonrası
    // netleşecek (bkz. plan §6.5). Şimdilik bu duruma özel bir hata kodu döner.
    throw new ApiError(401, "PASSWORD_NOT_SET", "Şifre henüz belirlenmemiş");
  }

  const passwordValid = await comparePassword(password, user.passwordHash);
  if (!passwordValid) {
    throw ApiError.invalidCredentials();
  }

  const { accessToken, refreshToken } = await issueTokens(user, { rememberMe, userAgent, ipAddress });

  await prisma.user.update({ where: { id: user.id }, data: { lastLoginAt: new Date() } });

  return { user: toSafeUser(user), accessToken, refreshToken, rememberMe };
}

async function issueTokens(user, { rememberMe, userAgent, ipAddress }) {
  const accessToken = signAccessToken(user);
  const { token: refreshToken, tokenHash } = generateRefreshToken();

  await prisma.session.create({
    data: {
      userId: user.id,
      refreshToken: tokenHash,
      rememberMe: Boolean(rememberMe),
      userAgent,
      ipAddress,
      expiresAt: getRefreshTokenExpiry(rememberMe),
    },
  });

  return { accessToken, refreshToken };
}

export async function refresh({ refreshToken, userAgent, ipAddress }) {
  if (!refreshToken) {
    throw ApiError.unauthorized();
  }
  const tokenHash = hashRefreshToken(refreshToken);
  const session = await prisma.session.findUnique({
    where: { refreshToken: tokenHash },
    include: { user: true },
  });

  if (!session || session.revokedAt || session.expiresAt < new Date()) {
    throw ApiError.unauthorized("Oturum geçersiz, lütfen tekrar giriş yapın");
  }
  if (session.user.status !== "ACTIVE") {
    throw new ApiError(403, "ACCOUNT_DISABLED", "Hesabınız aktif değil");
  }

  // Rotasyon: eski session'ı iptal edip yenisini oluştur.
  await prisma.session.update({ where: { id: session.id }, data: { revokedAt: new Date() } });

  const { accessToken, refreshToken: newRefreshToken } = await issueTokens(session.user, {
    rememberMe: session.rememberMe,
    userAgent,
    ipAddress,
  });

  return { user: toSafeUser(session.user), accessToken, refreshToken: newRefreshToken, rememberMe: session.rememberMe };
}

export async function logout({ refreshToken }) {
  if (!refreshToken) return;
  const tokenHash = hashRefreshToken(refreshToken);
  await prisma.session.updateMany({
    where: { refreshToken: tokenHash, revokedAt: null },
    data: { revokedAt: new Date() },
  });
}

export async function getCurrentUser(userId) {
  const user = await prisma.user.findUnique({ where: { id: userId } });
  if (!user) {
    throw ApiError.unauthorized();
  }
  return toSafeUser(user);
}
