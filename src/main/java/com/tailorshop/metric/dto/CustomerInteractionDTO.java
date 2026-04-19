package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CustomerInteraction Data Transfer Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerInteractionDTO {

    private Long id;
    private Long customerId;
    private Long staffId;
    private String staffName;           // populated on read
    private String interactionType;     // CALL | NOTE | MESSAGE | MEETING | EMAIL
    private String content;
    private String outcome;
    private LocalDateTime nextFollowupAt;
    private LocalDateTime createdAt;

}
