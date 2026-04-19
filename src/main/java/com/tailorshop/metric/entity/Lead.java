package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lead Entity — Bản ghi tiếp nhận khách hàng tiềm năng từ các kênh
 *
 * Vòng đời: NEW → CONTACTED → QUALIFIED → NEGOTIATING → CONVERTED / LOST
 */
@Entity
@Table(name = "leads", indexes = {
    @Index(name = "idx_leads_code",           columnList = "lead_code"),
    @Index(name = "idx_leads_channel",        columnList = "channel_id"),
    @Index(name = "idx_leads_status",         columnList = "status"),
    @Index(name = "idx_leads_assigned_staff", columnList = "assigned_staff_id"),
    @Index(name = "idx_leads_phone",          columnList = "phone"),
    @Index(name = "idx_leads_email",          columnList = "email"),
    @Index(name = "idx_leads_created_at",     columnList = "created_at"),
    @Index(name = "idx_leads_followup_at",    columnList = "followup_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã lead tự sinh: L-YYYYMMDD-XXXX
     */
    @Column(name = "lead_code", unique = true, nullable = false, length = 50)
    private String leadCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    /**
     * SĐT — tự động bắt từ nguồn tin nhắn nếu có
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Email — tự động bắt từ nguồn tin nhắn nếu có
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Nội dung tin nhắn / hội thoại gốc từ kênh
     */
    @Column(name = "source_message", columnDefinition = "TEXT")
    private String sourceMessage;

    /**
     * Loại nhu cầu: SUIT, SHIRT, PANTS, DRESS, WEDDING, OTHER
     */
    @Column(name = "need_type", length = 50)
    private String needType;

    @Column(name = "need_description", columnDefinition = "TEXT")
    private String needDescription;

    @Column(name = "estimated_budget", precision = 15, scale = 2)
    private BigDecimal estimatedBudget;

    /**
     * Trạng thái lead: NEW | CONTACTED | QUALIFIED | NEGOTIATING | CONVERTED | LOST
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status = "NEW";

    @Column(name = "lost_reason", columnDefinition = "TEXT")
    private String lostReason;

    /**
     * Nhân viên phụ trách lead này (FK -> users)
     */
    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    /**
     * Sau khi chốt đơn, tham chiếu tới Customer được tạo
     */
    @Column(name = "converted_customer_id")
    private Long convertedCustomerId;

    /**
     * Sau khi chốt đơn, tham chiếu tới TailoringOrder được tạo
     */
    @Column(name = "converted_order_id")
    private Long convertedOrderId;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    /**
     * Thời gian hẹn liên lạc lại
     */
    @Column(name = "followup_at")
    private LocalDateTime followupAt;

    @Column(name = "last_contacted_at")
    private LocalDateTime lastContactedAt;

    @Column(name = "contact_count", nullable = false)
    private Integer contactCount = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Tags phân loại, ngăn cách bằng dấu phẩy: "VIP,URGENT,WEDDING"
     */
    @Column(name = "tags", length = 500)
    private String tags;

    /**
     * TRUE: khách đã từng mua trước đây — ưu tiên gán về nhân viên cũ
     */
    @Column(name = "is_returning_customer", nullable = false)
    private Boolean isReturningCustomer = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;
}
