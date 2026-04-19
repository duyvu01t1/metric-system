package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LeadAssignment Data Transfer Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadAssignmentDTO {

    private Long id;
    private Long leadId;
    private String leadCode;            // populated on read
    private String leadName;            // populated on read
    private Long staffId;
    private String staffName;           // populated on read
    private Long assignedBy;
    private String assignmentType;      // AUTO_ROUND_ROBIN | AUTO_PERFORMANCE | MANUAL
    private String approvalStatus;      // APPROVED | PENDING | REJECTED
    private Long approvedByManagerId;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private String notes;
    private Boolean isCurrent;
    private LocalDateTime createdAt;

}
