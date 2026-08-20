ALTER TABLE orders
  ADD COLUMN carrier_name VARCHAR(255) NULL,
  ADD COLUMN tracking_number VARCHAR(255) NULL;
