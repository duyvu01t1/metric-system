package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Quotation DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationDTO {

    private Long id;
    private String quotationCode;
    private Long orderId;
    private Long customerId;
    private String customerName;        // populated on read
    private String orderType;
    private String fabricMaterial;
    private String fabricColor;
    private String accessories;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal fabricCost;
    private BigDecimal accessoriesCost;
    private BigDecimal laborCost;
    private BigDecimal subtotal;
    private Long discountCodeId;
    private String discountCodeValue;   // populated on read — mã text
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Long sourceChannelId;
    private String sourceChannelName;   // populated on read
    private String status;              // DRAFT | SENT | ACCEPTED | REJECTED | CONVERTED
    private LocalDate validUntil;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
}
