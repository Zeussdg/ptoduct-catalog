ALTER TABLE users ADD COLUMN price_list_id INT NULL AFTER role;
ALTER TABLE users ADD CONSTRAINT fk_users_price_list FOREIGN KEY (price_list_id) REFERENCES price_lists(id) ON DELETE SET NULL;
CREATE INDEX idx_users_price_list ON users (price_list_id);
