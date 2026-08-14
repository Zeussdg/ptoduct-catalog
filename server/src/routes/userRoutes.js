import { Router } from "express";
import * as userController from "../controllers/userController.js";
import { requireAuth } from "../middleware/auth.js";
import { validate } from "../middleware/validate.js";

const router = Router();
router.use(requireAuth);

router.get("/me", userController.getMe);
router.put("/me", validate(userController.updateProfileSchema), userController.updateMe);
router.put("/me/password", validate(userController.changePasswordSchema), userController.changePassword);

export default router;
