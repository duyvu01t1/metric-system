package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * LeadAssignment Entity — Ghi vết phân công Lead cho nhân viên + approval workflow
 *
 * assignmentType: AUTO_ROUND_ROBIN | AUTO_PERFORMANCE | MANUAL
 * approvalStatus: APPROVED | PENDING | REJECTED
 *
 * Khi nhân viên có performanceScore thấp hơn ngưỡng cấu hình,
 * approvalStatus = PENDING; manager cần vào duyệt.
 */
@Entity
@Table(name = "lead_assignments", indexes = {
    @Index(name = "idx_lead_assign_lead_id",    columnList = "lead_id"),
    @Index(name = "idx_lead_assign_staff_id",   columnList = "staff_id"),
    @Index(name = "idx_lead_assign_status",     columnList = "approval_status"),
    @Index(name = "idx_lead_assign_is_current", columnList = "is_current")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_id", nullable = false)
    private Long leadId;

    /** Nhân viên được phân */
    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    /** Người thực hiện phân (null = hệ thống tự động) */
    @Column(name = "assigned_by")
    private Long assignedBy;

    /**
     * Kiểu phân: AUTO_ROUND_ROBIN | AUTO_PERFORMANCE | MANUAL
     */
    @Column(name = "assignment_type", nullable = false, length = 30)
    private String assignmentType = "AUTO_ROUND_ROBIN";

    /**
     * Trạng thái duyệt: APPROVED | PENDING | REJECTED
     */
    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus = "APPROVED";

    /** Manager đã duyệt / từ chối */
    @Column(name = "approved_by_manager_id")
    private Long approvedByManagerId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Chỉ 1 assignment active mỗi lead tại 1 thời điểm */
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
