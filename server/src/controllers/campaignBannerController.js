import * as campaignBannerService from "../services/campaignBannerService.js";
import * as auditLogService from "../services/auditLogService.js";
import { asyncHandler } from "../utils/asyncHandler.js";

export const listBanners = asyncHandler(async (req, res) => {
  const banners = await campaignBannerService.listBanners();
  res.json({ banners });
});

export const uploadBanner = asyncHandler(async (req, res) => {
  const { campaignId } = req.params;
  if (!req.file) {
    return res.status(400).json({ error: { code: "BAD_REQUEST", message: "Görsel dosyası zorunludur" } });
  }
  const banner = await campaignBannerService.setBannerImage({
    campaignId,
    buffer: req.file.buffer,
    mimeType: req.file.mimetype,
    originalName: req.file.originalname,
  });
  await auditLogService.record({
    actorId: req.user.id,
    action: "CAMPAIGN_BANNER_SET",
    entityType: "CampaignBanner",
    entityId: campaignId,
    ipAddress: req.ip,
  });
  res.status(201).json({ banner });
});

export const deleteBanner = asyncHandler(async (req, res) => {
  const { campaignId } = req.params;
  await campaignBannerService.deleteBanner(campaignId);
  await auditLogService.record({
    actorId: req.user.id,
    action: "CAMPAIGN_BANNER_DELETED",
    entityType: "CampaignBanner",
    entityId: campaignId,
    ipAddress: req.ip,
  });
  res.status(204).send();
});
