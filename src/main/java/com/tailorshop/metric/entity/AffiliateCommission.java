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
 * AffiliateCommission Entity — Hoa hồng Affiliate per đơn hàng
 *
 * Được tạo khi discount code của affiliate được áp dụng cho đơn hàng.
 * commissionAmount = order.totalPrice * affiliate.commissionRate
 */
@Entity
@Table(name = "affiliate_commissions", indexes = {
    @Index(name = "idx_aff_comm_order",     columnList = "order_id"),
    @Index(name = "idx_aff_comm_affiliate", columnList = "affiliate_id"),
    @Index(name = "idx_aff_comm_is_paid",   columnList = "is_paid")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AffiliateCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "affiliate_id", nullable = false)
    private Long affiliateId;

    /** FK -> discount_codes.id — code đã dùng */
    @Column(name = "discount_code_id")
    private Long discountCodeId;

    /** Số tiền giảm đã cho khách */
    @Column(name = "discount_given", precision = 15, scale = 2)
    private BigDecimal discountGiven = BigDecimal.ZERO;

    /** Tỷ lệ hoa hồng của affiliate */
    @Column(name = "commission_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal commissionRate;

    /** Số tiền hoa hồng phải trả affiliate */
    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionAmount;

    /** Đã thanh toán cho affiliate */
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
