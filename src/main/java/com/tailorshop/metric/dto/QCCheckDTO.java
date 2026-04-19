package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QCCheck DTO — phiếu kiểm tra chất lượng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QCCheckDTO {

    private Long id;
    private Long orderId;
    private String orderCode;           // populated on read
    private String customerName;        // populated on read

    private String qcNumber;
    private String status;              // PENDING | IN_PROGRESS | PASSED | FAILED
    private String statusLabel;         // populated on read

    private String overallResult;       // PASS | FAIL | null
    private String overallResultLabel;  // populated on read

    private Long checkedBy;
    private String checkedByName;       // populated on read
    private LocalDateTime checkedAt;

    private Long approvedBy;
    private String approvedByName;      // populated on read
    private LocalDateTime approvedAt;

    private Integer checkRound;
    private String overallNotes;
    private String internalNotes;

    // Thống kê items (populated on read)
    private Integer totalItems;
    private Integer passCount;
    private Integer failCount;
    private Integer naCount;

    private List<QCItemDTO> items;      // populated khi load chi tiết

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
