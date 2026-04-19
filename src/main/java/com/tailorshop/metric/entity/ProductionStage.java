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
 * ProductionStage Entity — Công đoạn sản xuất
 *
 * Mỗi đơn hàng tạo đúng 4 công đoạn theo thứ tự cố định:
 *   1 = CUT       (Cắt vải)
 *   2 = ASSEMBLE  (Ráp may)
 *   3 = FITTING   (Thử + chỉnh sửa)
 *   4 = DELIVERY  (Hoàn thiện & giao hàng)
 *
 * Logic alert (4.6):
 *   GREEN  = planned_end_date > today + yellowThresholdDays
 *   YELLOW = today ≤ planned_end_date ≤ today + yellowThresholdDays
 *   RED    = planned_end_date < today  AND status ≠ COMPLETED
 *
 * Deposit gate (3.7 / 4.1):
 *   Phân hệ sản xuất chỉ được mở khi depositStatus = CONFIRMED.
 *   ProductionService.assertDepositConfirmed() sẽ kiểm tra trước khi tạo stages.
 */
@Entity
@Table(name = "production_stages", indexes = {
    @Index(name = "idx_prod_stages_order_id",    columnList = "order_id"),
    @Index(name = "idx_prod_stages_status",      columnList = "status"),
    @Index(name = "idx_prod_stages_worker",      columnList = "assigned_worker_id"),
    @Index(name = "idx_prod_stages_sale",        columnList = "assigned_sale_id"),
    @Index(name = "idx_prod_stages_alert",       columnList = "alert_status"),
    @Index(name = "idx_prod_stages_planned_end", columnList = "planned_end_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK -> tailoring_orders.id */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * Loại công đoạn: CUT | ASSEMBLE | FITTING | DELIVERY
     */
    @Column(name = "stage_type", nullable = false, length = 30)
    private String stageType;

    /** Thứ tự công đoạn: 1 → 2 → 3 → 4 */
    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    /**
     * Trạng thái: PENDING | IN_PROGRESS | COMPLETED | SKIPPED
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    // ─── Nhân công ───────────────────────────────────────────────────────────

    /** FK -> staff.id — thợ may thực hiện công đoạn */
    @Column(name = "assigned_worker_id")
    private Long assignedWorkerId;

    /** FK -> staff.id — sale phụ trách (theo dõi / nghiệm thu) */
    @Column(name = "assigned_sale_id")
    private Long assignedSaleId;

    // ─── Lịch kế hoạch ───────────────────────────────────────────────────────

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    // ─── Cảnh báo màu (4.6) ──────────────────────────────────────────────────

    /**
     * Trạng thái cảnh báo: GREEN | YELLOW | RED
     * Được tính tự động mỗi khi planned_end_date hoặc status thay đổi.
     */
    @Column(name = "alert_status", nullable = false, length = 10)
    private String alertStatus = "GREEN";

    // ─── Hoa hồng công đoạn ──────────────────────────────────────────────────

    /** Tỷ lệ hoa hồng áp dụng cho công đoạn này */
    @Column(name = "commission_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    /** Số tiền hoa hồng cho thợ (có thể override thủ công) */
    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    /** True = người dùng đã override thủ công */
    @Column(name = "is_commission_override", nullable = false)
    private Boolean isCommissionOverride = false;

    @Column(name = "commission_override_reason", columnDefinition = "TEXT")
    private String commissionOverrideReason;

    // ─── Meta ─────────────────────────────────────────────────────────────────

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    /** Người đánh dấu hoàn thành */
    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
