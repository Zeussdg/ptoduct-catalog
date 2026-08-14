import { prisma } from "../config/prismaClient.js";
import { ApiError } from "../utils/apiError.js";
import { hashPassword, comparePassword } from "../utils/bcryptHelpers.js";
import { toSafeUser } from "../utils/serializers.js";

const PROFILE_EDITABLE_FIELDS = ["name", "surname", "companyName", "phone"];

export async function getProfile(userId) {
  const user = await prisma.user.findUnique({ where: { id: userId } });
  if (!user) throw ApiError.notFound("Kullanıcı bulunamadı");
  return toSafeUser(user);
}

export async function updateProfile(userId, data) {
  const patch = {};
  for (const field of PROFILE_EDITABLE_FIELDS) {
    if (data[field] !== undefined) patch[field] = data[field];
  }
  const user = await prisma.user.update({ where: { id: userId }, data: patch });
  return toSafeUser(user);
}

export async function changePassword(userId, { currentPassword, newPassword }) {
  const user = await prisma.user.findUnique({ where: { id: userId } });
  if (!user) throw ApiError.notFound("Kullanıcı bulunamadı");

  if (user.passwordHash) {
    const valid = await comparePassword(currentPassword, user.passwordHash);
    if (!valid) throw ApiError.badRequest("Mevcut şifre hatalı");
  }

  const passwordHash = await hashPassword(newPassword);
  await prisma.user.update({ where: { id: userId }, data: { passwordHash } });
}

// --- Admin/Super Admin yönetim fonksiyonları ---

export async function listUsers({ role, page = 1, pageSize = 50 } = {}) {
  const where = role ? { role } : {};
  const skip = (page - 1) * pageSize;
  const [items, total] = await Promise.all([
    prisma.user.findMany({ where, skip, take: pageSize, orderBy: { createdAt: "desc" } }),
    prisma.user.count({ where }),
  ]);
  return { items: items.map(toSafeUser), total, page, pageSize };
}

export async function getUserDetail(id) {
  const user = await prisma.user.findUnique({
    where: { id },
    include: { quotes: { orderBy: { createdAt: "desc" }, take: 20 } },
  });
  if (!user) throw ApiError.notFound("Kullanıcı bulunamadı");
  const { passwordHash: _passwordHash, ...safe } = user;
  return safe;
}

// Yalnızca SUPER_ADMIN çağırır (route-level guard). Rol burada zorunlu
// tutulur; ADMIN/SUPER_ADMIN hesapları her zaman yerel olarak oluşturulur.
export async function createUser({ email, username, password, role, name, surname, companyName, phone }) {
  const existing = await prisma.user.findFirst({ where: { OR: [{ email }, ...(username ? [{ username }] : [])] } });
  if (existing) throw ApiError.conflict("Bu e-posta veya kullanıcı adı zaten kullanımda");

  const passwordHash = await hashPassword(password);
  const user = await prisma.user.create({
    data: { email, username, passwordHash, role, name, surname, companyName, phone },
  });
  return toSafeUser(user);
}

export async function updateRole(id, role) {
  const user = await prisma.user.findUnique({ where: { id } });
  if (!user) throw ApiError.notFound("Kullanıcı bulunamadı");
  const updated = await prisma.user.update({ where: { id }, data: { role } });
  return toSafeUser(updated);
}

export async function updateStatus(id, status) {
  const user = await prisma.user.findUnique({ where: { id } });
  if (!user) throw ApiError.notFound("Kullanıcı bulunamadı");
  const updated = await prisma.user.update({ where: { id }, data: { status } });
  return toSafeUser(updated);
}

export async function deleteUser(id) {
  const user = await prisma.user.findUnique({ where: { id } });
  if (!user) throw ApiError.notFound("Kullanıcı bulunamadı");
  await prisma.user.delete({ where: { id } });
}
