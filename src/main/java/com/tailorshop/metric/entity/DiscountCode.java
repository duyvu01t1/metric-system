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
 * DiscountCode Entity — Mã giảm giá liên kết đối tác
 *
 * discountType: PERCENT | FIXED
 * Khi khách dùng mã này: totalAmount giảm, đồng thời ghi nhận hoa hồng cho affiliate.
 */
@Entity
@Table(name = "discount_codes", indexes = {
    @Index(name = "idx_discount_code",        columnList = "code"),
    @Index(name = "idx_discount_affiliate",   columnList = "affiliate_id"),
    @Index(name = "idx_discount_is_active",   columnList = "is_active"),
    @Index(name = "idx_discount_valid_until", columnList = "valid_until")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    /** FK -> affiliates.id (nullable: mã không thuộc đối tác nào cũng OK) */
    @Column(name = "affiliate_id")
    private Long affiliateId;

    /**
     * Loại giảm giá: PERCENT | FIXED
     * PERCENT: giảm x% tổng đơn (có thể giới hạn maxDiscountAmount)
     * FIXED: giảm trực tiếp số tiền cố định
     */
    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType = "PERCENT";

    /** Giá trị giảm: % hoặc VNĐ tùy discountType */
    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    /** Giá trị đơn tối thiểu để áp mã */
    @Column(name = "min_order_value", precision = 15, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    /** Giới hạn số tiền giảm tối đa (chỉ áp dụng khi discountType = PERCENT) */
    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    private BigDecimal maxDiscountAmount;

    /** Số lần sử dụng tối đa (null = không giới hạn) */
    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;
}
