package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lead DTO — Bản ghi tiếp nhận khách hàng tiềm năng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadDTO {

    private Long id;
    private String leadCode;

    // Channel info
    private Long channelId;
    private String channelCode;
    private String channelDisplayName;
    private String channelIconClass;

    // Contact info
    private String fullName;
    private String phone;
    private String email;
    private String sourceMessage;

    // Nhu cầu
    private String needType;
    private String needDescription;
    private BigDecimal estimatedBudget;

    // Trạng thái
    private String status;
    private String statusLabel;     // VN label tương ứng cho hiển thị
    private String lostReason;

    // Nhân viên phụ trách
    private Long assignedStaffId;
    private String assignedStaffName;

    // Chuyển đổi
    private Long convertedCustomerId;
    private Long convertedOrderId;
    private LocalDateTime convertedAt;

    // Follow-up
    private LocalDateTime followupAt;
    private LocalDateTime lastContactedAt;
    private Integer contactCount;

    private String notes;
    private String tags;
    private Boolean isReturningCustomer;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
}
