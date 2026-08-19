-- Dashboard aggregate sorguları (aylık trend, bu ay sayımı, en çok teklif
-- edilen ürünler) için gerekli index'ler. quotes.user_id/status üzerinde
-- index zaten V1'de var, burada sadece eksik olanlar ekleniyor.
ALTER TABLE quotes ADD INDEX idx_quotes_created_at (created_at);
ALTER TABLE quote_items ADD INDEX idx_quote_items_product_code (product_code);
