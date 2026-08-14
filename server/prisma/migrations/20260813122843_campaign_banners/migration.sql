-- CreateTable
CREATE TABLE "campaign_banners" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "campaign_id" TEXT NOT NULL,
    "key" TEXT NOT NULL,
    "url" TEXT NOT NULL,
    "created_at" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" DATETIME NOT NULL
);

-- CreateIndex
CREATE UNIQUE INDEX "campaign_banners_campaign_id_key" ON "campaign_banners"("campaign_id");
