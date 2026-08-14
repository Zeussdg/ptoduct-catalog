export class ApiError extends Error {
  constructor(statusCode, code, message, details) {
    super(message);
    this.statusCode = statusCode;
    this.code = code;
    this.details = details;
  }

  static badRequest(message, details) {
    return new ApiError(400, "BAD_REQUEST", message, details);
  }

  static unauthorized(message = "Kimlik doğrulama gerekli") {
    return new ApiError(401, "UNAUTHENTICATED", message);
  }

  static invalidCredentials() {
    return new ApiError(401, "INVALID_CREDENTIALS", "E-posta/kullanıcı adı veya şifre hatalı");
  }

  static forbidden(message = "Bu işlem için yetkiniz yok") {
    return new ApiError(403, "FORBIDDEN", message);
  }

  static notFound(message = "Kayıt bulunamadı") {
    return new ApiError(404, "NOT_FOUND", message);
  }

  static conflict(message) {
    return new ApiError(409, "CONFLICT", message);
  }

  static serviceUnavailable(message) {
    return new ApiError(503, "SERVICE_UNAVAILABLE", message);
  }

  static internal(message = "Beklenmeyen bir hata oluştu") {
    return new ApiError(500, "INTERNAL_ERROR", message);
  }
}
