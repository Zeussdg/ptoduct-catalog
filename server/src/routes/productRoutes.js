import { Router } from "express";
import multer from "multer";
import * as productController from "../controllers/productController.js";
import { requireAuth, requireRole } from "../middleware/auth.js";
import { validate } from "../middleware/validate.js";

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

router.get("/", productController.list);
router.get("/:id", productController.getById);
router.post("/", ...adminOnly, validate(productController.productSchema), productController.create);
router.put("/:id", ...adminOnly, validate(productController.productUpdateSchema), productController.update);
router.delete("/:id", ...adminOnly, productController.remove);

router.post("/:id/images", ...adminOnly, upload.single("image"), productController.uploadImage);
router.put(
  "/:id/images/:imageId",
  ...adminOnly,
  validate(productController.productImageUpdateSchema),
  productController.updateImage
);
router.delete("/:id/images/:imageId", ...adminOnly, productController.deleteImage);

export default router;
