import { z } from "zod";
import * as productService from "../services/productService.js";
import * as auditLogService from "../services/auditLogService.js";
import { asyncHandler } from "../utils/asyncHandler.js";

const decimalString = z.union([z.number(), z.string()]).transform((v) => String(v));

export const productSchema = z.object({
  brand: z.string().optional().nullable(),
  name: z.string().min(1),
  stockCode: z.string().min(1),
  categoryId: z.number().int().positive().nullable().optional(),
  description: z.string().optional().nullable(),
  price: decimalString,
  discountPrice: decimalString.optional().nullable(),
  currency: z.enum(["TRY", "USD", "EUR"]).optional(),
  isActive: z.boolean().optional(),
});

export const productUpdateSchema = productSchema.partial();

export const productImageUpdateSchema = z.object({
  isPrimary: z.boolean().optional(),
  sortOrder: z.number().int().optional(),
});

export const list = asyncHandler(async (req, res) => {
  const { q, brand, kategori, sayfa } = req.query;
  const includeInactive = Boolean(req.user && ["ADMIN", "SUPER_ADMIN"].includes(req.user.role));
  const result = await productService.listProducts({
    q,
    brand,
    categorySlug: kategori,
    page: sayfa ? Number(sayfa) : 1,
    includeInactive,
  });
  res.json(result);
});

export const getById = asyncHandler(async (req, res) => {
  const product = await productService.getProductById(Number(req.params.id));
  res.json({ product });
});

export const create = asyncHandler(async (req, res) => {
  const product = await productService.createProduct(req.body);
  await auditLogService.record({ actorId: req.user.id, action: "PRODUCT_CREATED", entityType: "Product", entityId: String(product.id), ipAddress: req.ip });
  res.status(201).json({ product });
});

export const update = asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  const product = await productService.updateProduct(id, req.body);
  await auditLogService.record({ actorId: req.user.id, action: "PRODUCT_UPDATED", entityType: "Product", entityId: String(id), metadata: req.body, ipAddress: req.ip });
  res.json({ product });
});

export const remove = asyncHandler(async (req, res) => {
  const id = Number(req.params.id);
  await productService.deleteProduct(id);
  await auditLogService.record({ actorId: req.user.id, action: "PRODUCT_DELETED", entityType: "Product", entityId: String(id), ipAddress: req.ip });
  res.status(204).send();
});

export const uploadImage = asyncHandler(async (req, res) => {
  const productId = Number(req.params.id);
  if (!req.file) {
    return res.status(400).json({ error: { code: "BAD_REQUEST", message: "Görsel dosyası zorunludur" } });
  }
  const image = await productService.addProductImage({
    productId,
    buffer: req.file.buffer,
    mimeType: req.file.mimetype,
    originalName: req.file.originalname,
    isPrimary: req.body.isPrimary === "true",
  });
  await auditLogService.record({ actorId: req.user.id, action: "PRODUCT_IMAGE_ADDED", entityType: "Product", entityId: String(productId), ipAddress: req.ip });
  res.status(201).json({ image });
});

export const updateImage = asyncHandler(async (req, res) => {
  const productId = Number(req.params.id);
  const imageId = Number(req.params.imageId);
  const image = await productService.updateProductImage(productId, imageId, req.body);
  res.json({ image });
});

export const deleteImage = asyncHandler(async (req, res) => {
  const productId = Number(req.params.id);
  const imageId = Number(req.params.imageId);
  await productService.deleteProductImage(productId, imageId);
  await auditLogService.record({ actorId: req.user.id, action: "PRODUCT_IMAGE_DELETED", entityType: "Product", entityId: String(productId), ipAddress: req.ip });
  res.status(204).send();
});
