-- ════════════════════════════════════════════════════════════════════════
-- V7 — Phân hệ 5: Kiểm soát Chất lượng (QC) & Giao hàng
-- ════════════════════════════════════════════════════════════════════════
-- Luồng nghiệp vụ:
--   1. Sau khi công đoạn FITTING hoàn thành → tạo QCCheck cho đơn hàng
--   2. Nhân viên QC điền từng qc_items (checklist tiêu chuẩn)
--   3. Nếu tất cả items PASS → QCCheck.status = PASSED → mở khóa giao hàng
--   4. Tạo Delivery record → khi giao xong → thu phần còn lại →
--      order.status = COMPLETED, order.paymentStatus = PAID
-- ════════════════════════════════════════════════════════════════════════

-- ┌─────────────────────────────────────────────────────────────────────┐
-- │  qc_checks — Phiếu kiểm tra chất lượng (1 phiếu / lần kiểm tra)   │
-- └─────────────────────────────────────────────────────────────────────┘
CREATE TABLE qc_checks (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT        NOT NULL REFERENCES tailoring_orders(id) ON DELETE CASCADE,
    qc_number        VARCHAR(60)   NOT NULL UNIQUE,  -- QC-20260411-001

    -- Trạng thái phiếu QC: PENDING | IN_PROGRESS | PASSED | FAILED
    status           VARCHAR(30)   NOT NULL DEFAULT 'PENDING',

    -- Kết quả tổng thể (tự động tính từ các items hoặc override bởi QC supervisor)
    overall_result   VARCHAR(10),                   -- PASS | FAIL | NULL (chưa kết luận)

    -- Nhân viên thực hiện kiểm tra
    checked_by       BIGINT        REFERENCES users(id),
    checked_at       TIMESTAMP,

    -- Nhân viên phê duyệt (QC supervisor / manager)
    approved_by      BIGINT        REFERENCES users(id),
    approved_at      TIMESTAMP,

    -- Lần kiểm tra thứ mấy (1 = lần đầu, 2 = re-check sau khi sửa, ...)
    check_round      INT           NOT NULL DEFAULT 1,

    overall_notes    TEXT,
    internal_notes   TEXT,          -- ghi chú nội bộ (không hiển thị cho khách)

    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_qc_checks_order_id ON qc_checks(order_id);
CREATE INDEX idx_qc_checks_status   ON qc_checks(status);
CREATE INDEX idx_qc_checks_result   ON qc_checks(overall_result);

-- ┌─────────────────────────────────────────────────────────────────────┐
-- │  qc_items — Từng tiêu chí trong phiếu QC                           │
-- └─────────────────────────────────────────────────────────────────────┘
CREATE TABLE qc_items (
    id              BIGSERIAL PRIMARY KEY,
    qc_check_id     BIGINT       NOT NULL REFERENCES qc_checks(id) ON DELETE CASCADE,
    order_id        BIGINT       NOT NULL REFERENCES tailoring_orders(id),

    -- Mã tiêu chí (chuẩn hóa, dùng để nhóm thống kê)
    item_code       VARCHAR(50)  NOT NULL,
    -- Tên hiển thị tiêu chí
    item_name       VARCHAR(200) NOT NULL,
    -- Danh mục: THREAD | CHALK_MARK | FABRIC | STITCHING | MEASUREMENT | ACCESSORIES | FINISHING | OTHER
    category        VARCHAR(50)  NOT NULL DEFAULT 'OTHER',
    -- Mô tả chi tiết tiêu chí
    description     TEXT,

    -- Kết quả đánh giá: PASS | FAIL | NA (không áp dụng cho loại sản phẩm này)
    result          VARCHAR(10)  DEFAULT 'NA',
    -- Ghi chú lỗi cụ thể (bắt buộc nếu result = FAIL)
    fail_note       TEXT,
    -- Hình ảnh lỗi (URL/path)
    image_url       VARCHAR(500),

    -- Người kiểm tra item này (có thể khác người kiểm tổng)
    checked_by      BIGINT       REFERENCES users(id),
    checked_at      TIMESTAMP,

    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_qc_items_qc_check_id ON qc_items(qc_check_id);
CREATE INDEX idx_qc_items_order_id    ON qc_items(order_id);
CREATE INDEX idx_qc_items_result      ON qc_items(result);

-- ┌─────────────────────────────────────────────────────────────────────┐
-- │  deliveries — Phiếu giao hàng & tất toán                           │
-- └─────────────────────────────────────────────────────────────────────┘
CREATE TABLE deliveries (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT        NOT NULL REFERENCES tailoring_orders(id) ON DELETE CASCADE,
    qc_check_id         BIGINT        REFERENCES qc_checks(id),   -- QC phiếu đã PASSED liên kết
    delivery_code       VARCHAR(60)   NOT NULL UNIQUE,             -- DEL-20260411-001

    -- Trạng thái: SCHEDULED | OUT_FOR_DELIVERY | DELIVERED | RETURNED | CANCELLED
    status              VARCHAR(30)   NOT NULL DEFAULT 'SCHEDULED',

    -- Ngày hẹn giao / ngày thực giao
    scheduled_date      DATE,
    actual_delivery_date DATE,

    -- Hình thức giao: PICKUP (khách tự đến lấy) | SHIP (gửi tiết kiệm) | STAFF_DELIVERY (nhân viên đem đến)
    delivery_method     VARCHAR(30)   NOT NULL DEFAULT 'PICKUP',

    -- Người nhận
    recipient_name      VARCHAR(200),
    recipient_phone     VARCHAR(20),
    delivery_address    TEXT,

    -- ─── Tất toán thanh toán (5.3) ──────────────────────────────────
    -- Số tiền còn lại phải thu khi giao
    remaining_amount    DECIMAL(15,2) NOT NULL DEFAULT 0,
    -- Số tiền thực thu khi giao
    amount_collected    DECIMAL(15,2) DEFAULT 0,
    -- Phương thức thanh toán phần còn lại
    payment_method      VARCHAR(30),
    -- TRUE nếu đã thu đủ tại thời điểm giao
    is_fully_paid       BOOLEAN       NOT NULL DEFAULT FALSE,

    -- ─── Xác nhận ────────────────────────────────────────────────────
    -- Nhân viên xác nhận giao thành công
    confirmed_by        BIGINT        REFERENCES users(id),
    confirmed_at        TIMESTAMP,

    -- Chữ ký / biên nhận (URL ảnh hoặc text)
    receipt_signature   TEXT,

    notes               TEXT,

    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_deliveries_order_id       ON deliveries(order_id);
CREATE INDEX idx_deliveries_status         ON deliveries(status);
CREATE INDEX idx_deliveries_scheduled_date ON deliveries(scheduled_date);
CREATE INDEX idx_deliveries_qc_check_id    ON deliveries(qc_check_id);
