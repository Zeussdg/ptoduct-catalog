import { z } from "zod";
import * as cartService from "../services/cartService.js";
import { asyncHandler } from "../utils/asyncHandler.js";

export const addItemSchema = z.object({
  productId: z.number().int().positive(),
  qty: z.number().int().positive().default(1),
});

export const updateItemSchema = z.object({
  qty: z.number().int().positive(),
});

export const getCart = asyncHandler(async (req, res) => {
  const cart = await cartService.getOrCreateCart(req.user.id);
  res.json({ cart });
});

export const addItem = asyncHandler(async (req, res) => {
  const cart = await cartService.addItem(req.user.id, req.body);
  res.status(201).json({ cart });
});

export const updateItem = asyncHandler(async (req, res) => {
  const cart = await cartService.updateItemQty(req.user.id, Number(req.params.id), req.body.qty);
  res.json({ cart });
});

export const removeItem = asyncHandler(async (req, res) => {
  const cart = await cartService.removeItem(req.user.id, Number(req.params.id));
  res.json({ cart });
});

export const clearCart = asyncHandler(async (req, res) => {
  const cart = await cartService.clearCart(req.user.id);
  res.json({ cart });
});
