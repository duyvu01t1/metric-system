package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FollowUpReminder Entity — Phiếu nhắc chăm sóc sau bán hàng (Phân hệ 6)
 *
 * Luồng tạo tự động:
 *   Khi order.status = COMPLETED → Scheduler 6h sáng hàng ngày tạo 3 reminder:
 *     - DAY_3:  completedDate + 3 ngày
 *     - DAY_7:  completedDate + 7 ngày
 *     - DAY_10: completedDate + 10 ngày
 *
 * Nhân viên có thể tạo thêm reminder CUSTOM thủ công bất kỳ lúc nào.
 *
 * Trạng thái:
 *   PENDING  → chưa thực hiện
 *   DONE     → đã liên hệ thành công
 *   SKIPPED  → bỏ qua (không liên lạc được trong thời hạn)
 *   CANCELLED → hủy (ví dụ: khách đã mua ở chỗ khác)
 */
@Entity
@Table(name = "follow_up_reminders", indexes = {
    @Index(name = "idx_fur_order_id",       columnList = "order_id"),
    @Index(name = "idx_fur_customer_id",    columnList = "customer_id"),
    @Index(name = "idx_fur_assigned_staff", columnList = "assigned_staff_id"),
    @Index(name = "idx_fur_reminder_date",  columnList = "reminder_date"),
    @Index(name = "idx_fur_status",         columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK -> tailoring_orders.id */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** FK -> customers.id */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** FK -> staff.id — nhân viên phụ trách (mặc định = primaryStaffId của đơn hàng) */
    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    /** Ngày hẹn thực hiện chăm sóc */
    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    /**
     * Loại nhắc nhở:
     * DAY_3  — 3 ngày sau khi giao
     * DAY_7  — 7 ngày sau khi giao
     * DAY_10 — 10 ngày sau khi giao
     * CUSTOM — do nhân viên tạo thủ công
     */
    @Column(name = "reminder_type", nullable = false, length = 20)
    private String reminderType = "CUSTOM";

    /**
     * Trạng thái: PENDING | DONE | SKIPPED | CANCELLED
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    /**
     * Độ ưu tiên: LOW | MEDIUM | HIGH
     * DAY_3 = HIGH, DAY_7 = MEDIUM, DAY_10 = LOW (mặc định)
     */
    @Column(name = "priority", nullable = false, length = 10)
    private String priority = "MEDIUM";

    /** Nội dung gợi ý chăm sóc */
    @Column(name = "care_notes", columnDefinition = "TEXT")
    private String careNotes;

    /** Thời điểm hoàn thành */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** FK -> users.id — người đánh dấu DONE */
    @Column(name = "completed_by")
    private Long completedBy;

    /** Lý do bỏ qua (khi SKIPPED / CANCELLED) */
    @Column(name = "skip_reason", columnDefinition = "TEXT")
    private String skipReason;

    /** Số lần liên hệ đã thực hiện (cập nhật sau mỗi lần log contact) */
    @Column(name = "contact_count", nullable = false)
    private Integer contactCount = 0;

    /** Đánh giá hài lòng gần nhất của khách (1-5) */
    @Column(name = "customer_rating")
    private Integer customerRating;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** FK -> users.id — người tạo */
    @Column(name = "created_by")
    private Long createdBy;
}
