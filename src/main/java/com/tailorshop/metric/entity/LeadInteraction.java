package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * LeadInteraction Entity — Lịch sử tương tác với lead
 * Ghi lại mọi cuộc gọi, tin nhắn, gặp mặt liên quan đến một lead
 */
@Entity
@Table(name = "lead_interactions", indexes = {
    @Index(name = "idx_lead_interactions_lead", columnList = "lead_id"),
    @Index(name = "idx_lead_interactions_at",   columnList = "interacted_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    /**
     * Loại tương tác: NOTE | CALL | MESSAGE | MEETING | EMAIL
     */
    @Column(name = "interaction_type", nullable = false, length = 30)
    private String interactionType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Kết quả: INTERESTED, NOT_INTERESTED, FOLLOW_UP, CONVERTED, etc.
     */
    @Column(name = "outcome", length = 100)
    private String outcome;

    @Column(name = "interacted_by")
    private Long interactedBy;

    @Column(name = "interacted_at", nullable = false)
    private LocalDateTime interactedAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
