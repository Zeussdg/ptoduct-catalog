import { z } from "zod";
import * as priceListService from "../services/priceListService.js";
import { asyncHandler } from "../utils/asyncHandler.js";

export const priceListSchema = z.object({
  name: z.string().min(1),
  description: z.string().optional(),
  isActive: z.boolean().optional(),
});

export const customerPriceSchema = z.object({
  userId: z.number().int().positive(),
  productId: z.number().int().positive(),
  priceListId: z.number().int().positive().optional().nullable(),
  price: z.union([z.number(), z.string()]).transform((v) => String(v)),
  currency: z.enum(["TRY", "USD", "EUR"]).optional(),
  validFrom: z.coerce.date().optional(),
  validTo: z.coerce.date().optional(),
});

export const list = asyncHandler(async (req, res) => {
  const priceLists = await priceListService.listPriceLists();
  res.json({ priceLists });
});

export const create = asyncHandler(async (req, res) => {
  const priceList = await priceListService.createPriceList(req.body);
  res.status(201).json({ priceList });
});

export const setCustomerPrice = asyncHandler(async (req, res) => {
  const customerPrice = await priceListService.setCustomerPrice(req.body);
  res.status(201).json({ customerPrice });
});
