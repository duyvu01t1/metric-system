package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Delivery Entity — Phiếu giao hàng & tất toán (Phân hệ 5)
 *
 * Ràng buộc nghiệp vụ (5.3):
 *  - Chỉ được tạo khi QCCheck.status = PASSED (QC gate)
 *  - Khi confirmDelivered():
 *      1. Ghi nhận amountCollected
 *      2. Tự tạo Payment(amountCollected, DEL_COLLECTION) nếu > 0
 *      3. Cập nhật TailoringOrder:
 *           - status = COMPLETED
 *           - completedDate = today
 *           - paymentStatus = tính lại (UNPAID / PARTIAL / PAID)
 *      4. Nếu production stage DELIVERY chưa COMPLETED → cập nhật
 *
 * Trạng thái delivery:
 *   SCHEDULED      — Đã lên lịch giao
 *   OUT_FOR_DELIVERY — Đang trên đường giao
 *   DELIVERED      — Đã giao thành công → trigger tất toán
 *   RETURNED       — Khách từ chối / trả lại
 *   CANCELLED      — Hủy phiếu giao hàng
 */
@Entity
@Table(name = "deliveries", indexes = {
    @Index(name = "idx_deliveries_order_id",       columnList = "order_id"),
    @Index(name = "idx_deliveries_status",         columnList = "status"),
    @Index(name = "idx_deliveries_scheduled_date", columnList = "scheduled_date"),
    @Index(name = "idx_deliveries_qc_check_id",    columnList = "qc_check_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK -> tailoring_orders.id */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** FK -> qc_checks.id — QC phiếu PASSED đã liên kết */
    @Column(name = "qc_check_id")
    private Long qcCheckId;

    /** Mã phiếu giao hàng: DEL-20260411-001 */
    @Column(name = "delivery_code", nullable = false, unique = true, length = 60)
    private String deliveryCode;

    /**
     * Trạng thái giao hàng:
     * SCHEDULED | OUT_FOR_DELIVERY | DELIVERED | RETURNED | CANCELLED
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status = "SCHEDULED";

    /** Ngày hẹn giao */
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    /** Ngày thực tế giao xong */
    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    /**
     * Hình thức giao:
     * PICKUP        — Khách tự đến lấy tại cửa hàng
     * SHIP          — Gửi qua đơn vị vận chuyển
     * STAFF_DELIVERY — Nhân viên đem tới tận nơi
     */
    @Column(name = "delivery_method", nullable = false, length = 30)
    private String deliveryMethod = "PICKUP";

    // ─── Người nhận ────────────────────────────────────────────────────

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    // ─── Tất toán thanh toán (5.3) ────────────────────────────────────

    /** Số tiền còn lại phải thu khi giao (= totalPrice - depositAmount - các lần đã thanh toán) */
    @Column(name = "remaining_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    /** Số tiền thực thu khi giao (có thể < remaining nếu khách trả thiếu) */
    @Column(name = "amount_collected", precision = 15, scale = 2)
    private BigDecimal amountCollected = BigDecimal.ZERO;

    /** Phương thức thanh toán phần còn lại: CASH | BANK_TRANSFER | CARD | MOMO */
    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    /** True nếu đã thu đủ tổng tiền tại thời điểm giao */
    @Column(name = "is_fully_paid", nullable = false)
    private Boolean isFullyPaid = false;

    // ─── Xác nhận ────────────────────────────────────────────────────

    /** FK -> users.id — nhân viên xác nhận giao thành công */
    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /** Chữ ký / biên nhận (lưu URL ảnh hoặc văn bản mã hóa base64 ngắn) */
    @Column(name = "receipt_signature", columnDefinition = "TEXT")
    private String receiptSignature;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
