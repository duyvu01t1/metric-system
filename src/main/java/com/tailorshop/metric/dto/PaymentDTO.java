package com.tailorshop.metric.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Data Transfer Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentDTO {

    private Long id;
    private Long tailoringOrderId;
    private String orderCode;
    private String customerName;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionReference;
    private LocalDateTime paymentDate;
    private String notes;
    private LocalDateTime createdAt;
    private Long createdBy;

    // Additional fields for display
    private String status;

}
