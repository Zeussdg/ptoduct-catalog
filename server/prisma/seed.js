import { PrismaClient } from "@prisma/client";
import bcrypt from "bcryptjs";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CLIENT_DATA_DIR = path.resolve(__dirname, "../../src/data");

const prisma = new PrismaClient();

async function loadJson(fileName) {
  const raw = await readFile(path.join(CLIENT_DATA_DIR, fileName), "utf-8");
  return JSON.parse(raw);
}

async function seedCategories() {
  const categories = await loadJson("categories.json");
  const slugToId = new Map();

  for (const [index, main] of categories.entries()) {
    const created = await prisma.category.upsert({
      where: { slug: main.slug },
      update: { name: main.name, sortOrder: index },
      create: { name: main.name, slug: main.slug, sortOrder: index },
    });
    slugToId.set(main.slug, created.id);

    for (const [subIndex, sub] of (main.subCategories ?? []).entries()) {
      const createdSub = await prisma.category.upsert({
        where: { slug: sub.slug },
        update: { name: sub.name, parentId: created.id, sortOrder: subIndex },
        create: { name: sub.name, slug: sub.slug, parentId: created.id, sortOrder: subIndex },
      });
      slugToId.set(sub.slug, createdSub.id);
    }
  }

  console.log(`Kategoriler seed edildi (${slugToId.size} kategori).`);
  return slugToId;
}

async function seedProducts(slugToId) {
  const products = await loadJson("products.json");
  let created = 0;

  for (const p of products) {
    // Ürün leaf-kategoriye bağlanır; yoksa ana kategoriye, o da yoksa kategorisiz kalır.
    const categoryId = slugToId.get(p.categorySlug) ?? slugToId.get(p.mainCategorySlug) ?? null;

    await prisma.product.upsert({
      where: { stockCode: p.stockCode || `LEGACY-${p.id}` },
      update: {
        brand: p.brand,
        name: p.name,
        categoryId,
        description: p.description,
        price: p.price ?? 0,
        currency: p.currency ?? "TRY",
      },
      create: {
        brand: p.brand,
        name: p.name,
        stockCode: p.stockCode || `LEGACY-${p.id}`,
        categoryId,
        description: p.description,
        price: p.price ?? 0,
        currency: p.currency ?? "TRY",
      },
    });
    created += 1;
  }

  console.log(`Ürünler seed edildi (${created} ürün).`);
}

async function seedBootstrapAdmin() {
  const existing = await prisma.user.findFirst({ where: { role: "SUPER_ADMIN" } });
  if (existing) {
    console.log("Zaten bir SUPER_ADMIN mevcut, bootstrap admin atlandı.");
    return;
  }

  const email = process.env.SEED_SUPER_ADMIN_EMAIL || "superadmin@2mbilisim.local";
  const password = process.env.SEED_SUPER_ADMIN_PASSWORD || "ChangeMe123!";
  const passwordHash = await bcrypt.hash(password, 12);

  await prisma.user.create({
    data: {
      email,
      passwordHash,
      role: "SUPER_ADMIN",
      status: "ACTIVE",
      name: "Süper",
      surname: "Admin",
    },
  });

  console.log("Bootstrap SUPER_ADMIN oluşturuldu:");
  console.log(`  email:    ${email}`);
  console.log(`  password: ${password}`);
  console.log("  (İlk girişten sonra şifreyi değiştirin — bu yalnızca geliştirme amaçlıdır.)");
}

async function main() {
  const slugToId = await seedCategories();
  await seedProducts(slugToId);
  await seedBootstrapAdmin();
}

main()
  .catch((err) => {
    console.error(err);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
