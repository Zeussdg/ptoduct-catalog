import rateLimit from "express-rate-limit";

// Brute-force koruması: IP başına ve IP+identifier kombinasyonuna göre sınırlar.
export const loginRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 10,
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator: (req) => `${req.ip}:${req.body?.identifier ?? ""}`,
  message: { error: { code: "TOO_MANY_REQUESTS", message: "Çok fazla deneme yapıldı, lütfen daha sonra tekrar deneyin" } },
});

export const generalApiRateLimiter = rateLimit({
  windowMs: 60 * 1000,
  limit: 120,
  standardHeaders: true,
  legacyHeaders: false,
});
