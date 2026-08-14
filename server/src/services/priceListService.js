import { prisma } from "../config/prismaClient.js";
import { ApiError } from "../utils/apiError.js";

export async function listPriceLists() {
  return prisma.priceList.findMany({ orderBy: { createdAt: "desc" } });
}

export async function createPriceList(data) {
  return prisma.priceList.create({ data });
}

export async function setCustomerPrice({ userId, productId, priceListId, price, currency, validFrom, validTo }) {
  const [user, product] = await Promise.all([
    prisma.user.findUnique({ where: { id: userId } }),
    prisma.product.findUnique({ where: { id: productId } }),
  ]);
  if (!user) throw ApiError.notFound("Kullanıcı bulunamadı");
  if (!product) throw ApiError.notFound("Ürün bulunamadı");

  return prisma.customerPrice.upsert({
    where: { userId_productId: { userId, productId } },
    update: { priceListId, price, currency, validFrom, validTo },
    create: { userId, productId, priceListId, price, currency, validFrom, validTo },
  });
}
