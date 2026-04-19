package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * CustomerInteraction Entity — Lịch sử tương tác với khách hàng
 *
 * interactionType: CALL | NOTE | MESSAGE | MEETING | EMAIL
 */
@Entity
@Table(name = "customer_interactions", indexes = {
    @Index(name = "idx_cust_inter_customer", columnList = "customer_id"),
    @Index(name = "idx_cust_inter_staff",    columnList = "staff_id"),
    @Index(name = "idx_cust_inter_created",  columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** Nhân viên thực hiện tương tác (null = hệ thống tự ghi) */
    @Column(name = "staff_id")
    private Long staffId;

    /**
     * Loại tương tác: CALL | NOTE | MESSAGE | MEETING | EMAIL
     */
    @Column(name = "interaction_type", nullable = false, length = 30)
    private String interactionType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** Kết quả / phản hồi của khách */
    @Column(name = "outcome", columnDefinition = "TEXT")
    private String outcome;

    /** Lịch follow-up tiếp theo */
    @Column(name = "next_followup_at")
    private LocalDateTime nextFollowupAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
