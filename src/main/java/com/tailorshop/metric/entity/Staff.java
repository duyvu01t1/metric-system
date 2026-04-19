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
 * Staff Entity — Nhân viên (SALE / STAFF / MANAGER)
 *
 * Mỗi nhân viên liên kết với 1 tài khoản User.
 * performanceScore do manager đặt (0-100), dùng để phân khách tự động.
 */
@Entity
@Table(name = "staff", indexes = {
    @Index(name = "idx_staff_code",       columnList = "staff_code"),
    @Index(name = "idx_staff_user_id",    columnList = "user_id"),
    @Index(name = "idx_staff_role",       columnList = "staff_role"),
    @Index(name = "idx_staff_is_active",  columnList = "is_active"),
    @Index(name = "idx_staff_perf_score", columnList = "performance_score")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã nhân viên: S-YYYYMMDD-XXXX */
    @Column(name = "staff_code", unique = true, nullable = false, length = 50)
    private String staffCode;

    /** FK -> users.id — tài khoản đăng nhập */
    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "department", length = 100)
    private String department;

    /**
     * Vai trò: SALE | STAFF | MANAGER
     */
    @Column(name = "staff_role", nullable = false, length = 20)
    private String staffRole = "SALE";

    /**
     * Điểm hiệu suất 0-100, do manager định nghĩa.
     * Nhân viên có điểm thấp (< ngưỡng cấu hình) cần manager phê duyệt khi phân khách.
     */
    @Column(name = "performance_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal performanceScore = BigDecimal.valueOf(50.00);

    /** Doanh số kế hoạch tháng */
    @Column(name = "monthly_target", precision = 15, scale = 2)
    private BigDecimal monthlyTarget = BigDecimal.ZERO;

    /** Tỷ lệ hoa hồng cơ bản (ví dụ: 0.05 = 5%) */
    @Column(name = "base_commission_rate", precision = 6, scale = 4)
    private BigDecimal baseCommissionRate = BigDecimal.valueOf(0.0500);

    /** Tổng số lead đã nhận */
    @Column(name = "total_leads", nullable = false)
    private Integer totalLeads = 0;

    /** Tổng số lead đã chốt thành đơn */
    @Column(name = "total_converted", nullable = false)
    private Integer totalConverted = 0;

    /** Tỷ lệ chuyển đổi = totalConverted / totalLeads * 100 */
    @Column(name = "conversion_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal conversionRate = BigDecimal.ZERO;

    /** Tổng doanh thu mang về */
    @Column(name = "total_revenue", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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

}
