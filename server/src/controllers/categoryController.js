import { z } from "zod";
import * as categoryService from "../services/categoryService.js";
import * as auditLogService from "../services/auditLogService.js";
import { asyncHandler } from "../utils/asyncHandler.js";

export const categorySchema = z.object({
  name: z.string().min(1),
  slug: z.string().min(1),
  parentId: z.number().int().positive().nullable().optional(),
  sortOrder: z.number().int().optional(),
  isActive: z.boolean().optional(),
});

export const categoryUpdateSchema = categorySchema.partial();

export const list = asyncHandler(async (req, res) => {
  const includeInactive = req.user && ["ADMIN", "SUPER_ADMIN"].includes(req.user.role);
  const categories = await categoryService.listCategories({ includeInactive });
  res.json({ categories });
});

export const create = asyncHandler(async (req, res) => {
  const category = await categoryService.createCategory(req.body);
  await auditLogService.record({ actorId: req.user.id, action: "CATEGORY_CREATED", entityType: "Category", entityId: String(category.id), ipAddress: req.ip });
  res.status(201).json({ category });
});

export const update = asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  const category = await categoryService.updateCategory(id, req.body);
  await auditLogService.record({ actorId: req.user.id, action: "CATEGORY_UPDATED", entityType: "Category", entityId: String(id), metadata: req.body, ipAddress: req.ip });
  res.json({ category });
});

export const remove = asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  await categoryService.deleteCategory(id);
  await auditLogService.record({ actorId: req.user.id, action: "CATEGORY_DELETED", entityType: "Category", entityId: String(id), ipAddress: req.ip });
  res.status(204).send();
});
