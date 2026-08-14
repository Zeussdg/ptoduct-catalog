import { z } from "zod";
import * as quoteService from "../services/quoteService.js";
import { asyncHandler } from "../utils/asyncHandler.js";

export const createQuoteSchema = z.object({
  note: z.string().optional(),
});

export const create = asyncHandler(async (req, res) => {
  const quote = await quoteService.createFromCart(req.user.id, req.body);
  res.status(201).json({ quote });
});

export const listMine = asyncHandler(async (req, res) => {
  const quotes = await quoteService.listForUser(req.user.id);
  res.json({ quotes });
});

export const getMine = asyncHandler(async (req, res) => {
  const quote = await quoteService.getById(Number(req.params.id), { userId: req.user.id, isAdmin: false });
  res.json({ quote });
});
