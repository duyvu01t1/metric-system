package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * StaffCommission DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffCommissionDTO {

    private Long id;
    private Long orderId;
    private String orderCode;           // populated on read
    private Long staffId;
    private String staffName;           // populated on read
    private String staffRoleType;       // PRIMARY | SECONDARY
    private BigDecimal commissionRate;
    private BigDecimal commissionBase;
    private BigDecimal commissionAmount;
    private Boolean isManualOverride;
    private String overrideReason;
    private Boolean isPaid;
    private LocalDateTime paidAt;

    // ─── Phân hệ 7 fields ─────────────────────────────────────────────────
    private Long productionStageId;
    private String productionStageName;     // populated on read
    private Short periodMonth;
    private Short periodYear;
    private String notes;
    private String roleTypeLabel;           // populated on read
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
