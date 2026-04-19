package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ProductionCalendar DTO — sự kiện lịch sản xuất
 *
 * Các field title/start/end/color được thiết kế để dùng trực tiếp với FullCalendar.js v6.
 * extendedProps chứa metadata bổ sung để hiển thị tooltip/popover.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionCalendarDTO {

    private Long id;
    private Long orderId;
    private Long stageId;
    private Long staffId;
    private String calendarType;    // ORDER | WORKER | SALE

    // FullCalendar standard fields
    private String title;           // alias for eventTitle
    private LocalDateTime start;    // alias for eventStart
    private LocalDateTime end;      // alias for eventEnd
    private String color;           // alias for eventColor
    private Boolean allDay;

    // Extended props (metadata cho tooltip)
    private String orderCode;
    private String staffName;
    private String stageType;
    private String stageTypeName;
    private String stageStatus;
    private String alertStatus;
    private String notes;

    private LocalDateTime createdAt;
}
