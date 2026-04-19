-- =====================================================
-- PHÂN HỆ 4 — Quản lý Sản xuất & Tiến độ
-- V6 Migration
-- =====================================================

-- ─── 1. Công đoạn sản xuất / ProductionStage ──────────────────────────────
--
-- Mỗi đơn hàng có đúng 4 công đoạn theo thứ tự:
--   1 = CUT (Cắt vải)
--   2 = ASSEMBLE (Ráp may)
--   3 = FITTING (Thử + chỉnh sửa)
--   4 = DELIVERY (Hoàn thiện & giao hàng)
--
-- alert_status được tính tự động dựa trên planned_end_date và ngưỡng yellow (configurable):
--   GREEN  = planned_end_date > NOW() + yellow_threshold_days
--   YELLOW = NOW() <= planned_end_date <= NOW() + yellow_threshold_days
--   RED    = planned_end_date < NOW() VÀ status != COMPLETED

CREATE TABLE IF NOT EXISTS production_stages (
    id                    BIGSERIAL       PRIMARY KEY,
    order_id              BIGINT          NOT NULL REFERENCES tailoring_orders(id) ON DELETE CASCADE,
    stage_type            VARCHAR(30)     NOT NULL,  -- CUT | ASSEMBLE | FITTING | DELIVERY
    stage_order           INT             NOT NULL,  -- 1 | 2 | 3 | 4
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
                                                     -- PENDING | IN_PROGRESS | COMPLETED | SKIPPED

    -- Nhân công gán vào công đoạn
    assigned_worker_id    BIGINT          REFERENCES staff(id),  -- thợ may thực hiện
    assigned_sale_id      BIGINT          REFERENCES staff(id),  -- sale phụ trách

    -- Lịch kế hoạch
    planned_start_date    DATE,
    planned_end_date      DATE,
    actual_start_date     DATE,
    actual_end_date       DATE,

    -- Cảnh báo màu (4.6)
    alert_status          VARCHAR(10)     NOT NULL DEFAULT 'GREEN', -- GREEN | YELLOW | RED

    -- Hoa hồng công đoạn (chỉnh sửa thủ công — 4.3 / 3.3)
    commission_rate       DECIMAL(6,4)    NOT NULL DEFAULT 0,
    commission_amount     DECIMAL(15,2)   NOT NULL DEFAULT 0,
    is_commission_override BOOLEAN        NOT NULL DEFAULT FALSE,
    commission_override_reason TEXT,

    notes                 TEXT,
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT          REFERENCES users(id),
    completed_by          BIGINT          REFERENCES users(id),
    completed_at          TIMESTAMP,

    CONSTRAINT uq_order_stage_type UNIQUE(order_id, stage_type),
    CONSTRAINT uq_order_stage_order UNIQUE(order_id, stage_order)
);

CREATE INDEX IF NOT EXISTS idx_prod_stages_order_id   ON production_stages(order_id);
CREATE INDEX IF NOT EXISTS idx_prod_stages_status     ON production_stages(status);
CREATE INDEX IF NOT EXISTS idx_prod_stages_worker     ON production_stages(assigned_worker_id);
CREATE INDEX IF NOT EXISTS idx_prod_stages_sale       ON production_stages(assigned_sale_id);
CREATE INDEX IF NOT EXISTS idx_prod_stages_alert      ON production_stages(alert_status);
CREATE INDEX IF NOT EXISTS idx_prod_stages_planned_end ON production_stages(planned_end_date);

-- ─── 2. Nhật ký thay đổi công đoạn / ProductionStageLog ──────────────────
--
-- Lưu vết TOÀN BỘ thay đổi: chuyển trạng thái, đổi thợ, đổi lịch,
-- cập nhật hoa hồng, thêm ghi chú.

CREATE TABLE IF NOT EXISTS production_stage_logs (
    id           BIGSERIAL   PRIMARY KEY,
    stage_id     BIGINT      NOT NULL REFERENCES production_stages(id) ON DELETE CASCADE,
    order_id     BIGINT      NOT NULL REFERENCES tailoring_orders(id),
    change_type  VARCHAR(50) NOT NULL,
                              -- STATUS_CHANGED | WORKER_ASSIGNED | SALE_ASSIGNED
                              -- COMMISSION_UPDATED | SCHEDULE_CHANGED | NOTE_ADDED

    old_value    TEXT,        -- giá trị trước thay đổi (JSON hoặc plain text)
    new_value    TEXT,        -- giá trị sau thay đổi
    change_note  TEXT,        -- ghi chú của người thực hiện thay đổi

    changed_by   BIGINT      REFERENCES users(id),
    changed_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stage_log_stage_id ON production_stage_logs(stage_id);
CREATE INDEX IF NOT EXISTS idx_stage_log_order_id ON production_stage_logs(order_id);
CREATE INDEX IF NOT EXISTS idx_stage_log_changed_at ON production_stage_logs(changed_at);

-- ─── 3. Lịch sản xuất / ProductionCalendar ───────────────────────────────
--
-- Mỗi sự kiện là 1 công đoạn được gán cho 1 nhân viên (thợ hoặc sale).
-- calendar_type phân biệt:
--   ORDER  = calendar tổng của đơn hàng
--   WORKER = lịch của thợ may cụ thể
--   SALE   = lịch của sale cụ thể
--
-- event_color theo cảnh báo: #66bb6a (green) | #ffb74d (yellow) | #ef5350 (red)

CREATE TABLE IF NOT EXISTS production_calendars (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES tailoring_orders(id) ON DELETE CASCADE,
    stage_id        BIGINT          REFERENCES production_stages(id) ON DELETE SET NULL,
    staff_id        BIGINT          REFERENCES staff(id),
    calendar_type   VARCHAR(20)     NOT NULL DEFAULT 'ORDER', -- ORDER | WORKER | SALE
    event_title     VARCHAR(300)    NOT NULL,
    event_start     TIMESTAMP       NOT NULL,
    event_end       TIMESTAMP       NOT NULL,
    event_color     VARCHAR(20)     DEFAULT '#66bb6a',
    all_day         BOOLEAN         NOT NULL DEFAULT FALSE,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prod_cal_order_id    ON production_calendars(order_id);
CREATE INDEX IF NOT EXISTS idx_prod_cal_staff_id    ON production_calendars(staff_id);
CREATE INDEX IF NOT EXISTS idx_prod_cal_type        ON production_calendars(calendar_type);
CREATE INDEX IF NOT EXISTS idx_prod_cal_event_start ON production_calendars(event_start);
