import { prisma } from "../config/prismaClient.js";
import { ApiError } from "../utils/apiError.js";

const cartInclude = {
  items: { include: { product: { include: { images: true } } }, orderBy: { id: "asc" } },
};

export async function getOrCreateCart(userId) {
  let cart = await prisma.cart.findUnique({ where: { userId }, include: cartInclude });
  if (!cart) {
    cart = await prisma.cart.create({ data: { userId }, include: cartInclude });
  }
  return cart;
}

export async function addItem(userId, { productId, qty }) {
  const product = await prisma.product.findUnique({ where: { id: productId } });
  if (!product || !product.isActive) throw ApiError.notFound("Ürün bulunamadı");

  const cart = await getOrCreateCart(userId);
  const effectivePrice = product.discountPrice ?? product.price;

  const existing = await prisma.cartItem.findUnique({
    where: { cartId_productId: { cartId: cart.id, productId } },
  });

  if (existing) {
    await prisma.cartItem.update({
      where: { id: existing.id },
      data: { qty: existing.qty + qty, unitPrice: effectivePrice, currency: product.currency },
    });
  } else {
    await prisma.cartItem.create({
      data: {
        cartId: cart.id,
        productId,
        qty,
        unitPrice: effectivePrice, // snapshot — ekleme anındaki fiyat
        currency: product.currency,
      },
    });
  }

  return getOrCreateCart(userId);
}

export async function updateItemQty(userId, itemId, qty) {
  const cart = await getOrCreateCart(userId);
  const item = cart.items.find((i) => i.id === itemId);
  if (!item) throw ApiError.notFound("Sepet öğesi bulunamadı");

  await prisma.cartItem.update({ where: { id: itemId }, data: { qty } });
  return getOrCreateCart(userId);
}

export async function removeItem(userId, itemId) {
  const cart = await getOrCreateCart(userId);
  const item = cart.items.find((i) => i.id === itemId);
  if (!item) throw ApiError.notFound("Sepet öğesi bulunamadı");

  await prisma.cartItem.delete({ where: { id: itemId } });
  return getOrCreateCart(userId);
}

export async function clearCart(userId) {
  const cart = await getOrCreateCart(userId);
  await prisma.cartItem.deleteMany({ where: { cartId: cart.id } });
  return getOrCreateCart(userId);
}
