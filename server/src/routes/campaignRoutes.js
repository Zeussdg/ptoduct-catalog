import { Router } from "express";
import multer from "multer";
import * as campaignBannerController from "../controllers/campaignBannerController.js";
import { requireAuth, requireRole } from "../middleware/auth.js";

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    if (!file.mimetype.startsWith("image/")) {
      return cb(new Error("Yalnızca görsel dosyaları yüklenebilir"));
    }
    cb(null, true);
  },
});

const router = Router();
const adminOnly = [requireAuth, requireRole("ADMIN", "SUPER_ADMIN")];

// Slider herkese açık olduğu için banner listesi de public.
router.get("/banners", campaignBannerController.listBanners);
router.post("/banners/:campaignId/image", ...adminOnly, upload.single("image"), campaignBannerController.uploadBanner);
router.delete("/banners/:campaignId", ...adminOnly, campaignBannerController.deleteBanner);

export default router;
