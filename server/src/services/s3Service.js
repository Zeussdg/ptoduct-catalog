import { PutObjectCommand, DeleteObjectCommand } from "@aws-sdk/client-s3";
import crypto from "node:crypto";
import { s3Client, s3Bucket } from "../config/s3Client.js";
import { env } from "../config/env.js";

function sanitizeFileName(name) {
  return name.replace(/[^a-zA-Z0-9._-]/g, "_");
}

export async function uploadProductImage({ productId, buffer, mimeType, originalName }) {
  const key = `products/${productId}/${crypto.randomUUID()}-${sanitizeFileName(originalName)}`;

  await s3Client.send(
    new PutObjectCommand({
      Bucket: s3Bucket,
      Key: key,
      Body: buffer,
      ContentType: mimeType,
    })
  );

  const url = env.S3_PUBLIC_URL_BASE ? `${env.S3_PUBLIC_URL_BASE}/${key}` : key;
  return { key, url };
}

export async function uploadCampaignBanner({ campaignId, buffer, mimeType, originalName }) {
  const key = `campaigns/${sanitizeFileName(campaignId)}/${crypto.randomUUID()}-${sanitizeFileName(originalName)}`;

  await s3Client.send(
    new PutObjectCommand({
      Bucket: s3Bucket,
      Key: key,
      Body: buffer,
      ContentType: mimeType,
    })
  );

  const url = env.S3_PUBLIC_URL_BASE ? `${env.S3_PUBLIC_URL_BASE}/${key}` : key;
  return { key, url };
}

export async function deleteObject(key) {
  await s3Client.send(new DeleteObjectCommand({ Bucket: s3Bucket, Key: key }));
}
