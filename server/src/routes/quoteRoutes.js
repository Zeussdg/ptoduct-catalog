import { Router } from "express";
import * as quoteController from "../controllers/quoteController.js";
import { requireAuth, requireRole } from "../middleware/auth.js";
import { validate } from "../middleware/validate.js";

const router = Router();
router.use(requireAuth, requireRole("CUSTOMER"));

router.post("/", validate(quoteController.createQuoteSchema), quoteController.create);
router.get("/", quoteController.listMine);
router.get("/:id", quoteController.getMine);

export default router;
