import { prisma } from "../config/prismaClient.js";
import { ApiError } from "../utils/apiError.js";

function buildTree(categories, parentId = null) {
  return categories
    .filter((c) => c.parentId === parentId)
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map((c) => ({ ...c, children: buildTree(categories, c.id) }));
}

export async function listCategories({ includeInactive = false } = {}) {
  const categories = await prisma.category.findMany({
    where: includeInactive ? {} : { isActive: true },
    orderBy: { sortOrder: "asc" },
  });
  return buildTree(categories);
}

export async function createCategory(data) {
  return prisma.category.create({ data });
}

export async function updateCategory(id, data) {
  const existing = await prisma.category.findUnique({ where: { id } });
  if (!existing) throw ApiError.notFound("Kategori bulunamadı");
  return prisma.category.update({ where: { id }, data });
}

export async function deleteCategory(id) {
  const existing = await prisma.category.findUnique({ where: { id } });
  if (!existing) throw ApiError.notFound("Kategori bulunamadı");
  await prisma.category.delete({ where: { id } });
}
