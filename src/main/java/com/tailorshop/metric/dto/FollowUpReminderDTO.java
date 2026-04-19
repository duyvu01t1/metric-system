package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FollowUpReminderDTO — Phân hệ 6 (Chăm sóc Sau Bán Hàng)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpReminderDTO {

    private Long id;

    // ─── Order & Customer info ────────────────────────────────────────────
    private Long orderId;
    private String orderCode;           // populated on read
    private String orderType;           // populated on read
    private Long customerId;
    private String customerName;        // populated on read
    private String customerPhone;       // populated on read

    // ─── Assignment ───────────────────────────────────────────────────────
    private Long assignedStaffId;
    private String assignedStaffName;   // populated on read

    // ─── Scheduling ──────────────────────────────────────────────────────
    private LocalDate reminderDate;
    private String reminderType;        // DAY_3 | DAY_7 | DAY_10 | CUSTOM
    private String reminderTypeLabel;   // populated on read (e.g. "3 ngày sau giao")

    // ─── Status ───────────────────────────────────────────────────────────
    private String status;              // PENDING | DONE | SKIPPED | CANCELLED
    private String statusLabel;         // populated on read
    private String priority;            // LOW | MEDIUM | HIGH
    private String priorityLabel;       // populated on read

    // ─── Content ──────────────────────────────────────────────────────────
    private String careNotes;

    // ─── Completion ───────────────────────────────────────────────────────
    private LocalDateTime completedAt;
    private Long completedBy;
    private String completedByName;     // populated on read
    private String skipReason;

    // ─── Statistics ───────────────────────────────────────────────────────
    private Integer contactCount;
    private Integer customerRating;     // 1-5

    // ─── Computed on read ─────────────────────────────────────────────────
    private Boolean isOverdue;          // reminderDate < today AND status = PENDING
    private Long daysOverdue;           // bao nhiêu ngày quá hạn
    private Long daysUntilDue;          // bao nhiêu ngày nữa đến hạn

    // ─── Logs (optional, loaded on detail) ───────────────────────────────
    private List<FollowUpLogDTO> logs;

    // ─── Audit ────────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
}
