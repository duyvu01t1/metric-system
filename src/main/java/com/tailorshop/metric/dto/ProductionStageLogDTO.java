package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ProductionStageLog DTO — nhật ký thay đổi công đoạn
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionStageLogDTO {

    private Long id;
    private Long stageId;
    private Long orderId;
    private String orderCode;           // populated on read
    private String changeType;          // STATUS_CHANGED | WORKER_ASSIGNED | ...
    private String changeTypeLabel;     // populated on read (tiếng Việt)
    private String oldValue;
    private String newValue;
    private String changeNote;
    private Long changedBy;
    private String changedByName;       // populated on read
    private LocalDateTime changedAt;
}
