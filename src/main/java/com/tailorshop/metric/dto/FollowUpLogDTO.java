package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FollowUpLogDTO — Nhật ký từng lần liên hệ chăm sóc (Phân hệ 6 - Mục 6.4)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpLogDTO {

    private Long id;

    // ─── References ───────────────────────────────────────────────────────
    private Long reminderId;
    private Long orderId;
    private String orderCode;           // populated on read
    private Long customerId;
    private String customerName;        // populated on read
    private String customerPhone;       // populated on read

    // ─── Contact person ───────────────────────────────────────────────────
    private Long staffId;
    private String staffName;           // populated on read

    // ─── Contact details ──────────────────────────────────────────────────
    /**
     * Hình thức: CALL | MESSAGE | EMAIL | VISIT | ZALO | FACEBOOK
     */
    private String contactType;
    private String contactTypeLabel;    // populated on read

    /**
     * Kết quả: ANSWERED | NO_ANSWER | CALLBACK | LEFT_MESSAGE |
     *          SATISFIED | COMPLAINED | REPEAT_ORDER | NOT_INTERESTED
     */
    private String outcome;
    private String outcomeLabel;        // populated on read

    // ─── Content ──────────────────────────────────────────────────────────
    private String content;             // nội dung cuộc gọi / tin nhắn
    private String customerFeedback;    // ý kiến phản hồi của khách

    /** Đánh giá hài lòng của khách (1-5 sao) */
    private Integer customerRating;

    /** Hành động tiếp theo */
    private String nextAction;

    // ─── Timing ───────────────────────────────────────────────────────────
    private LocalDateTime contactedAt;
    private LocalDateTime createdAt;
}
