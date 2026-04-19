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
 * Expense Entity — Chi phí vận hành (Phân hệ 7.4)
 *
 * category: RENT | UTILITIES | MATERIALS | SALARY | MARKETING | EQUIPMENT | MAINTENANCE | OTHER
 * status:   PENDING | APPROVED | REJECTED
 */
@Entity
@Table(name = "expenses", indexes = {
    @Index(name = "idx_expenses_code",     columnList = "expense_code"),
    @Index(name = "idx_expenses_category", columnList = "category"),
    @Index(name = "idx_expenses_date",     columnList = "expense_date"),
    @Index(name = "idx_expenses_period",   columnList = "period_year,period_month"),
    @Index(name = "idx_expenses_status",   columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã chi phí: EXP-YYYYMM-XXXX */
    @Column(name = "expense_code", unique = true, nullable = false, length = 50)
    private String expenseCode;

    /**
     * Danh mục:
     * RENT | UTILITIES | MATERIALS | SALARY | MARKETING | EQUIPMENT | MAINTENANCE | OTHER
     */
    @Column(name = "category", nullable = false, length = 50)
    private String category = "OTHER";

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    /** Tháng báo cáo (1-12), tính từ expenseDate nếu null */
    @Column(name = "period_month")
    private Short periodMonth;

    /** Năm báo cáo, tính từ expenseDate nếu null */
    @Column(name = "period_year")
    private Short periodYear;

    /** Chi phí lặp lại hàng tháng */
    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;

    /** Người phê duyệt */
    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * Trạng thái: PENDING | APPROVED | REJECTED
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    /** URL chứng từ / hóa đơn */
    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

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
