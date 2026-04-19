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
 * Quotation Entity — Báo giá cho khách hàng
 *
 * Luồng: DRAFT → SENT → ACCEPTED → CONVERTED (tạo TailoringOrder)
 *                     ↘ REJECTED
 *
 * Sau khi ACCEPTED, tạo TailoringOrder và gán quotationId vào đơn hàng.
 */
@Entity
@Table(name = "quotations", indexes = {
    @Index(name = "idx_quotations_code",        columnList = "quotation_code"),
    @Index(name = "idx_quotations_order_id",    columnList = "order_id"),
    @Index(name = "idx_quotations_customer_id", columnList = "customer_id"),
    @Index(name = "idx_quotations_status",      columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã báo giá: Q-YYYYMMDD-XXXX */
    @Column(name = "quotation_code", unique = true, nullable = false, length = 50)
    private String quotationCode;

    /** Liên kết đơn hàng sau khi chuyển đổi */
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "order_type", nullable = false, length = 50)
    private String orderType;

    @Column(name = "fabric_material", length = 500)
    private String fabricMaterial;

    @Column(name = "fabric_color", length = 100)
    private String fabricColor;

    @Column(name = "accessories", columnDefinition = "TEXT")
    private String accessories;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "fabric_cost", precision = 15, scale = 2)
    private BigDecimal fabricCost = BigDecimal.ZERO;

    @Column(name = "accessories_cost", precision = 15, scale = 2)
    private BigDecimal accessoriesCost = BigDecimal.ZERO;

    @Column(name = "labor_cost", precision = 15, scale = 2)
    private BigDecimal laborCost = BigDecimal.ZERO;

    /** Tổng trước giảm giá */
    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    /** FK -> discount_codes.id */
    @Column(name = "discount_code_id")
    private Long discountCodeId;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Tổng sau giảm giá */
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    /** FK -> channels.id — kênh nguồn */
    @Column(name = "source_channel_id")
    private Long sourceChannelId;

    /**
     * Trạng thái: DRAFT | SENT | ACCEPTED | REJECTED | CONVERTED
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "valid_until")
    private LocalDate validUntil;

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
