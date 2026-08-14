import { prisma } from "../config/prismaClient.js";
import { ApiError } from "../utils/apiError.js";
import { CAMPAIGN_IDS } from "../constants/campaignIds.js";
import * as s3Service from "./s3Service.js";

export async function listBanners() {
  return prisma.campaignBanner.findMany();
}

export async function setBannerImage({ campaignId, buffer, mimeType, originalName }) {
  if (!CAMPAIGN_IDS.includes(campaignId)) {
    throw ApiError.notFound("Kampanya bulunamadı");
  }

  const { key, url } = await s3Service.uploadCampaignBanner({ campaignId, buffer, mimeType, originalName });

  // Kampanya zaten görsele sahipse eski S3 nesnesini temizle (tek görsel tutulur).
  const existing = await prisma.campaignBanner.findUnique({ where: { campaignId } });
  if (existing) {
    await s3Service.deleteObject(existing.key);
  }

  return prisma.campaignBanner.upsert({
    where: { campaignId },
    update: { key, url },
    create: { campaignId, key, url },
  });
}

export async function deleteBanner(campaignId) {
  const banner = await prisma.campaignBanner.findUnique({ where: { campaignId } });
  if (!banner) throw ApiError.notFound("Kampanya görseli bulunamadı");

  // S3 nesnesi önce silinir; başarısız olursa DB satırı korunur (kırık link
  // yerine "silinemedi" hatası tercih edilir — productService ile aynı yaklaşım).
  await s3Service.deleteObject(banner.key);
  await prisma.campaignBanner.delete({ where: { campaignId } });
}
