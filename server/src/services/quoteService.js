import { prisma } from "../config/prismaClient.js";
import { ApiError } from "../utils/apiError.js";
import * as cartService from "./cartService.js";

const quoteInclude = {
  items: { orderBy: { id: "asc" } },
  user: { select: { id: true, email: true, name: true, surname: true, companyName: true, phone: true } },
};

export async function createFromCart(userId, { note } = {}) {
  const cart = await cartService.getOrCreateCart(userId);
  if (cart.items.length === 0) {
    throw ApiError.badRequest("Sepetiniz boş, teklif oluşturulamaz");
  }

  const quote = await prisma.$transaction(async (tx) => {
    const created = await tx.quote.create({
      data: {
        userId,
        note,
        status: "PENDING",
        items: {
          create: cart.items.map((item) => ({
            productId: item.productId,
            productName: item.product.name,
            productCode: item.product.stockCode,
            qty: item.qty,
            unitPrice: item.unitPrice,
            totalPrice: Number(item.unitPrice) * item.qty,
            currency: item.currency,
          })),
        },
      },
      include: quoteInclude,
    });
    await tx.cartItem.deleteMany({ where: { cartId: cart.id } });
    return created;
  });

  return quote;
}

export async function listForUser(userId) {
  return prisma.quote.findMany({ where: { userId }, include: quoteInclude, orderBy: { createdAt: "desc" } });
}

export async function getById(id, { userId, isAdmin }) {
  const quote = await prisma.quote.findUnique({ where: { id }, include: quoteInclude });
  if (!quote) throw ApiError.notFound("Teklif bulunamadı");
  if (!isAdmin && quote.userId !== userId) {
    throw ApiError.forbidden();
  }
  return quote;
}

export async function listAll({ status, page = 1, pageSize = 50 } = {}) {
  const where = status ? { status } : {};
  const skip = (page - 1) * pageSize;
  const [items, total] = await Promise.all([
    prisma.quote.findMany({ where, include: quoteInclude, orderBy: { createdAt: "desc" }, skip, take: pageSize }),
    prisma.quote.count({ where }),
  ]);
  return { items, total, page, pageSize };
}

export async function updateStatus(id, { status, adminNote }) {
  const existing = await prisma.quote.findUnique({ where: { id } });
  if (!existing) throw ApiError.notFound("Teklif bulunamadı");
  return prisma.quote.update({
    where: { id },
    data: { status, ...(adminNote !== undefined ? { adminNote } : {}) },
    include: quoteInclude,
  });
}
