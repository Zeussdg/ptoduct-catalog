import { Router } from "express";
import * as adminUserController from "../controllers/adminUserController.js";
import * as adminQuoteController from "../controllers/adminQuoteController.js";
import * as adminAuditLogController from "../controllers/adminAuditLogController.js";
import * as priceListController from "../controllers/priceListController.js";
import { requireAuth, requireRole } from "../middleware/auth.js";
import { validate } from "../middleware/validate.js";

const router = Router();
router.use(requireAuth);

const adminOrAbove = requireRole("ADMIN", "SUPER_ADMIN");
const superAdminOnly = requireRole("SUPER_ADMIN");

// --- Quotes (ADMIN, SUPER_ADMIN) ---
router.get("/quotes", adminOrAbove, adminQuoteController.list);
router.get("/quotes/:id", adminOrAbove, adminQuoteController.getById);
router.put(
  "/quotes/:id/status",
  adminOrAbove,
  validate(adminQuoteController.statusSchema),
  adminQuoteController.updateStatus
);

// --- Customers (ADMIN, SUPER_ADMIN — sadece CUSTOMER rolündeki kullanıcılar) ---
router.get("/customers", adminOrAbove, adminUserController.listCustomers);
router.get("/customers/:id", adminOrAbove, adminUserController.getCustomerDetail);

// --- Price lists / customer pricing (ADMIN, SUPER_ADMIN) ---
router.get("/price-lists", adminOrAbove, priceListController.list);
router.post("/price-lists", adminOrAbove, validate(priceListController.priceListSchema), priceListController.create);
router.post(
  "/customer-prices",
  adminOrAbove,
  validate(priceListController.customerPriceSchema),
  priceListController.setCustomerPrice
);

// --- Users / roles (SUPER_ADMIN only) ---
router.get("/users", superAdminOnly, adminUserController.list);
router.post("/users", superAdminOnly, validate(adminUserController.createUserSchema), adminUserController.create);
router.put(
  "/users/:id/role",
  superAdminOnly,
  validate(adminUserController.roleSchema),
  adminUserController.updateRole
);
router.put(
  "/users/:id/status",
  superAdminOnly,
  validate(adminUserController.statusSchema),
  adminUserController.updateStatus
);
router.delete("/users/:id", superAdminOnly, adminUserController.remove);

// --- Audit logs (SUPER_ADMIN only) ---
router.get("/audit-logs", superAdminOnly, adminAuditLogController.list);

export default router;
