-- =====================================================
-- PHÂN HỆ 2 — CRM / Quản lý Khách hàng
-- V3 Migration
-- =====================================================

-- ─── 1. Bổ sung role SALE, STAFF, MANAGER ─────────────────────────────────

INSERT INTO user_roles (name, description)
VALUES
    ('MANAGER', 'Quản lý — duyệt phân khách, xem toàn bộ hệ thống'),
    ('SALE',    'Nhân viên kinh doanh — chăm sóc khách hàng, xử lý lead'),
    ('STAFF',   'Thợ may — thực hiện sản xuất, không thấy tài chính')
ON CONFLICT (name) DO NOTHING;

-- ─── 2. Bảng Staff ────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS staff (
    id                   BIGSERIAL PRIMARY KEY,
    staff_code           VARCHAR(50)    NOT NULL UNIQUE,
    user_id              BIGINT         NOT NULL UNIQUE,  -- FK -> users
    full_name            VARCHAR(200)   NOT NULL,
    phone                VARCHAR(20),
    department           VARCHAR(100),
    staff_role           VARCHAR(20)    NOT NULL DEFAULT 'SALE', -- SALE | STAFF | MANAGER
    performance_score    DECIMAL(5,2)   NOT NULL DEFAULT 50.00,  -- 0-100, manager sets
    monthly_target       DECIMAL(15,2)  DEFAULT 0,               -- doanh số kế hoạch
    base_commission_rate DECIMAL(6,4)   DEFAULT 0.0500,          -- % hoa hồng cơ bản
    total_leads          INT            NOT NULL DEFAULT 0,
    total_converted      INT            NOT NULL DEFAULT 0,
    conversion_rate      DECIMAL(5,2)   NOT NULL DEFAULT 0.00,   -- %
    total_revenue        DECIMAL(15,2)  NOT NULL DEFAULT 0,      -- tổng doanh thu mang về
    is_active            BOOLEAN        NOT NULL DEFAULT TRUE,
    notes                TEXT,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           BIGINT,
    FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_staff_code         ON staff(staff_code);
CREATE INDEX IF NOT EXISTS idx_staff_user_id      ON staff(user_id);
CREATE INDEX IF NOT EXISTS idx_staff_role         ON staff(staff_role);
CREATE INDEX IF NOT EXISTS idx_staff_is_active    ON staff(is_active);
CREATE INDEX IF NOT EXISTS idx_staff_perf_score   ON staff(performance_score);

-- ─── 3. Mở rộng bảng customers ────────────────────────────────────────────

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS assigned_staff_id  BIGINT         REFERENCES staff(id),
    ADD COLUMN IF NOT EXISTS cac                DECIMAL(15,2)  DEFAULT 0,
    ADD COLUMN IF NOT EXISTS interaction_count  INT            NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS source_channel_id  BIGINT         REFERENCES channels(id),
    ADD COLUMN IF NOT EXISTS last_interaction_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_customers_assigned_staff ON customers(assigned_staff_id);
CREATE INDEX IF NOT EXISTS idx_customers_source_channel ON customers(source_channel_id);

-- ─── 4. Bảng CustomerInteraction ──────────────────────────────────────────

CREATE TABLE IF NOT EXISTS customer_interactions (
    id                  BIGSERIAL     PRIMARY KEY,
    customer_id         BIGINT        NOT NULL,
    staff_id            BIGINT,                   -- nhân viên thực hiện (nullable = system)
    interaction_type    VARCHAR(30)   NOT NULL,   -- CALL | NOTE | MESSAGE | MEETING | EMAIL
    content             TEXT,
    outcome             TEXT,                     -- kết quả / phản hồi của khách
    next_followup_at    TIMESTAMP,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (staff_id)    REFERENCES staff(id)
);

CREATE INDEX IF NOT EXISTS idx_cust_inter_customer ON customer_interactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_cust_inter_staff    ON customer_interactions(staff_id);
CREATE INDEX IF NOT EXISTS idx_cust_inter_type     ON customer_interactions(interaction_type);
CREATE INDEX IF NOT EXISTS idx_cust_inter_created  ON customer_interactions(created_at);

-- ─── 5. Bảng LeadAssignment — phân khách + approval workflow ──────────────

CREATE TABLE IF NOT EXISTS lead_assignments (
    id                      BIGSERIAL    PRIMARY KEY,
    lead_id                 BIGINT       NOT NULL,
    staff_id                BIGINT       NOT NULL,  -- nhân viên được phân
    assigned_by             BIGINT,                 -- người thực hiện phân (NULL = system)
    assignment_type         VARCHAR(30)  NOT NULL DEFAULT 'AUTO_ROUND_ROBIN',
                                                    -- AUTO_ROUND_ROBIN | AUTO_PERFORMANCE | MANUAL
    approval_status         VARCHAR(20)  NOT NULL DEFAULT 'APPROVED',
                                                    -- APPROVED | PENDING | REJECTED
    approved_by_manager_id  BIGINT,
    approved_at             TIMESTAMP,
    rejection_reason        TEXT,
    notes                   TEXT,
    is_current              BOOLEAN      NOT NULL DEFAULT TRUE, -- chỉ 1 assignment active / lead
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lead_id)               REFERENCES leads(id) ON DELETE CASCADE,
    FOREIGN KEY (staff_id)              REFERENCES staff(id),
    FOREIGN KEY (assigned_by)           REFERENCES users(id),
    FOREIGN KEY (approved_by_manager_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_lead_assign_lead_id    ON lead_assignments(lead_id);
CREATE INDEX IF NOT EXISTS idx_lead_assign_staff_id   ON lead_assignments(staff_id);
CREATE INDEX IF NOT EXISTS idx_lead_assign_status     ON lead_assignments(approval_status);
CREATE INDEX IF NOT EXISTS idx_lead_assign_is_current ON lead_assignments(is_current);
