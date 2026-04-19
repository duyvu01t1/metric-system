package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * StaffCommission Entity — Hoa hồng nhân viên per đơn hàng
 *
 * staffRoleType: PRIMARY | SECONDARY
 * Công thức mặc định:
 *   PRIMARY   = commissionBase * primaryRate   (ví dụ 5%)
 *   SECONDARY = commissionBase * secondaryRate (ví dụ 2%)
 * Có thể chỉnh sửa thủ công (isManualOverride = true).
 */
@Entity
@Table(name = "staff_commissions", indexes = {
    @Index(name = "idx_staff_comm_order",   columnList = "order_id"),
    @Index(name = "idx_staff_comm_staff",   columnList = "staff_id"),
    @Index(name = "idx_staff_comm_is_paid", columnList = "is_paid")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    /**
     * Vai trò trong đơn: PRIMARY | SECONDARY
     */
    @Column(name = "staff_role_type", nullable = false, length = 20)
    private String staffRoleType;

    /** Tỷ lệ hoa hồng áp dụng */
    @Column(name = "commission_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal commissionRate;

    /** Giá trị làm căn tính (thường = totalPrice đơn hàng) */
    @Column(name = "commission_base", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionBase;

    /** Số tiền hoa hồng thực tế */
    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionAmount;

    /** Đã bị override thủ công */
    @Column(name = "is_manual_override", nullable = false)
    private Boolean isManualOverride = false;

    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    /** Đã thanh toán hoa hồng cho nhân viên */
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // ─── Phân hệ 7 — denormalized & period fields ─────────────────────────────

    /** Tên nhân viên (denormalized) */
    @Column(name = "staff_name", length = 200)
    private String staffName;

    /** Mã đơn hàng (denormalized) */
    @Column(name = "order_code", length = 100)
    private String orderCode;

    /** FK -> production_stages.id — nếu override theo công đoạn */
    @Column(name = "production_stage_id")
    private Long productionStageId;

    /** Tháng kỳ lương (1-12) */
    @Column(name = "period_month")
    private Short periodMonth;

    /** Năm kỳ lương */
    @Column(name = "period_year")
    private Short periodYear;

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

    @Column(name = "updated_by")
    private Long updatedBy;
}
