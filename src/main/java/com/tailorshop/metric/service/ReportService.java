package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.ReportDTO;
import com.tailorshop.metric.entity.TailoringOrder;
import com.tailorshop.metric.repository.CustomerRepository;
import com.tailorshop.metric.repository.PaymentRepository;
import com.tailorshop.metric.repository.TailoringOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report Service
 * Handles business logic for report generation
 */
@Service
@Slf4j
public class ReportService {

    @Autowired
    private TailoringOrderRepository tailoringOrderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Get revenue report for date range
     * @param startDate Start date
     * @param endDate End date
     * @return Revenue report
     */
    @Transactional(readOnly = true)
    public ReportDTO getRevenueReport(LocalDate startDate, LocalDate endDate) {
        log.info("Generating revenue report from {} to {}", startDate, endDate);

        // Get all orders in date range
        List<TailoringOrder> orders = tailoringOrderRepository.findAll();
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalPayments = BigDecimal.ZERO;
        int totalOrdersCount = 0;
        int completedOrdersCount = 0;
        int pendingOrdersCount = 0;

        for (TailoringOrder order : orders) {
            if (order.getOrderDate().isAfter(startDate) && 
                order.getOrderDate().isBefore(endDate.plusDays(1))) {
                
                totalOrdersCount++;
                totalRevenue = totalRevenue.add(order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO);
                
                if ("COMPLETED".equals(order.getStatus())) {
                    completedOrdersCount++;
                }
                if ("PENDING".equals(order.getStatus())) {
                    pendingOrdersCount++;
                }
            }
        }

        // Calculate total payments from payment records
        for (var payment : paymentRepository.findAll()) {
            if (payment.getPaymentDate().toLocalDate().isAfter(startDate) && 
                payment.getPaymentDate().toLocalDate().isBefore(endDate.plusDays(1))) {
                totalPayments = totalPayments.add(payment.getAmount());
            }
        }

        BigDecimal pendingPayments = totalRevenue.subtract(totalPayments);

        return ReportDTO.builder()
            .reportType("REVENUE")
            .title("Revenue Report")
            .description("Sales and revenue metrics")
            .reportDate(LocalDate.now())
            .startDate(startDate)
            .endDate(endDate)
            .totalRevenue(totalRevenue)
            .totalPayments(totalPayments)
            .pendingPayments(pendingPayments)
            .totalOrders(totalOrdersCount)
            .completedOrders(completedOrdersCount)
            .pendingOrders(pendingOrdersCount)
            .build();
    }

    /**
     * Get customer report
     * @return Customer report
     */
    @Transactional(readOnly = true)
    public ReportDTO getCustomerReport() {
        log.info("Generating customer report");

        long totalCustomers = customerRepository.count();
        long activeCustomers = customerRepository.findByIsActiveTrue(
            org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)
        ).getTotalElements();

        return ReportDTO.builder()
            .reportType("CUSTOMERS")
            .title("Customer Report")
            .description("Customer statistics and metrics")
            .reportDate(LocalDate.now())
            .totalCustomers((int) totalCustomers)
            .activeCustomers((int) activeCustomers)
            .inactiveCustomers((int) (totalCustomers - activeCustomers))
            .build();
    }

    /**
     * Get orders report
     * @return Orders report
     */
    @Transactional(readOnly = true)
    public ReportDTO getOrdersReport() {
        log.info("Generating orders report");

        List<TailoringOrder> orders = tailoringOrderRepository.findAll();
        
        int totalOrders = orders.size();
        int completedOrders = 0;
        int pendingOrders = 0;
        int cancelledOrders = 0;
        
        double completionRate = 0;
        if (totalOrders > 0) {
            for (TailoringOrder order : orders) {
                if ("COMPLETED".equals(order.getStatus())) {
                    completedOrders++;
                } else if ("PENDING".equals(order.getStatus())) {
                    pendingOrders++;
                } else if ("CANCELLED".equals(order.getStatus())) {
                    cancelledOrders++;
                }
            }
            completionRate = (double) completedOrders / totalOrders * 100;
        }

        return ReportDTO.builder()
            .reportType("ORDERS")
            .title("Orders Report")
            .description("Order statistics and metrics")
            .reportDate(LocalDate.now())
            .totalOrders(totalOrders)
            .completedOrders(completedOrders)
            .pendingOrders(pendingOrders)
            .completionRate(completionRate)
            .build();
    }

    /**
     * Get dashboard summary report
     * @return Dashboard report with key metrics
     */
    @Transactional(readOnly = true)
    public ReportDTO getDashboardSummary() {
        log.info("Generating dashboard summary report");

        // Revenue data
        List<TailoringOrder> orders = tailoringOrderRepository.findAll();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalPayments = BigDecimal.ZERO;
        int totalOrders = orders.size();
        int completedOrders = 0;

        for (TailoringOrder order : orders) {
            totalRevenue = totalRevenue.add(order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO);
            if ("COMPLETED".equals(order.getStatus())) {
                completedOrders++;
            }
        }

        // Payments data
        for (var payment : paymentRepository.findAll()) {
            totalPayments = totalPayments.add(payment.getAmount());
        }

        long totalCustomers = customerRepository.count();

        return ReportDTO.builder()
            .reportType("DASHBOARD")
            .title("Dashboard Summary")
            .description("Key metrics overview")
            .reportDate(LocalDate.now())
            .totalRevenue(totalRevenue)
            .totalPayments(totalPayments)
            .totalOrders(totalOrders)
            .completedOrders(completedOrders)
            .totalCustomers((int) totalCustomers)
            .build();
    }
}
