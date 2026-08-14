import { Router } from "express";
import * as authController from "../controllers/authController.js";
import { requireAuth } from "../middleware/auth.js";
import { validate } from "../middleware/validate.js";
import { loginRateLimiter } from "../middleware/rateLimiters.js";

const router = Router();

router.post("/login", loginRateLimiter, validate(authController.loginSchema), authController.login);
router.post("/refresh", authController.refresh);
router.post("/logout", authController.logout);
router.get("/me", requireAuth, authController.me);

export default router;
