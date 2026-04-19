package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ProductionStage DTO — trả về đầy đủ thông tin công đoạn kèm tên hiển thị
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionStageDTO {

    private Long id;
    private Long orderId;
    private String orderCode;           // populated on read
    private String customerName;        // populated on read

    private String stageType;           // CUT | ASSEMBLE | FITTING | DELIVERY
    private String stageTypeName;       // Cắt vải | Ráp may | ... (populated on read)
    private Integer stageOrder;         // 1-4
    private String status;              // PENDING | IN_PROGRESS | COMPLETED | SKIPPED
    private String statusLabel;         // Chờ thực hiện | Đang thực hiện | ... (populated on read)

    // Nhân công
    private Long assignedWorkerId;
    private String workerName;          // populated on read
    private Long assignedSaleId;
    private String saleName;            // populated on read

    // Lịch kế hoạch
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    // Cảnh báo
    private String alertStatus;         // GREEN | YELLOW | RED
    private String alertColor;          // #66bb6a | #ffb74d | #ef5350 (populated on read)

    // Hoa hồng
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private Boolean isCommissionOverride;
    private String commissionOverrideReason;

    private String notes;

    private Long createdBy;
    private Long completedBy;
    private String completedByName;     // populated on read
    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
