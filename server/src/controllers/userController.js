import { z } from "zod";
import * as userService from "../services/userService.js";
import { asyncHandler } from "../utils/asyncHandler.js";

export const updateProfileSchema = z.object({
  name: z.string().optional(),
  surname: z.string().optional(),
  companyName: z.string().optional(),
  phone: z.string().optional(),
});

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1),
  newPassword: z.string().min(8, "Şifre en az 8 karakter olmalı"),
});

export const getMe = asyncHandler(async (req, res) => {
  const user = await userService.getProfile(req.user.id);
  res.json({ user });
});

export const updateMe = asyncHandler(async (req, res) => {
  const user = await userService.updateProfile(req.user.id, req.body);
  res.json({ user });
});

export const changePassword = asyncHandler(async (req, res) => {
  await userService.changePassword(req.user.id, req.body);
  res.status(204).send();
});
