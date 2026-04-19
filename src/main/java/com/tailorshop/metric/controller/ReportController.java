package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.ReportDTO;
import com.tailorshop.metric.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Report Controller
 * REST API endpoints for report generation
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "Report generation endpoints")
public class ReportController {

    private final ReportService reportService;

    /**
     * Get revenue report for date range
     * @param startDate Start date (yyyy-MM-dd)
     * @param endDate End date (yyyy-MM-dd)
     * @return Revenue report
     */
    @GetMapping("/revenue")
    @Operation(summary = "Get revenue report")
    public ResponseEntity<ApiResponse<ReportDTO>> getRevenueReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("Getting revenue report from {} to {}", startDate, endDate);
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        ReportDTO report = reportService.getRevenueReport(start, end);
        return ResponseEntity.ok(ApiResponse.success("Revenue report retrieved successfully", report));
    }

    /**
     * Get customer report
     * @return Customer report
     */
    @GetMapping("/customers")
    @Operation(summary = "Get customer report")
    public ResponseEntity<ApiResponse<ReportDTO>> getCustomerReport() {
        log.info("Getting customer report");
        
        ReportDTO report = reportService.getCustomerReport();
        return ResponseEntity.ok(ApiResponse.success("Customer report retrieved successfully", report));
    }

    /**
     * Get orders report
     * @return Orders report
     */
    @GetMapping("/orders")
    @Operation(summary = "Get orders report")
    public ResponseEntity<ApiResponse<ReportDTO>> getOrdersReport() {
        log.info("Getting orders report");
        
        ReportDTO report = reportService.getOrdersReport();
        return ResponseEntity.ok(ApiResponse.success("Orders report retrieved successfully", report));
    }

    /**
     * Get dashboard summary
     * @return Dashboard summary report
     */
    @GetMapping("/dashboard-summary")
    @Operation(summary = "Get dashboard summary")
    public ResponseEntity<ApiResponse<ReportDTO>> getDashboardSummary() {
        log.info("Getting dashboard summary");
        
        ReportDTO report = reportService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary retrieved successfully", report));
    }
}
