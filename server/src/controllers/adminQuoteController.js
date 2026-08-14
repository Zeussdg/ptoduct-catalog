import { z } from "zod";
import * as quoteService from "../services/quoteService.js";
import * as auditLogService from "../services/auditLogService.js";
import { asyncHandler } from "../utils/asyncHandler.js";

export const statusSchema = z.object({
  status: z.enum(["PENDING", "REVIEWING", "OFFERED", "ACCEPTED", "REJECTED", "CANCELLED"]),
  adminNote: z.string().optional(),
});

export const list = asyncHandler(async (req, res) => {
  const { status, page } = req.query;
  const result = await quoteService.listAll({ status, page: page ? Number(page) : 1 });
  res.json(result);
});

export const getById = asyncHandler(async (req, res) => {
  const quote = await quoteService.getById(Number(req.params.id), { isAdmin: true });
  res.json({ quote });
});

export const updateStatus = asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  const quote = await quoteService.updateStatus(id, req.body);
  await auditLogService.record({ actorId: req.user.id, action: "QUOTE_STATUS_CHANGED", entityType: "Quote", entityId: String(id), metadata: req.body, ipAddress: req.ip });
  res.json({ quote });
});
