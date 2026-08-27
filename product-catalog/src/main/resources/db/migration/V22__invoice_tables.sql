-- Fatura + müşteri fatura bilgileri. Mevcut orders/users/products/cari_transactions tablolarına
-- hiçbir ALTER/DROP yapılmaz (users'a sadece yeni nullable kolonlar eklenir).

ALTER TABLE users ADD COLUMN tax_number VARCHAR(50) NULL;
ALTER TABLE users ADD COLUMN tax_office VARCHAR(150) NULL;
ALTER TABLE users ADD COLUMN billing_address VARCHAR(500) NULL;

CREATE TABLE invoices (
  id                     INT AUTO_INCREMENT PRIMARY KEY,
  invoice_number         VARCHAR(30) NOT NULL,
  order_id               INT NOT NULL,
  user_id                INT NOT NULL,
  status                 VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
  issued_at              DATETIME(6) NOT NULL,
  due_date               DATETIME(6) NULL,
  seller_company_name    VARCHAR(255) NOT NULL,
  seller_tax_number      VARCHAR(50) NOT NULL,
  seller_tax_office      VARCHAR(150) NOT NULL,
  seller_address         VARCHAR(500) NOT NULL,
  customer_company_name  VARCHAR(255) NOT NULL,
  customer_tax_number    VARCHAR(50) NOT NULL,
  customer_tax_office    VARCHAR(150) NOT NULL,
  customer_address       VARCHAR(500) NOT NULL,
  cancelled_at           DATETIME(6) NULL,
  cancelled_by           INT NULL,
  created_at             DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at             DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_invoices_order (order_id),
  UNIQUE KEY uq_invoices_number (invoice_number),
  KEY idx_invoices_user (user_id),
  KEY idx_invoices_status (status),
  CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
  CONSTRAINT fk_invoices_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT fk_invoices_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoice_items (
  id                  INT AUTO_INCREMENT PRIMARY KEY,
  invoice_id          INT NOT NULL,
  product_id          INT NULL,
  product_code        VARCHAR(191) NOT NULL,
  product_name        VARCHAR(255) NOT NULL,
  quantity            INT NOT NULL,
  unit_price          DECIMAL(12,2) NOT NULL,
  line_total          DECIMAL(12,2) NOT NULL,
  currency            VARCHAR(10) NOT NULL,
  price_includes_vat  BOOLEAN NOT NULL DEFAULT FALSE,
  vat_rate            DECIMAL(5,2) NOT NULL,
  vat_amount          DECIMAL(12,2) NOT NULL,
  created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_invoice_items_invoice (invoice_id),
  CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
  CONSTRAINT fk_invoice_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
