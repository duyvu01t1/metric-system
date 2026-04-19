-- ════════════════════════════════════════════════════════════════════════
-- V8 — Phân hệ 6: Chăm sóc Sau Bán Hàng (After-Sales Care)
-- ════════════════════════════════════════════════════════════════════════
-- Luồng nghiệp vụ:
--   1. Khi order.status = COMPLETED →
--          Scheduler (6h sáng hàng ngày) tự tạo 3 reminder:
--          DAY_3 (completedDate + 3), DAY_7 (+7), DAY_10 (+10)
--   2. Nhân viên xem danh sách hôm nay cần chăm sóc → gọi / nhắn tin
--   3. Ghi nhận kết quả (FollowUpLog) và đánh dấu DONE hoặc SKIPPED
--   4. Nếu khách có nhu cầu tiếp → nhân viên tạo lead mới
-- ════════════════════════════════════════════════════════════════════════

-- ┌─────────────────────────────────────────────────────────────────────┐
-- │  follow_up_reminders — Phiếu nhắc chăm sóc sau bán hàng            │
-- └─────────────────────────────────────────────────────────────────────┘
CREATE TABLE follow_up_reminders (
    id                  BIGSERIAL       PRIMARY KEY,

    -- Đơn hàng đã hoàn thành
    order_id            BIGINT          NOT NULL REFERENCES tailoring_orders(id) ON DELETE CASCADE,

    -- Khách hàng (denormalized để query nhanh)
    customer_id         BIGINT          NOT NULL REFERENCES customers(id),

    -- Nhân viên được giao (ban đầu = nhân viên chính của đơn hàng)
    assigned_staff_id   BIGINT          REFERENCES staff(id),

    -- Ngày hẹn chăm sóc
    reminder_date       DATE            NOT NULL,

    -- Loại nhắc nhở
    -- DAY_3:  3 ngày sau khi giao xong
    -- DAY_7:  7 ngày sau khi giao xong
    -- DAY_10: 10 ngày sau khi giao xong
    -- CUSTOM: do nhân viên tạo thủ công
    reminder_type       VARCHAR(20)     NOT NULL DEFAULT 'CUSTOM',

    -- PENDING | DONE | SKIPPED | CANCELLED
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    -- Độ ưu tiên: LOW | MEDIUM | HIGH
    priority            VARCHAR(10)     NOT NULL DEFAULT 'MEDIUM',

    -- Nội dung gợi ý chăm sóc (hệ thống sinh tự động hoặc nhân viên nhập)
    care_notes          TEXT,

    -- Khi hoàn thành
    completed_at        TIMESTAMP,
    completed_by        BIGINT          REFERENCES users(id),

    -- Khi bỏ qua/hủy
    skip_reason         TEXT,

    -- Số lần liên hệ đã thực hiện (tổng hợp từ follow_up_logs)
    contact_count       INT             NOT NULL DEFAULT 0,

    -- Đánh giá hài lòng của khách (1-5) — lấy từ log cuối cùng có rating
    customer_rating     INT             CHECK (customer_rating BETWEEN 1 AND 5),

    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT          REFERENCES users(id)
);

CREATE INDEX idx_fur_order_id        ON follow_up_reminders(order_id);
CREATE INDEX idx_fur_customer_id     ON follow_up_reminders(customer_id);
CREATE INDEX idx_fur_assigned_staff  ON follow_up_reminders(assigned_staff_id);
CREATE INDEX idx_fur_reminder_date   ON follow_up_reminders(reminder_date);
CREATE INDEX idx_fur_status          ON follow_up_reminders(status);
CREATE INDEX idx_fur_type_date       ON follow_up_reminders(reminder_type, reminder_date);

-- Đảm bảo mỗi loại reminder chỉ tạo 1 lần per đơn hàng
CREATE UNIQUE INDEX idx_fur_unique_type_per_order
    ON follow_up_reminders(order_id, reminder_type)
    WHERE reminder_type IN ('DAY_3', 'DAY_7', 'DAY_10');

-- ┌─────────────────────────────────────────────────────────────────────┐
-- │  follow_up_logs — Nhật ký từng lần liên hệ chăm sóc               │
-- └─────────────────────────────────────────────────────────────────────┘
CREATE TABLE follow_up_logs (
    id              BIGSERIAL       PRIMARY KEY,

    -- Phiếu nhắc liên quan
    reminder_id     BIGINT          NOT NULL REFERENCES follow_up_reminders(id) ON DELETE CASCADE,

    -- Thông tin order / khách (denormalized cho báo cáo)
    order_id        BIGINT          NOT NULL REFERENCES tailoring_orders(id),
    customer_id     BIGINT          NOT NULL REFERENCES customers(id),

    -- Nhân viên thực hiện liên hệ
    staff_id        BIGINT          REFERENCES users(id),

    -- Hình thức: CALL | MESSAGE | EMAIL | VISIT | ZALO | FACEBOOK
    contact_type    VARCHAR(20)     NOT NULL DEFAULT 'CALL',

    -- Kết quả:
    -- ANSWERED          — Khách nghe máy / phản hồi
    -- NO_ANSWER         — Không liên lạc được
    -- CALLBACK          — Khách sẽ gọi lại
    -- LEFT_MESSAGE      — Để lại tin nhắn / voicemail
    -- SATISFIED         — Khách hài lòng
    -- COMPLAINED        — Khách khiếu nại / phàn nàn
    -- REPEAT_ORDER      — Khách muốn đặt thêm đơn mới
    -- NOT_INTERESTED    — Khách không có nhu cầu
    outcome         VARCHAR(30)     NOT NULL DEFAULT 'ANSWERED',

    -- Nội dung chi tiết cuộc gọi / tin nhắn
    content         TEXT,

    -- Phản hồi / ý kiến khách hàng
    customer_feedback TEXT,

    -- Đánh giá sự hài lòng của khách (1-5 sao)
    customer_rating INT             CHECK (customer_rating BETWEEN 1 AND 5),

    -- Hành động tiếp theo (nhân viên tự ghi)
    next_action     TEXT,

    -- Thời điểm thực hiện liên hệ
    contacted_at    TIMESTAMP       NOT NULL DEFAULT NOW(),

    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ful_reminder_id  ON follow_up_logs(reminder_id);
CREATE INDEX idx_ful_order_id     ON follow_up_logs(order_id);
CREATE INDEX idx_ful_customer_id  ON follow_up_logs(customer_id);
CREATE INDEX idx_ful_staff_id     ON follow_up_logs(staff_id);
CREATE INDEX idx_ful_contacted_at ON follow_up_logs(contacted_at DESC);
CREATE INDEX idx_ful_outcome      ON follow_up_logs(outcome);
