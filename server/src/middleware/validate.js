import { ApiError } from "../utils/apiError.js";

// zod şemasını body/query/params üzerinde çalıştırır, hatalıysa 400 döner.
export function validate(schema, source = "body") {
  return (req, res, next) => {
    const result = schema.safeParse(req[source]);
    if (!result.success) {
      return next(ApiError.badRequest("Geçersiz istek verisi", result.error.flatten().fieldErrors));
    }
    req[source] = result.data;
    return next();
  };
}
