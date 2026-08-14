import * as auditLogService from "../services/auditLogService.js";
import { asyncHandler } from "../utils/asyncHandler.js";

export const list = asyncHandler(async (req, res) => {
  const { page } = req.query;
  const result = await auditLogService.list({ page: page ? Number(page) : 1 });
  res.json(result);
});
