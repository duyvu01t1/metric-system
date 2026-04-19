-- =====================================================
-- PHÂN HỆ 3 — Quản lý Đơn hàng & Báo giá
-- V5 Migration
-- =====================================================

-- ─── 1. Đối tác / Affiliate ───────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS affiliates (
    id                    BIGSERIAL      PRIMARY KEY,
    affiliate_code        VARCHAR(50)    NOT NULL UNIQUE,
    company_name          VARCHAR(200)   NOT NULL,
    contact_name          VARCHAR(200),
    phone                 VARCHAR(20),
    email                 VARCHAR(255),
    commission_rate       DECIMAL(6,4)   NOT NULL DEFAULT 0.0500, -- % tỷ lệ hoa hồng đối tác
    total_orders          INT            NOT NULL DEFAULT 0,
    total_commission_paid DECIMAL(15,2)  NOT NULL DEFAULT 0,
    is_active             BOOLEAN        NOT NULL DEFAULT TRUE,
    notes                 TEXT,
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT         REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_affiliates_code      ON affiliates(affiliate_code);
CREATE INDEX IF NOT EXISTS idx_affiliates_is_active ON affiliates(is_active);

-- ─── 2. Mã giảm giá / DiscountCode ───────────────────────────────────────

CREATE TABLE IF NOT EXISTS discount_codes (
    id                 BIGSERIAL      PRIMARY KEY,
    code               VARCHAR(50)    NOT NULL UNIQUE,
    affiliate_id       BIGINT         REFERENCES affiliates(id),
    discount_type      VARCHAR(20)    NOT NULL DEFAULT 'PERCENT', -- PERCENT | FIXED
    discount_value     DECIMAL(15,2)  NOT NULL,                 -- % hoặc VNĐ
    min_order_value    DECIMAL(15,2)  DEFAULT 0,                -- đơn tối thiểu để áp
    max_discount_amount DECIMAL(15,2),                          -- giới hạn số tiền giảm tối đa (PERCENT)
    max_uses           INT,                                     -- NULL = không giới hạn
    used_count         INT            NOT NULL DEFAULT 0,
    valid_from         DATE,
    valid_until        DATE,
    is_active          BOOLEAN        NOT NULL DEFAULT TRUE,
    description        TEXT,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         BIGINT         REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_discount_code       ON discount_codes(code);
CREATE INDEX IF NOT EXISTS idx_discount_affiliate  ON discount_codes(affiliate_id);
CREATE INDEX IF NOT EXISTS idx_discount_is_active  ON discount_codes(is_active);
CREATE INDEX IF NOT EXISTS idx_discount_valid_until ON discount_codes(valid_until);

-- ─── 3. Báo giá / Quotation ───────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS quotations (
    id                  BIGSERIAL       PRIMARY KEY,
    quotation_code      VARCHAR(50)     NOT NULL UNIQUE,
    order_id            BIGINT          REFERENCES tailoring_orders(id) ON DELETE SET NULL,
    customer_id         BIGINT          NOT NULL REFERENCES customers(id),
    -- Thông tin sản phẩm
    order_type          VARCHAR(50)     NOT NULL,      -- SUIT, SHIRT, PANTS, DRESS, CUSTOM
    fabric_material     VARCHAR(500),                  -- chất liệu vải
    fabric_color        VARCHAR(100),
    accessories         TEXT,                          -- phụ liệu (nút, khóa, lót, v.v.)
    quantity            INT             NOT NULL DEFAULT 1,
    -- Giá
    unit_price          DECIMAL(15,2),
    fabric_cost         DECIMAL(15,2)   DEFAULT 0,    -- chi phí vải
    accessories_cost    DECIMAL(15,2)   DEFAULT 0,    -- chi phí phụ liệu
    labor_cost          DECIMAL(15,2)   DEFAULT 0,    -- nhân công
    subtotal            DECIMAL(15,2),                -- tổng trước giảm
    discount_code_id    BIGINT          REFERENCES discount_codes(id),
    discount_amount     DECIMAL(15,2)   DEFAULT 0,
    total_amount        DECIMAL(15,2),                -- tổng sau giảm
    -- Kênh nguồn
    source_channel_id   BIGINT          REFERENCES channels(id),
    -- Trạng thái
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
                                                      -- DRAFT | SENT | ACCEPTED | REJECTED | CONVERTED
    valid_until         DATE,
    notes               TEXT,
    -- Audit
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT          REFERENCES users(id),
    updated_by          BIGINT          REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_quotations_code        ON quotations(quotation_code);
CREATE INDEX IF NOT EXISTS idx_quotations_order_id    ON quotations(order_id);
CREATE INDEX IF NOT EXISTS idx_quotations_customer_id ON quotations(customer_id);
CREATE INDEX IF NOT EXISTS idx_quotations_status      ON quotations(status);

-- ─── 4. Mở rộng tailoring_orders ─────────────────────────────────────────

ALTER TABLE tailoring_orders
    ADD COLUMN IF NOT EXISTS fabric_material     VARCHAR(500),
    ADD COLUMN IF NOT EXISTS fabric_color        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS accessories         TEXT,
    ADD COLUMN IF NOT EXISTS source_channel_id   BIGINT REFERENCES channels(id),
    -- Đặt cọc (3.6)
    ADD COLUMN IF NOT EXISTS deposit_amount      DECIMAL(15,2)  DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deposit_status      VARCHAR(30)    DEFAULT 'NONE',
                                                              -- NONE | PENDING | CONFIRMED
    ADD COLUMN IF NOT EXISTS deposit_date        DATE,
    ADD COLUMN IF NOT EXISTS deposit_confirmed_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS deposit_confirmed_at TIMESTAMP,
    -- Báo giá gốc
    ADD COLUMN IF NOT EXISTS quotation_id        BIGINT REFERENCES quotations(id),
    ADD COLUMN IF NOT EXISTS discount_code_id    BIGINT REFERENCES discount_codes(id),
    ADD COLUMN IF NOT EXISTS discount_amount     DECIMAL(15,2)  DEFAULT 0,
    -- 2 nhân viên (3.9)
    ADD COLUMN IF NOT EXISTS primary_staff_id    BIGINT REFERENCES staff(id),
    ADD COLUMN IF NOT EXISTS secondary_staff_id  BIGINT REFERENCES staff(id);

CREATE INDEX IF NOT EXISTS idx_orders_source_channel ON tailoring_orders(source_channel_id);
CREATE INDEX IF NOT EXISTS idx_orders_deposit_status ON tailoring_orders(deposit_status);
CREATE INDEX IF NOT EXISTS idx_orders_primary_staff  ON tailoring_orders(primary_staff_id);

-- ─── 5. Hoa hồng nhân viên / StaffCommission (3.8) ───────────────────────

CREATE TABLE IF NOT EXISTS staff_commissions (
    id                BIGSERIAL       PRIMARY KEY,
    order_id          BIGINT          NOT NULL REFERENCES tailoring_orders(id) ON DELETE CASCADE,
    staff_id          BIGINT          NOT NULL REFERENCES staff(id),
    staff_role_type   VARCHAR(20)     NOT NULL, -- PRIMARY | SECONDARY
    commission_rate   DECIMAL(6,4)    NOT NULL, -- tỷ lệ %
    commission_base   DECIMAL(15,2)   NOT NULL, -- giá trị làm căn tính
    commission_amount DECIMAL(15,2)   NOT NULL, -- số tiền thực tế
    is_manual_override BOOLEAN        NOT NULL DEFAULT FALSE, -- đã chỉnh sửa thủ công
    override_reason   TEXT,
    is_paid           BOOLEAN         NOT NULL DEFAULT FALSE,
    paid_at           TIMESTAMP,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT          REFERENCES users(id),
    UNIQUE (order_id, staff_id, staff_role_type)
);

CREATE INDEX IF NOT EXISTS idx_staff_comm_order   ON staff_commissions(order_id);
CREATE INDEX IF NOT EXISTS idx_staff_comm_staff   ON staff_commissions(staff_id);
CREATE INDEX IF NOT EXISTS idx_staff_comm_is_paid ON staff_commissions(is_paid);

-- ─── 6. Hoa hồng đối tác / AffiliateCommission ───────────────────────────

CREATE TABLE IF NOT EXISTS affiliate_commissions (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL REFERENCES tailoring_orders(id) ON DELETE CASCADE,
    affiliate_id        BIGINT          NOT NULL REFERENCES affiliates(id),
    discount_code_id    BIGINT          REFERENCES discount_codes(id),
    discount_given      DECIMAL(15,2)   NOT NULL DEFAULT 0, -- số tiền giảm cho khách
    commission_rate     DECIMAL(6,4)    NOT NULL,
    commission_amount   DECIMAL(15,2)   NOT NULL,           -- số tiền trả đối tác
    is_paid             BOOLEAN         NOT NULL DEFAULT FALSE,
    paid_at             TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (order_id, affiliate_id)
);

CREATE INDEX IF NOT EXISTS idx_aff_comm_order     ON affiliate_commissions(order_id);
CREATE INDEX IF NOT EXISTS idx_aff_comm_affiliate ON affiliate_commissions(affiliate_id);
