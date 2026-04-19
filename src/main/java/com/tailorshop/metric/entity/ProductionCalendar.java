package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ProductionCalendar Entity — Sự kiện lịch sản xuất (4.7)
 *
 * Ba loại lịch:
 *   ORDER  = calendar tổng hợp của đơn hàng (tất cả công đoạn)
 *   WORKER = lịch riêng của từng thợ may (staff có role STAFF)
 *   SALE   = lịch riêng của từng nhân viên sale (staff có role SALE)
 *
 * Màu event_color tương ứng với alertStatus của ProductionStage:
 *   GREEN  → #66bb6a
 *   YELLOW → #ffb74d
 *   RED    → #ef5350
 *   Mặc định → #1976d2 (xanh dương primary)
 */
@Entity
@Table(name = "production_calendars", indexes = {
    @Index(name = "idx_prod_cal_order_id",    columnList = "order_id"),
    @Index(name = "idx_prod_cal_staff_id",    columnList = "staff_id"),
    @Index(name = "idx_prod_cal_type",        columnList = "calendar_type"),
    @Index(name = "idx_prod_cal_event_start", columnList = "event_start")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK -> tailoring_orders.id */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** FK -> production_stages.id — null nếu là sự kiện tổng của đơn hàng */
    @Column(name = "stage_id")
    private Long stageId;

    /** FK -> staff.id — null nếu calendar_type = ORDER */
    @Column(name = "staff_id")
    private Long staffId;

    /**
     * Loại lịch: ORDER | WORKER | SALE
     */
    @Column(name = "calendar_type", nullable = false, length = 20)
    private String calendarType = "ORDER";

    /** Tiêu đề sự kiện hiển thị trên calendar */
    @Column(name = "event_title", nullable = false, length = 300)
    private String eventTitle;

    @Column(name = "event_start", nullable = false)
    private LocalDateTime eventStart;

    @Column(name = "event_end", nullable = false)
    private LocalDateTime eventEnd;

    /** HEX color cho FullCalendar */
    @Column(name = "event_color", length = 20)
    private String eventColor = "#1976d2";

    /** True nếu sự kiện không có giờ cụ thể (hiển thị all-day) */
    @Column(name = "all_day", nullable = false)
    private Boolean allDay = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
