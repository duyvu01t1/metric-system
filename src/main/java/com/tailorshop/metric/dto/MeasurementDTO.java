package com.tailorshop.metric.dto;

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
    private Long tailoringOrderId;
    private String orderCode;
    private Long measurementTemplateId;
    private Long fieldId;
    private String fieldName;
    private BigDecimal value;
    private String unit;
    private String notes;
    private LocalDateTime measuredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
