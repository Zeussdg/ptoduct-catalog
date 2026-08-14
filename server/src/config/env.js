import { z } from "zod";

const envSchema = z.object({
  NODE_ENV: z.enum(["development", "production", "test"]).default("development"),
  PORT: z.coerce.number().int().positive().default(4000),
  CLIENT_ORIGIN: z.string().min(1).default("http://localhost:5173"),
  DATABASE_URL: z.string().min(1, "DATABASE_URL zorunludur"),
  JWT_ACCESS_SECRET: z.string().min(16, "JWT_ACCESS_SECRET en az 16 karakter olmalı"),
  JWT_REFRESH_SECRET: z.string().min(16, "JWT_REFRESH_SECRET en az 16 karakter olmalı"),

  S3_ENDPOINT: z.string().optional().default(""),
  S3_REGION: z.string().optional().default("auto"),
  S3_BUCKET: z.string().optional().default(""),
  S3_ACCESS_KEY_ID: z.string().optional().default(""),
  S3_SECRET_ACCESS_KEY: z.string().optional().default(""),
  S3_PUBLIC_URL_BASE: z.string().optional().default(""),
  S3_FORCE_PATH_STYLE: z
    .string()
    .optional()
    .default("true")
    .transform((v) => v === "true"),

  // External customer DB — hepsi opsiyonel. Boşsa MockExternalCustomerAdapter
  // kullanılır (bkz. services/externalCustomer). Gerçek yapı UNKNOWN (plan §6).
  EXTERNAL_DB_HOST: z.string().optional().default(""),
  EXTERNAL_DB_PORT: z.string().optional().default(""),
  EXTERNAL_DB_NAME: z.string().optional().default(""),
  EXTERNAL_DB_USER: z.string().optional().default(""),
  EXTERNAL_DB_PASSWORD: z.string().optional().default(""),
});

const parsed = envSchema.safeParse(process.env);

if (!parsed.success) {
  console.error("Ortam değişkenleri doğrulanamadı:");
  console.error(parsed.error.flatten().fieldErrors);
  process.exit(1);
}

export const env = parsed.data;
export const isProd = env.NODE_ENV === "production";
