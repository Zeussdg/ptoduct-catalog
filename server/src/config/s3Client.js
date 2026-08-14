import { S3Client } from "@aws-sdk/client-s3";
import { env } from "./env.js";

// Genel S3-uyumlu istemci: AWS S3, MinIO, Cloudflare R2, Backblaze B2,
// DigitalOcean Spaces vb. ile S3_ENDPOINT/S3_FORCE_PATH_STYLE üzerinden çalışır.
export const s3Client = new S3Client({
  region: env.S3_REGION || "auto",
  endpoint: env.S3_ENDPOINT || undefined,
  forcePathStyle: env.S3_FORCE_PATH_STYLE,
  credentials:
    env.S3_ACCESS_KEY_ID && env.S3_SECRET_ACCESS_KEY
      ? {
          accessKeyId: env.S3_ACCESS_KEY_ID,
          secretAccessKey: env.S3_SECRET_ACCESS_KEY,
        }
      : undefined,
});

export const s3Bucket = env.S3_BUCKET;
