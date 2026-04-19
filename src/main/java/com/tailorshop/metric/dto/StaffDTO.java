package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Staff Data Transfer Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffDTO {

    private Long id;
    private String staffCode;
    private Long userId;
    private String username;        // populated on read
    private String fullName;
    private String phone;
    private String department;
    private String staffRole;       // SALE | STAFF | MANAGER
    private BigDecimal performanceScore;
    private BigDecimal monthlyTarget;
    private BigDecimal baseCommissionRate;
    private Integer totalLeads;
    private Integer totalConverted;
    private BigDecimal conversionRate;
    private BigDecimal totalRevenue;
    private Boolean isActive;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
