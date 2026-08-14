import { prisma } from "../config/prismaClient.js";
import { ApiError } from "../utils/apiError.js";
import * as s3Service from "./s3Service.js";

const publicListInclude = {
  category: true,
  images: { orderBy: { sortOrder: "asc" } },
};

export async function listProducts({ q, brand, categorySlug, page = 1, pageSize = 50, includeInactive = false }) {
  const where = {
    ...(includeInactive ? {} : { isActive: true }),
    ...(brand ? { brand } : {}),
    ...(categorySlug ? { category: { slug: categorySlug } } : {}),
    ...(q
      ? {
          OR: [
            { name: { contains: q } },
            { stockCode: { contains: q } },
            { brand: { contains: q } },
          ],
        }
      : {}),
  };

  const skip = (page - 1) * pageSize;
  const [items, total] = await Promise.all([
    prisma.product.findMany({ where, include: publicListInclude, skip, take: pageSize, orderBy: { id: "asc" } }),
    prisma.product.count({ where }),
  ]);

  return { items, total, page, pageSize };
}

export async function getProductById(id) {
  const product = await prisma.product.findUnique({ where: { id }, include: publicListInclude });
  if (!product) throw ApiError.notFound("Ürün bulunamadı");
  return product;
}

export async function createProduct(data) {
  return prisma.product.create({ data, include: publicListInclude });
}

export async function updateProduct(id, data) {
  const existing = await prisma.product.findUnique({ where: { id } });
  if (!existing) throw ApiError.notFound("Ürün bulunamadı");
  return prisma.product.update({ where: { id }, data, include: publicListInclude });
}

export async function deleteProduct(id) {
  const existing = await prisma.product.findUnique({ where: { id } });
  if (!existing) throw ApiError.notFound("Ürün bulunamadı");
  try {
    await prisma.product.delete({ where: { id } });
  } catch (err) {
    if (err.code === "P2003") {
      // Sepette/tekliflerde referans var (onDelete: Restrict) — hard delete yerine pasifleştirme öner.
      throw ApiError.conflict("Bu ürün sepetlerde kullanılıyor, silmek yerine pasif yapabilirsiniz");
    }
    throw err;
  }
}

export async function addProductImage({ productId, buffer, mimeType, originalName, isPrimary }) {
  const product = await prisma.product.findUnique({ where: { id: productId } });
  if (!product) throw ApiError.notFound("Ürün bulunamadı");

  const { key, url } = await s3Service.uploadProductImage({ productId, buffer, mimeType, originalName });

  if (isPrimary) {
    await prisma.productImage.updateMany({ where: { productId }, data: { isPrimary: false } });
  }

  const maxSort = await prisma.productImage.aggregate({ where: { productId }, _max: { sortOrder: true } });

  return prisma.productImage.create({
    data: {
      productId,
      key,
      url,
      isPrimary: Boolean(isPrimary),
      sortOrder: (maxSort._max.sortOrder ?? -1) + 1,
    },
  });
}

export async function updateProductImage(productId, imageId, data) {
  const image = await prisma.productImage.findFirst({ where: { id: imageId, productId } });
  if (!image) throw ApiError.notFound("Görsel bulunamadı");

  if (data.isPrimary) {
    await prisma.productImage.updateMany({ where: { productId }, data: { isPrimary: false } });
  }

  return prisma.productImage.update({ where: { id: imageId }, data });
}

export async function deleteProductImage(productId, imageId) {
  const image = await prisma.productImage.findFirst({ where: { id: imageId, productId } });
  if (!image) throw ApiError.notFound("Görsel bulunamadı");

  // S3 nesnesi önce silinir; başarısız olursa DB satırı korunur (kırık link
  // yerine "silinemedi" hatası tercih edilir — bkz. plan §6 image upload notu).
  await s3Service.deleteObject(image.key);
  await prisma.productImage.delete({ where: { id: imageId } });
}
