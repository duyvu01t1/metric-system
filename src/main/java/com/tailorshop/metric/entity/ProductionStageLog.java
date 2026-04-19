package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ProductionStageLog Entity — Nhật ký thay đổi công đoạn sản xuất
 *
 * Ghi lại TOÀN BỘ sự kiện:
 *   STATUS_CHANGED     — chuyển PENDING → IN_PROGRESS → COMPLETED v.v.
 *   WORKER_ASSIGNED    — gán / đổi thợ may
 *   SALE_ASSIGNED      — gán / đổi nhân viên sale
 *   COMMISSION_UPDATED — cập nhật / override thủ công hoa hồng
 *   SCHEDULE_CHANGED   — thay đổi planned_start/end_date
 *   NOTE_ADDED         — thêm ghi chú
 */
@Entity
@Table(name = "production_stage_logs", indexes = {
    @Index(name = "idx_stage_log_stage_id",  columnList = "stage_id"),
    @Index(name = "idx_stage_log_order_id",  columnList = "order_id"),
    @Index(name = "idx_stage_log_changed_at",columnList = "changed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionStageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * Loại sự kiện:
     * STATUS_CHANGED | WORKER_ASSIGNED | SALE_ASSIGNED |
     * COMMISSION_UPDATED | SCHEDULE_CHANGED | NOTE_ADDED
     */
    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType;

    /** Giá trị trước khi thay đổi (dạng text hoặc JSON đơn giản) */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** Giá trị sau khi thay đổi */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /** Ghi chú / lý do thay đổi */
    @Column(name = "change_note", columnDefinition = "TEXT")
    private String changeNote;

    /** FK -> users.id — người thực hiện thay đổi */
    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();
}
