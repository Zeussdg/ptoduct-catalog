// Ana sayfa kampanya slider'ındaki (CampaignSlider) sabit kampanya id'leri.
// Frontend'deki src/data/campaigns.js ile eşleşir — kampanya görseli yüklenirken
// keyfi bir campaignId ile S3'e yazılmasını engellemek için allowlist görevi görür.
// campaigns.js'e yeni bir kampanya eklendiğinde buraya da eklenmelidir.
export const CAMPAIGN_IDS = [
  "ups-guardian",
  "hik-security",
  "tenda-network",
  "fiber-infra",
  "huawei-enterprise",
];
