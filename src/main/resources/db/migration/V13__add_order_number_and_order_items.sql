-- V13: Add order_number (SỐ BIÊN LẠI) to tailoring_orders and create order_items table

-- 1. Sequence for auto-increment order numbers starting at a reasonable base
CREATE SEQUENCE IF NOT EXISTS order_number_seq START WITH 10000 INCREMENT BY 1;

-- 2. Add order_number column to tailoring_orders
ALTER TABLE tailoring_orders
    ADD COLUMN IF NOT EXISTS order_number BIGINT;

-- Populate existing rows
UPDATE tailoring_orders
SET order_number = nextval('order_number_seq')
WHERE order_number IS NULL;

-- Make non-nullable and unique
ALTER TABLE tailoring_orders
    ALTER COLUMN order_number SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_order_number
    ON tailoring_orders(order_number);

-- 3. Create order_items table (chi tiết từng sản phẩm trong đơn hàng)
CREATE TABLE IF NOT EXISTS order_items (
    id           BIGSERIAL    PRIMARY KEY,
    order_id     BIGINT       NOT NULL REFERENCES tailoring_orders(id) ON DELETE CASCADE,
    product_name VARCHAR(200) NOT NULL,            -- SẢN PHẨM
    product_code VARCHAR(200),                     -- MÃ SP
    fabric_meters DECIMAL(8, 2),                   -- MET VẢI
    quantity     INTEGER      NOT NULL DEFAULT 1,  -- SỐ LƯỢNG
    unit_price   DECIMAL(15, 2) NOT NULL,          -- ĐƠN GIÁ
    total_price  DECIMAL(15, 2) NOT NULL,          -- THÀNH TIỀN = quantity * unit_price
    sort_order   INTEGER DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
