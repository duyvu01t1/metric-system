package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.service.AnalyticsExportService;
import com.tailorshop.metric.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * AnalyticsController — Phân hệ 8: Báo cáo & Đánh giá
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Advanced analytics and evaluation endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AnalyticsExportService analyticsExportService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get full analytics dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Analytics dashboard retrieved successfully",
                analyticsService.getFullAnalyticsDashboard(from, to)
        ));
    }

    @GetMapping("/overview")
    @Operation(summary = "Get analytics overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Analytics overview retrieved successfully",
                analyticsService.getOverview(from, to)
        ));
    }

    @GetMapping("/revenue-by-channel")
    @Operation(summary = "Get revenue by marketing channel")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevenueByChannel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Revenue by channel retrieved successfully",
                analyticsService.getRevenueByChannel(from, to)
        ));
    }

    @GetMapping("/revenue-by-staff")
    @Operation(summary = "Get revenue by staff")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevenueByStaff(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Revenue by staff retrieved successfully",
                analyticsService.getRevenueByStaff(from, to)
        ));
    }

    @GetMapping("/staff-performance")
    @Operation(summary = "Get staff performance analytics")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStaffPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Staff performance analytics retrieved successfully",
                analyticsService.getStaffPerformance(from, to)
        ));
    }

    @GetMapping("/lead-conversion")
    @Operation(summary = "Get lead conversion analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLeadConversion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lead conversion analytics retrieved successfully",
                analyticsService.getLeadConversionAnalytics(from, to)
        ));
    }

    @GetMapping("/marketing-effectiveness")
    @Operation(summary = "Get marketing effectiveness analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMarketingEffectiveness(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Marketing effectiveness analytics retrieved successfully",
                analyticsService.getMarketingEffectiveness(from, to)
        ));
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Export analytics as Excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] file = analyticsExportService.exportExcel(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @GetMapping("/export/pdf")
    @Operation(summary = "Export analytics as PDF")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] file = analyticsExportService.exportPdf(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }
}
