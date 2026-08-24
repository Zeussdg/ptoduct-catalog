CREATE TABLE price_list_items (
  id INT AUTO_INCREMENT PRIMARY KEY,
  price_list_id INT NOT NULL,
  product_id INT NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(10) NOT NULL DEFAULT 'TRY',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_price_list_items (price_list_id, product_id),
  KEY idx_price_list_items_product (product_id),
  CONSTRAINT fk_pli_price_list FOREIGN KEY (price_list_id) REFERENCES price_lists(id) ON DELETE CASCADE,
  CONSTRAINT fk_pli_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
