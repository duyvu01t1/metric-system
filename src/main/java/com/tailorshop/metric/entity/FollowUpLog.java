package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * FollowUpLog Entity — Nhật ký từng lần liên hệ chăm sóc (Phân hệ 6 - Mục 6.4)
 *
 * Mỗi lần nhân viên gọi / nhắn tin / ghé thăm khách đều ghi vào đây.
 * Nhiều log có thể thuộc 1 FollowUpReminder (ví dụ: gọi 3 lần mới liên lạc được).
 *
 * contactType: CALL | MESSAGE | EMAIL | VISIT | ZALO | FACEBOOK
 * outcome:     ANSWERED | NO_ANSWER | CALLBACK | LEFT_MESSAGE |
 *              SATISFIED | COMPLAINED | REPEAT_ORDER | NOT_INTERESTED
 */
@Entity
@Table(name = "follow_up_logs", indexes = {
    @Index(name = "idx_ful_reminder_id",  columnList = "reminder_id"),
    @Index(name = "idx_ful_order_id",     columnList = "order_id"),
    @Index(name = "idx_ful_customer_id",  columnList = "customer_id"),
    @Index(name = "idx_ful_staff_id",     columnList = "staff_id"),
    @Index(name = "idx_ful_contacted_at", columnList = "contacted_at"),
    @Index(name = "idx_ful_outcome",      columnList = "outcome")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK -> follow_up_reminders.id */
    @Column(name = "reminder_id", nullable = false)
    private Long reminderId;

    /** FK -> tailoring_orders.id (denormalized) */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** FK -> customers.id (denormalized) */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** FK -> users.id — nhân viên thực hiện liên hệ */
    @Column(name = "staff_id")
    private Long staffId;

    /**
     * Hình thức liên hệ:
     * CALL | MESSAGE | EMAIL | VISIT | ZALO | FACEBOOK
     */
    @Column(name = "contact_type", nullable = false, length = 20)
    private String contactType = "CALL";

    /**
     * Kết quả liên hệ:
     * ANSWERED       — Khách nghe máy / phản hồi
     * NO_ANSWER      — Không liên lạc được
     * CALLBACK       — Khách sẽ gọi lại
     * LEFT_MESSAGE   — Để lại tin nhắn / voicemail
     * SATISFIED      — Khách hài lòng, không có yêu cầu gì thêm
     * COMPLAINED     — Khách có khiếu nại / phàn nàn
     * REPEAT_ORDER   — Khách muốn đặt thêm đơn mới
     * NOT_INTERESTED — Khách không có nhu cầu
     */
    @Column(name = "outcome", nullable = false, length = 30)
    private String outcome = "ANSWERED";

    /** Nội dung chi tiết cuộc gọi / tin nhắn */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** Phản hồi / ý kiến của khách hàng */
    @Column(name = "customer_feedback", columnDefinition = "TEXT")
    private String customerFeedback;

    /**
     * Đánh giá sự hài lòng của khách (1-5 sao).
     * Chỉ điền khi outcome = SATISFIED hoặc COMPLAINED.
     */
    @Column(name = "customer_rating")
    private Integer customerRating;

    /** Hành động tiếp theo nhân viên cần thực hiện */
    @Column(name = "next_action", columnDefinition = "TEXT")
    private String nextAction;

    /** Thời điểm thực hiện liên hệ (nhân viên có thể điền lại nếu ghi sau) */
    @Column(name = "contacted_at", nullable = false)
    private LocalDateTime contactedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
