package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ChatbotFaq Entity — Bộ câu hỏi thường gặp & gợi ý trả lời
 */
@Entity
@Table(name = "chatbot_faqs", indexes = {
    @Index(name = "idx_chatbot_faqs_active",   columnList = "is_active"),
    @Index(name = "idx_chatbot_faqs_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotFaq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    /**
     * Danh mục: GIA_CA, QUY_TRINH, SAN_PHAM, GIAO_HANG, KHAC
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * Từ khóa để matching, ngăn cách bởi dấu phẩy: "giá,vest,bộ vest,chi phí"
     */
    @Column(name = "keywords", length = 500)
    private String keywords;

    @Column(name = "hit_count", nullable = false)
    private Integer hitCount = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;
}
