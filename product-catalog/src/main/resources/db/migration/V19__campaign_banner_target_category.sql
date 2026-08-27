ALTER TABLE campaign_banners ADD COLUMN target_category_id INT NULL;
ALTER TABLE campaign_banners ADD CONSTRAINT fk_campaign_banners_category
  FOREIGN KEY (target_category_id) REFERENCES categories(id) ON DELETE SET NULL;
