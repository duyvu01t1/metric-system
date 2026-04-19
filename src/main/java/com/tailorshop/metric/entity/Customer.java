package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer Entity
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customers_customer_code", columnList = "customer_code"),
    @Index(name = "idx_customers_email", columnList = "email"),
    @Index(name = "idx_customers_phone", columnList = "phone"),
    @Index(name = "idx_customers_is_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String customerCode;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;

    @Column(length = 50)
    private String identificationNumber;

    @Column(length = 50)
    private String identificationType; // ID_CARD, PASSPORT, etc.

    @Column
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender; // MALE, FEMALE, OTHER

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Boolean isActive = true;

    // ─── CRM fields (Phân hệ 2) ──────────────────────────────────────────────

    /** FK -> staff.id — nhân viên phụ trách khách hàng này */
    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    /** Chi phí có khách (Customer Acquisition Cost) */
    @Column(name = "cac", precision = 15, scale = 2)
    private BigDecimal cac = BigDecimal.ZERO;

    /** Số lần tương tác (cache, tăng khi có interaction mới) */
    @Column(name = "interaction_count", nullable = false)
    private Integer interactionCount = 0;

    /** FK -> channels.id — kênh mà khách tiếp cận lần đầu */
    @Column(name = "source_channel_id")
    private Long sourceChannelId;

    /** Thời điểm tương tác gần nhất */
    @Column(name = "last_interaction_at")
    private LocalDateTime lastInteractionAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private Long createdBy;

    @Column
    private Long updatedBy;

}
