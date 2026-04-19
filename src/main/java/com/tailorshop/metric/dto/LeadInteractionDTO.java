package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LeadInteraction DTO — Tương tác với lead
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadInteractionDTO {

    private Long id;
    private Long leadId;

    /**
     * Loại tương tác: NOTE | CALL | MESSAGE | MEETING | EMAIL
     */
    private String interactionType;
    private String interactionTypeLabel;

    private String content;
    private String outcome;

    private Long interactedBy;
    private String interactedByName;
    private LocalDateTime interactedAt;
    private LocalDateTime createdAt;
}
