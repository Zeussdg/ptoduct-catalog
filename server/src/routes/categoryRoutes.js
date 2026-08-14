import { Router } from "express";
import * as categoryController from "../controllers/categoryController.js";
import { requireAuth, requireRole } from "../middleware/auth.js";
import { validate } from "../middleware/validate.js";

const router = Router();

router.get("/", categoryController.list);
router.post(
  "/",
  requireAuth,
  requireRole("ADMIN", "SUPER_ADMIN"),
  validate(categoryController.categorySchema),
  categoryController.create
);
router.put(
  "/:id",
  requireAuth,
  requireRole("ADMIN", "SUPER_ADMIN"),
  validate(categoryController.categoryUpdateSchema),
  categoryController.update
);
router.delete("/:id", requireAuth, requireRole("ADMIN", "SUPER_ADMIN"), categoryController.remove);

export default router;
