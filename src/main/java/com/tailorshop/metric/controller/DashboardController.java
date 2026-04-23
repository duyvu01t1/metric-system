package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

/**
 * Dashboard Controller
 * REST API endpoints for dashboard data
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Dashboard data endpoints")
public class DashboardController {

    private final ReportService reportService;

    /**
     * Get dashboard statistics
     * @return Dashboard statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<ApiResponse<?>> getStatistics() {
        try {
            log.info("Getting dashboard statistics");
            var report = reportService.getDashboardSummary();
            
            // Convert to expected format
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCustomers", report.getTotalCustomers());
            stats.put("activeOrders", report.getTotalOrders() - report.getCompletedOrders());
            stats.put("pendingPayments", report.getPendingPayments());
            stats.put("completedOrders", report.getCompletedOrders());
            
            return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", stats));
        } catch (Exception e) {
            log.error("Error getting dashboard statistics", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("STATISTICS_FETCH_FAILED", e.getMessage()));
        }
    }
}