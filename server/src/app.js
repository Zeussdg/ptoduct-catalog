import express from "express";
import helmet from "helmet";
import cors from "cors";
import cookieParser from "cookie-parser";
import { corsConfig } from "./config/corsConfig.js";
import { generalApiRateLimiter } from "./middleware/rateLimiters.js";
import { notFoundHandler, errorHandler } from "./middleware/errorHandler.js";

import authRoutes from "./routes/authRoutes.js";
import productRoutes from "./routes/productRoutes.js";
import categoryRoutes from "./routes/categoryRoutes.js";
import cartRoutes from "./routes/cartRoutes.js";
import quoteRoutes from "./routes/quoteRoutes.js";
import userRoutes from "./routes/userRoutes.js";
import adminRoutes from "./routes/adminRoutes.js";
import campaignRoutes from "./routes/campaignRoutes.js";

export const app = express();

app.use(helmet());
app.use(cors(corsConfig));
app.use(express.json());
app.use(cookieParser());
app.use(generalApiRateLimiter);

app.get("/api/health", (req, res) => res.json({ status: "ok" }));

app.use("/api/auth", authRoutes);
app.use("/api/products", productRoutes);
app.use("/api/categories", categoryRoutes);
app.use("/api/cart", cartRoutes);
app.use("/api/quotes", quoteRoutes);
app.use("/api/users", userRoutes);
app.use("/api/admin", adminRoutes);
app.use("/api/campaigns", campaignRoutes);

app.use(notFoundHandler);
app.use(errorHandler);
