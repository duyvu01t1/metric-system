-- =====================================================
-- PHÂN HỆ 7 — Tài chính & Hoa hồng (Finance & Commission)
-- V9 Migration
-- =====================================================

-- ─── 1. Mở rộng staff_commissions — thêm denormalized fields & period ─────

ALTER TABLE staff_commissions
    ADD COLUMN IF NOT EXISTS staff_name          VARCHAR(200),
    ADD COLUMN IF NOT EXISTS order_code          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS production_stage_id BIGINT REFERENCES production_stages(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS period_month        SMALLINT,   -- 1-12
    ADD COLUMN IF NOT EXISTS period_year         SMALLINT,   -- e.g. 2026
    ADD COLUMN IF NOT EXISTS notes               TEXT,
    ADD COLUMN IF NOT EXISTS updated_by          BIGINT REFERENCES users(id);

-- Chỉ số tìm kiếm nhanh theo kỳ
CREATE INDEX IF NOT EXISTS idx_staff_comm_period ON staff_commissions(period_year, period_month);
CREATE INDEX IF NOT EXISTS idx_staff_comm_stage  ON staff_commissions(production_stage_id);

-- ─── 2. Chi phí vận hành / Expenses (7.4) ─────────────────────────────────

CREATE TABLE IF NOT EXISTS expenses (
    id               BIGSERIAL       PRIMARY KEY,
    expense_code     VARCHAR(50)     NOT NULL UNIQUE,   -- EXP-YYYYMM-XXXX
    -- Danh mục chi phí
    category         VARCHAR(50)     NOT NULL DEFAULT 'OTHER',
                                                --  RENT | UTILITIES | MATERIALS |
                                                --  SALARY | MARKETING | EQUIPMENT |
                                                --  MAINTENANCE | OTHER
    title            VARCHAR(300)    NOT NULL,
    description      TEXT,
    amount           DECIMAL(15,2)   NOT NULL,
    expense_date     DATE            NOT NULL,
    -- Kỳ báo cáo (tính từ expense_date nếu null)
    period_month     SMALLINT,                          -- 1-12
    period_year      SMALLINT,
    -- Có lặp lại hàng tháng không?
    is_recurring     BOOLEAN         NOT NULL DEFAULT FALSE,
    -- Phê duyệt
    approved_by      BIGINT          REFERENCES users(id),
    approved_at      TIMESTAMP,
    -- Trạng thái
    status           VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
                                                -- PENDING | APPROVED | REJECTED
    reject_reason    TEXT,
    -- Chứng từ
    receipt_url      VARCHAR(500),
    -- Audit
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       BIGINT          REFERENCES users(id),
    updated_by       BIGINT          REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_expenses_code     ON expenses(expense_code);
CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(category);
CREATE INDEX IF NOT EXISTS idx_expenses_date     ON expenses(expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_period   ON expenses(period_year, period_month);
CREATE INDEX IF NOT EXISTS idx_expenses_status   ON expenses(status);

-- ─── 3. Cài đặt tỷ lệ hoa hồng mặc định (system-wide) ────────────────────

-- Dùng bảng settings hiện có; insert mặc định nếu chưa có
INSERT INTO settings (setting_key, setting_value, description, category, data_type, is_editable)
VALUES
    ('commission.primary_rate',   '0.05',
     'Tỷ lệ hoa hồng nhân viên chính (0.05 = 5%)', 'FINANCE', 'DECIMAL', TRUE),
    ('commission.secondary_rate', '0.02',
     'Tỷ lệ hoa hồng nhân viên phụ (0.02 = 2%)',   'FINANCE', 'DECIMAL', TRUE),
    ('commission.base_type',      'TOTAL',
     'Căn tính hoa hồng: TOTAL | PROFIT | NET',    'FINANCE', 'STRING',  TRUE)
ON CONFLICT (setting_key) DO NOTHING;
