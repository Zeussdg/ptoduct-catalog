import { Router } from "express";
import * as cartController from "../controllers/cartController.js";
import { requireAuth, requireRole } from "../middleware/auth.js";
import { validate } from "../middleware/validate.js";

const router = Router();
router.use(requireAuth, requireRole("CUSTOMER"));

router.get("/", cartController.getCart);
router.post("/items", validate(cartController.addItemSchema), cartController.addItem);
router.put("/items/:id", validate(cartController.updateItemSchema), cartController.updateItem);
router.delete("/items/:id", cartController.removeItem);
router.delete("/", cartController.clearCart);

export default router;
