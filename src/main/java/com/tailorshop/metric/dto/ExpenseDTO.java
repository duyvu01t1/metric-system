package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Expense DTO — Chi phí vận hành (Phân hệ 7.4)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDTO {

    private Long id;
    private String expenseCode;

    /** RENT | UTILITIES | MATERIALS | SALARY | MARKETING | EQUIPMENT | MAINTENANCE | OTHER */
    private String category;
    private String categoryLabel;       // populated on read

    private String title;
    private String description;
    private BigDecimal amount;

    private LocalDate expenseDate;

    private Short periodMonth;
    private Short periodYear;

    private Boolean isRecurring;

    private Long approvedBy;
    private String approvedByName;      // populated on read
    private LocalDateTime approvedAt;

    /** PENDING | APPROVED | REJECTED */
    private String status;
    private String statusLabel;         // populated on read

    private String rejectReason;
    private String receiptUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
}
