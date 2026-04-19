package com.tailorshop.metric.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Report Data Transfer Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportDTO {

    private Long id;
    private String reportType; // REVENUE, ORDERS, CUSTOMERS, PAYMENTS
    private String title;
    private String description;
    private LocalDate reportDate;
    private LocalDate startDate;
    private LocalDate endDate;

    // Revenue Report Fields
    private BigDecimal totalRevenue;
    private BigDecimal totalPayments;
    private BigDecimal pendingPayments;
    private Integer totalOrders;
    private Integer completedOrders;
    private Integer pendingOrders;

    // Customer Report Fields
    private Integer totalCustomers;
    private Integer activeCustomers;
    private Integer inactiveCustomers;
    private Double averageOrdersPerCustomer;

    // Payment Report Fields
    private Integer totalPayments_count;
    private BigDecimal averagePaymentAmount;
    private String mostUsedPaymentMethod;

    // Order Report Fields
    private Double averageDeliveryDays;
    private Integer onTimeDeliveries;
    private Integer lateDeliveries;
    private Double completionRate;

}
