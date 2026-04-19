package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ChatbotFaq DTO — Câu hỏi thường gặp cho chatbot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotFaqDTO {

    private Long id;
    private String question;
    private String answer;
    private String category;
    private String categoryLabel;
    private String keywords;
    private Integer hitCount;
    private Boolean isActive;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
