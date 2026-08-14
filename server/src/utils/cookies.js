import { isProd } from "../config/env.js";
import { ACCESS_COOKIE_MAX_AGE_MS, refreshCookieMaxAgeMs } from "./jwt.js";

export const ACCESS_COOKIE_NAME = "access_token";
export const REFRESH_COOKIE_NAME = "refresh_token";

const baseCookieOptions = {
  httpOnly: true,
  secure: isProd,
  sameSite: "lax",
};

export function setAuthCookies(res, { accessToken, refreshToken, rememberMe }) {
  res.cookie(ACCESS_COOKIE_NAME, accessToken, {
    ...baseCookieOptions,
    path: "/",
    maxAge: ACCESS_COOKIE_MAX_AGE_MS,
  });
  res.cookie(REFRESH_COOKIE_NAME, refreshToken, {
    ...baseCookieOptions,
    path: "/api/auth",
    maxAge: refreshCookieMaxAgeMs(rememberMe),
  });
}

export function clearAuthCookies(res) {
  res.clearCookie(ACCESS_COOKIE_NAME, { ...baseCookieOptions, path: "/" });
  res.clearCookie(REFRESH_COOKIE_NAME, { ...baseCookieOptions, path: "/api/auth" });
}
