package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DiscountCode DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCodeDTO {

    private Long id;
    private String code;
    private Long affiliateId;
    private String affiliateName;       // populated on read
    private String discountType;        // PERCENT | FIXED
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private Integer maxUses;
    private Integer usedCount;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Boolean isActive;
    private String description;
    private LocalDateTime createdAt;

    /** Số tiền giảm ước tính (chỉ dùng khi preview — không lưu DB) */
    private BigDecimal estimatedDiscount;
}
