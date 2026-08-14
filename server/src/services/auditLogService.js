import { prisma } from "../config/prismaClient.js";

export async function record({ actorId, action, entityType, entityId, metadata, ipAddress }) {
  await prisma.auditLog.create({
    data: { actorId, action, entityType, entityId, metadata, ipAddress },
  });
}

export async function list({ page = 1, pageSize = 50 } = {}) {
  const skip = (page - 1) * pageSize;
  const [logs, total] = await Promise.all([
    prisma.auditLog.findMany({
      orderBy: { createdAt: "desc" },
      skip,
      take: pageSize,
      include: { actor: { select: { id: true, email: true, name: true, surname: true, role: true } } },
    }),
    prisma.auditLog.count(),
  ]);
  return { logs, total, page, pageSize };
}
