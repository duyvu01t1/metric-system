-- Phân hệ 8 — Analytics performance indexes & default settings

-- Improve reporting query performance
CREATE INDEX IF NOT EXISTS idx_orders_source_channel_id
    ON tailoring_orders(source_channel_id);

CREATE INDEX IF NOT EXISTS idx_orders_primary_staff_id
    ON tailoring_orders(primary_staff_id);

CREATE INDEX IF NOT EXISTS idx_orders_secondary_staff_id
    ON tailoring_orders(secondary_staff_id);

CREATE INDEX IF NOT EXISTS idx_orders_completed_date
    ON tailoring_orders(completed_date);

CREATE INDEX IF NOT EXISTS idx_orders_order_type_status
    ON tailoring_orders(order_type, status);

-- Default analytics thresholds
INSERT INTO settings (setting_key, setting_value, description, category, data_type, is_editable)
VALUES
    ('analytics.performance.good_threshold', '80', 'Ngưỡng hiệu suất tốt cho dashboard analytics', 'ANALYTICS', 'INT', true),
    ('analytics.performance.warning_threshold', '50', 'Ngưỡng hiệu suất cảnh báo cho dashboard analytics', 'ANALYTICS', 'INT', true),
    ('analytics.lead_conversion_target', '35', 'Mục tiêu tỷ lệ chuyển đổi lead (%)', 'ANALYTICS', 'DECIMAL', true)
ON CONFLICT (setting_key) DO NOTHING;
