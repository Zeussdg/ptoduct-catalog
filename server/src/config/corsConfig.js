import { env } from "./env.js";

export const corsConfig = {
  origin: env.CLIENT_ORIGIN,
  credentials: true,
};
