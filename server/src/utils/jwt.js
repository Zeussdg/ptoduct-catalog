import jwt from "jsonwebtoken";
import crypto from "node:crypto";
import { env } from "../config/env.js";

const ACCESS_TOKEN_TTL = "15m";
const ACCESS_TOKEN_TTL_MS = 15 * 60 * 1000;
const REFRESH_TTL_MS_DEFAULT = 24 * 60 * 60 * 1000; // 1 gün
const REFRESH_TTL_MS_REMEMBER = 30 * 24 * 60 * 60 * 1000; // 30 gün

export function signAccessToken(user) {
  return jwt.sign({ sub: user.id, role: user.role }, env.JWT_ACCESS_SECRET, {
    expiresIn: ACCESS_TOKEN_TTL,
  });
}

export function verifyAccessToken(token) {
  return jwt.verify(token, env.JWT_ACCESS_SECRET);
}

export function generateRefreshToken() {
  const token = crypto.randomBytes(48).toString("hex");
  const tokenHash = hashRefreshToken(token);
  return { token, tokenHash };
}

export function hashRefreshToken(token) {
  return crypto.createHmac("sha256", env.JWT_REFRESH_SECRET).update(token).digest("hex");
}

export function getRefreshTokenExpiry(rememberMe) {
  const ttl = rememberMe ? REFRESH_TTL_MS_REMEMBER : REFRESH_TTL_MS_DEFAULT;
  return new Date(Date.now() + ttl);
}

export const ACCESS_COOKIE_MAX_AGE_MS = ACCESS_TOKEN_TTL_MS;
export function refreshCookieMaxAgeMs(rememberMe) {
  return rememberMe ? REFRESH_TTL_MS_REMEMBER : REFRESH_TTL_MS_DEFAULT;
}
