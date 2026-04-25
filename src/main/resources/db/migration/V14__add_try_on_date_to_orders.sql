-- V14: Add try_on_date (NGÀY THỬ) to tailoring_orders
ALTER TABLE tailoring_orders
    ADD COLUMN IF NOT EXISTS try_on_date DATE;
