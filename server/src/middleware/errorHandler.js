import { ApiError } from "../utils/apiError.js";
import { isProd } from "../config/env.js";

export function notFoundHandler(req, res) {
  res.status(404).json({ error: { code: "NOT_FOUND", message: "Endpoint bulunamadı" } });
}

// eslint-disable-next-line no-unused-vars
export function errorHandler(err, req, res, next) {
  if (err instanceof ApiError) {
    return res.status(err.statusCode).json({
      error: { code: err.code, message: err.message, details: err.details },
    });
  }

  console.error(err);
  return res.status(500).json({
    error: {
      code: "INTERNAL_ERROR",
      message: isProd ? "Beklenmeyen bir hata oluştu" : err.message,
    },
  });
}
