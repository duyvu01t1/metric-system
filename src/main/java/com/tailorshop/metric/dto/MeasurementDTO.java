package com.tailorshop.metric.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Measurement Data Transfer Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementDTO {

    private Long id;
    
    @NotNull(message = "Mã đơn hàng là bắt buộc")
    private Long tailoringOrderId;
    
    private String orderCode;
    
    private Long measurementTemplateId;

    private Long fieldId;

    private String fieldName;
    
    @NotNull(message = "Giá trị đo lường là bắt buộc")
    @DecimalMin(value = "0", message = "Giá trị phải >= 0")
    private BigDecimal value;
    private String unit;
    private String notes;
    private LocalDateTime measuredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
