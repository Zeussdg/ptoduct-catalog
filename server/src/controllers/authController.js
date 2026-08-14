import { z } from "zod";
import * as authService from "../services/authService.js";
import { asyncHandler } from "../utils/asyncHandler.js";
import { setAuthCookies, clearAuthCookies, REFRESH_COOKIE_NAME } from "../utils/cookies.js";

export const loginSchema = z.object({
  identifier: z.string().min(1, "E-posta veya kullanıcı adı zorunludur"),
  password: z.string().min(1, "Şifre zorunludur"),
  rememberMe: z.boolean().optional().default(false),
});

export const login = asyncHandler(async (req, res) => {
  const { identifier, password, rememberMe } = req.body;
  const result = await authService.login({
    identifier,
    password,
    rememberMe,
    userAgent: req.get("user-agent"),
    ipAddress: req.ip,
  });

  setAuthCookies(res, result);
  res.json({ user: result.user });
});

export const refresh = asyncHandler(async (req, res) => {
  const refreshToken = req.cookies?.[REFRESH_COOKIE_NAME];
  const result = await authService.refresh({
    refreshToken,
    userAgent: req.get("user-agent"),
    ipAddress: req.ip,
  });

  setAuthCookies(res, result);
  res.json({ user: result.user });
});

export const logout = asyncHandler(async (req, res) => {
  const refreshToken = req.cookies?.[REFRESH_COOKIE_NAME];
  await authService.logout({ refreshToken });
  clearAuthCookies(res);
  res.status(204).send();
});

export const me = asyncHandler(async (req, res) => {
  const user = await authService.getCurrentUser(req.user.id);
  res.json({ user });
});
