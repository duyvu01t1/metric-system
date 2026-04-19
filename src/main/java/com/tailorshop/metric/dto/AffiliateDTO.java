package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Affiliate DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliateDTO {

    private Long id;
    private String affiliateCode;
    private String companyName;
    private String contactName;
    private String phone;
    private String email;
    private BigDecimal commissionRate;
    private Integer totalOrders;
    private BigDecimal totalCommissionPaid;
    private Boolean isActive;
    private String notes;
    private LocalDateTime createdAt;
}
