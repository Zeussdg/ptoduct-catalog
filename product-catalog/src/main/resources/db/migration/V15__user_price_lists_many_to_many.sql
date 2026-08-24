CREATE TABLE user_price_lists (
  user_id INT NOT NULL,
  price_list_id INT NOT NULL,
  PRIMARY KEY (user_id, price_list_id),
  CONSTRAINT fk_upl_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_upl_price_list FOREIGN KEY (price_list_id) REFERENCES price_lists(id) ON DELETE CASCADE
);

INSERT INTO user_price_lists (user_id, price_list_id)
SELECT id, price_list_id FROM users WHERE price_list_id IS NOT NULL;

ALTER TABLE users DROP FOREIGN KEY fk_users_price_list;
ALTER TABLE users DROP INDEX idx_users_price_list;
ALTER TABLE users DROP COLUMN price_list_id;
