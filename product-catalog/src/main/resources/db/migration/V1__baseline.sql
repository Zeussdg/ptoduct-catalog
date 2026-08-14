-- Baseline şema (MySQL). Prisma schema.prisma'dan birebir çevrilmiştir.
-- Enum kolonları @Enumerated(STRING) ile eşleştiği için VARCHAR olarak tutulur.

CREATE TABLE users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  email         VARCHAR(191) NOT NULL,
  username      VARCHAR(191),
  password_hash VARCHAR(255),
  role          VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
  status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  company_name  VARCHAR(255),
  name          VARCHAR(255),
  surname       VARCHAR(255),
  phone         VARCHAR(64),
  last_login_at DATETIME(6),
  created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_users_email (email),
  UNIQUE KEY uq_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE external_identities (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT NOT NULL,
  source      VARCHAR(255) NOT NULL,
  external_id VARCHAR(191) NOT NULL,
  metadata    TEXT,
  created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_ext_source_extid (source, external_id),
  KEY idx_ext_user (user_id),
  CONSTRAINT fk_ext_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sessions (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  user_id       INT NOT NULL,
  refresh_token VARCHAR(255) NOT NULL,
  user_agent    VARCHAR(512),
  ip_address    VARCHAR(64),
  remember_me   BOOLEAN NOT NULL DEFAULT FALSE,
  expires_at    DATETIME(6) NOT NULL,
  revoked_at    DATETIME(6),
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sessions_refresh (refresh_token),
  KEY idx_sessions_user (user_id),
  CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categories (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(255) NOT NULL,
  slug       VARCHAR(191) NOT NULL,
  parent_id  INT,
  sort_order INT NOT NULL DEFAULT 0,
  is_active  BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_categories_slug (slug),
  KEY idx_categories_parent (parent_id),
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE products (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  brand          VARCHAR(255),
  name           VARCHAR(255) NOT NULL,
  stock_code     VARCHAR(191) NOT NULL,
  category_id    INT,
  description    TEXT,
  price          DECIMAL(12,2) NOT NULL,
  discount_price DECIMAL(12,2),
  currency       VARCHAR(10) NOT NULL DEFAULT 'TRY',
  is_active      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_products_stockcode (stock_code),
  KEY idx_products_category (category_id),
  KEY idx_products_active (is_active),
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_images (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  product_id INT NOT NULL,
  `key`      VARCHAR(512) NOT NULL,
  url        TEXT NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_product_images_product (product_id),
  CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE campaign_banners (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  campaign_id VARCHAR(191) NOT NULL,
  `key`       VARCHAR(512) NOT NULL,
  url         TEXT NOT NULL,
  created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_campaign_banners_campaign (campaign_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE price_lists (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(255) NOT NULL,
  description VARCHAR(512),
  is_active   BOOLEAN NOT NULL DEFAULT TRUE,
  created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customer_prices (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  user_id       INT NOT NULL,
  product_id    INT NOT NULL,
  price_list_id INT,
  price         DECIMAL(12,2) NOT NULL,
  currency      VARCHAR(10) NOT NULL DEFAULT 'TRY',
  valid_from    DATETIME(6),
  valid_to      DATETIME(6),
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_customer_prices_user_product (user_id, product_id),
  KEY idx_customer_prices_product (product_id),
  KEY idx_customer_prices_pricelist (price_list_id),
  CONSTRAINT fk_customer_prices_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_customer_prices_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_customer_prices_pricelist FOREIGN KEY (price_list_id) REFERENCES price_lists(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE carts (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  user_id    INT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_carts_user (user_id),
  CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cart_items (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  cart_id    INT NOT NULL,
  product_id INT NOT NULL,
  qty        INT NOT NULL DEFAULT 1,
  unit_price DECIMAL(12,2) NOT NULL,
  currency   VARCHAR(10) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_cart_items_cart_product (cart_id, product_id),
  KEY idx_cart_items_product (product_id),
  CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE quotes (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  user_id    INT NOT NULL,
  status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  note       TEXT,
  admin_note TEXT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_quotes_user (user_id),
  KEY idx_quotes_status (status),
  CONSTRAINT fk_quotes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE quote_items (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  quote_id     INT NOT NULL,
  product_id   INT,
  product_name VARCHAR(255) NOT NULL,
  product_code VARCHAR(191) NOT NULL,
  qty          INT NOT NULL,
  unit_price   DECIMAL(12,2) NOT NULL,
  total_price  DECIMAL(12,2) NOT NULL,
  currency     VARCHAR(10) NOT NULL,
  created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_quote_items_quote (quote_id),
  CONSTRAINT fk_quote_items_quote FOREIGN KEY (quote_id) REFERENCES quotes(id) ON DELETE CASCADE,
  CONSTRAINT fk_quote_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  actor_id    INT,
  action      VARCHAR(191) NOT NULL,
  entity_type VARCHAR(191) NOT NULL,
  entity_id   VARCHAR(191),
  metadata    TEXT,
  ip_address  VARCHAR(64),
  created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_audit_entity (entity_type, entity_id),
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
