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
import java.util.ArrayList;
import java.util.List;

/**
 * Tailoring Order Entity
 */
@Entity
@Table(name = "tailoring_orders", indexes = {
    @Index(name = "idx_orders_order_code", columnList = "order_code"),
    @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_order_date", columnList = "order_date"),
    @Index(name = "idx_orders_promised_date", columnList = "promised_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TailoringOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SỐ BIÊN LẠI — auto-generated via sequence */
    @Column(name = "order_number", unique = true, nullable = false)
    private Long orderNumber;

    @Column(unique = true, nullable = false, length = 100)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private LocalDate orderDate;

    @Column
    private LocalDate promisedDate;

    @Column
    private LocalDate completedDate;

    @Column(nullable = false, length = 50)
    private String orderType; // SUIT, SHIRT, PANTS, DRESS, CUSTOM

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(length = 50, nullable = false)
    private String status = "PENDING"; // PENDING, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(length = 50, nullable = false)
    private String paymentStatus = "UNPAID"; // UNPAID, PARTIAL, PAID

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Boolean isArchived = false;

    // ─── Phân hệ 3 — Order & Quotation fields ────────────────────────────────

    /** Chất liệu vải (3.1) */
    @Column(name = "fabric_material", length = 500)
    private String fabricMaterial;

    /** Màu vải */
    @Column(name = "fabric_color", length = 100)
    private String fabricColor;

    /** Phụ liệu: nút, khóa, lót, v.v. (3.1) */
    @Column(name = "accessories", columnDefinition = "TEXT")
    private String accessories;

    /** FK -> channels.id — kênh nguồn khách (3.1) */
    @Column(name = "source_channel_id")
    private Long sourceChannelId;

    /** FK -> quotations.id — báo giá gốc (3.2) */
    @Column(name = "quotation_id")
    private Long quotationId;

    /** FK -> discount_codes.id — mã giảm giá đối tác (3.4) */
    @Column(name = "discount_code_id")
    private Long discountCodeId;

    /** Số tiền đã giảm (3.5) */
    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // ─── Đặt cọc (3.6) ────────────────────────────────────────────────────

    /** Số tiền đặt cọc (3.6) */
    @Column(name = "deposit_amount", precision = 15, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    /**
     * Trạng thái đặt cọc: NONE | PENDING | CONFIRMED (3.6)
     * Logic khoá sản xuất: chỉ cho phép tiến khi depositStatus = 'CONFIRMED' (3.7)
     */
    @Column(name = "deposit_status", length = 30)
    private String depositStatus = "NONE";

    /** Ngày đặt cọc (3.6) */
    @Column(name = "deposit_date")
    private LocalDate depositDate;

    /** Người xác nhận đặt cọc */
    @Column(name = "deposit_confirmed_by")
    private Long depositConfirmedBy;

    @Column(name = "deposit_confirmed_at")
    private LocalDateTime depositConfirmedAt;

    // ─── 2 nhân viên / đơn hàng (3.9) ────────────────────────────────────

    /** Nhân viên chính (người chốt đơn) */
    @Column(name = "primary_staff_id")
    private Long primaryStaffId;

    /** Nhân viên phụ */
    @Column(name = "secondary_staff_id")
    private Long secondaryStaffId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private Long createdBy;

    @Column
    private Long updatedBy;

    /** Danh sách sản phẩm trong đơn hàng */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    private List<OrderItem> items = new ArrayList<>();

}
