package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Channel DTO — Kênh tiếp nhận
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelDTO {

    private Long id;
    private String channelCode;
    private String displayName;
    private String iconClass;
    private String webhookUrl;
    private String description;
    private Boolean isActive;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Thống kê số lead theo kênh — được tính toán khi cần
     */
    private Long totalLeads;
    private Long newLeads;
}
