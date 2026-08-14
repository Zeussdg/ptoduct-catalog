import { z } from "zod";
import * as userService from "../services/userService.js";
import * as auditLogService from "../services/auditLogService.js";
import { asyncHandler } from "../utils/asyncHandler.js";
import { ApiError } from "../utils/apiError.js";

export const createUserSchema = z.object({
  email: z.string().email(),
  username: z.string().optional(),
  password: z.string().min(8),
  role: z.enum(["CUSTOMER", "ADMIN", "SUPER_ADMIN"]),
  name: z.string().optional(),
  surname: z.string().optional(),
  companyName: z.string().optional(),
  phone: z.string().optional(),
});

export const roleSchema = z.object({ role: z.enum(["CUSTOMER", "ADMIN", "SUPER_ADMIN"]) });
export const statusSchema = z.object({ status: z.enum(["ACTIVE", "INACTIVE", "SUSPENDED"]) });

// ADMIN de listeleyebilir (yalnızca CUSTOMER'lar — bkz. /api/admin/customers),
// tüm rollerin listelenmesi ve yönetimi (create/role/delete) yalnızca
// SUPER_ADMIN'e route-level'de kısıtlanmıştır.
export const list = asyncHandler(async (req, res) => {
  const { role, page } = req.query;
  const result = await userService.listUsers({ role, page: page ? Number(page) : 1 });
  res.json(result);
});

export const listCustomers = asyncHandler(async (req, res) => {
  const { page } = req.query;
  const result = await userService.listUsers({ role: "CUSTOMER", page: page ? Number(page) : 1 });
  res.json(result);
});

export const getCustomerDetail = asyncHandler(async (req, res) => {
  const user = await userService.getUserDetail(Number(req.params.id));
  res.json({ user });
});

export const create = asyncHandler(async (req, res) => {
  const user = await userService.createUser(req.body);
  await auditLogService.record({ actorId: req.user.id, action: "USER_CREATED", entityType: "User", entityId: String(user.id), metadata: { role: user.role }, ipAddress: req.ip });
  res.status(201).json({ user });
});

export const updateRole = asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (id === req.user.id) throw ApiError.badRequest("Kendi rolünüzü değiştiremezsiniz");
  const user = await userService.updateRole(id, req.body.role);
  await auditLogService.record({ actorId: req.user.id, action: "USER_ROLE_CHANGED", entityType: "User", entityId: String(id), metadata: { role: req.body.role }, ipAddress: req.ip });
  res.json({ user });
});

export const updateStatus = asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (id === req.user.id) throw ApiError.badRequest("Kendi durumunuzu değiştiremezsiniz");
  const user = await userService.updateStatus(id, req.body.status);
  await auditLogService.record({ actorId: req.user.id, action: "USER_STATUS_CHANGED", entityType: "User", entityId: String(id), metadata: { status: req.body.status }, ipAddress: req.ip });
  res.json({ user });
});

export const remove = asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  if (id === req.user.id) throw ApiError.badRequest("Kendi hesabınızı silemezsiniz");
  await userService.deleteUser(id);
  await auditLogService.record({ actorId: req.user.id, action: "USER_DELETED", entityType: "User", entityId: String(id), ipAddress: req.ip });
  res.status(204).send();
});
