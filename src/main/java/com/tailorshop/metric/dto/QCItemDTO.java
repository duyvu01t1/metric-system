package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * QCItem DTO — từng tiêu chí trong phiếu QC
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QCItemDTO {

    private Long id;
    private Long qcCheckId;
    private Long orderId;

    private String itemCode;
    private String itemName;
    private String category;
    private String categoryLabel;   // populated on read
    private String description;

    private String result;          // PASS | FAIL | NA
    private String resultLabel;     // populated on read

    private String failNote;
    private String imageUrl;

    private Long checkedBy;
    private String checkedByName;   // populated on read
    private LocalDateTime checkedAt;

    private Integer sortOrder;
    private LocalDateTime createdAt;
}
