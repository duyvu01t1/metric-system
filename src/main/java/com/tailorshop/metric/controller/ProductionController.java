package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.ProductionCalendarDTO;
import com.tailorshop.metric.dto.ProductionStageDTO;
import com.tailorshop.metric.dto.ProductionStageLogDTO;
import com.tailorshop.metric.service.ProductionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ProductionController — REST API cho Phân hệ 4 (Sản xuất & Tiến độ)
 *
 * Tất cả endpoint thuộc prefix /api/  (khai báo trong SecurityConfig/RequestMapping gốc).
 *
 * Endpoint list:
 *   POST   /api/orders/{orderId}/production/init            — Khởi tạo 4 công đoạn
 *   GET    /api/orders/{orderId}/production/stages          — Danh sách công đoạn của đơn hàng
 *   GET    /api/production/stages/{id}                     — Chi tiết 1 công đoạn
 *   PUT    /api/production/stages/{id}/status              — Cập nhật trạng thái
 *   PUT    /api/production/stages/{id}/assign              — Gán thợ + sale
 *   PUT    /api/production/stages/{id}/schedule            — Đặt lịch kế hoạch
 *   PUT    /api/production/stages/{id}/commission          — Override hoa hồng
 *   POST   /api/production/stages/{id}/notes               — Thêm ghi chú
 *   GET    /api/production/stages/{id}/logs                — Nhật ký thay đổi
 *   GET    /api/production/calendar/order/{orderId}        — Calendar của 1 đơn hàng
 *   GET    /api/production/calendar/staff/{staffId}        — Calendar của 1 nhân viên
 *   GET    /api/production/calendar                        — Calendar tổng (date range)
 *   GET    /api/production/overview                        — Tổng quan cảnh báo màu
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductionController {

    private final ProductionService productionService;

    // ─── Khởi tạo / danh sách ─────────────────────────────────────────────────

    /**
     * Khởi tạo 4 công đoạn sản xuất cho đơn hàng.
     * Yêu cầu: depositStatus = CONFIRMED.
     *
     * Body (JSON): { "userId": 1 }
     */
    @PostMapping("/orders/{orderId}/production/init")
    public ResponseEntity<ApiResponse<List<ProductionStageDTO>>> initStages(
            @PathVariable Long orderId,
            @RequestBody Map<String, Long> body) {
        Long userId = body.getOrDefault("userId", 1L);
        List<ProductionStageDTO> stages = productionService.initializeStages(orderId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Đã khởi tạo " + stages.size() + " công đoạn sản xuất", stages));
    }

    @GetMapping("/orders/{orderId}/production/stages")
    public ResponseEntity<ApiResponse<List<ProductionStageDTO>>> getStagesByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("OK", productionService.getStagesByOrder(orderId)));
    }

    @GetMapping("/production/stages/{id}")
    public ResponseEntity<ApiResponse<ProductionStageDTO>> getStage(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", productionService.getStageById(id)));
    }

    // ─── Cập nhật trạng thái ─────────────────────────────────────────────────

    /**
     * Body (JSON): { "status": "IN_PROGRESS", "userId": 1, "note": "..." }
     */
    @PutMapping("/production/stages/{id}/status")
    public ResponseEntity<ApiResponse<ProductionStageDTO>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        Long userId   = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        String note   = (String) body.get("note");
        return ResponseEntity.ok(ApiResponse.success("Trạng thái đã được cập nhật",
            productionService.updateStageStatus(id, status, userId, note)));
    }

    // ─── Gán thợ may + sale ──────────────────────────────────────────────────

    /**
     * Body (JSON): { "workerId": 5, "saleId": 3, "userId": 1 }
     */
    @PutMapping("/production/stages/{id}/assign")
    public ResponseEntity<ApiResponse<ProductionStageDTO>> assignWorkerSale(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long workerId = body.containsKey("workerId") ? Long.valueOf(body.get("workerId").toString()) : null;
        Long saleId   = body.containsKey("saleId")   ? Long.valueOf(body.get("saleId").toString())   : null;
        Long userId   = body.containsKey("userId")   ? Long.valueOf(body.get("userId").toString())   : 1L;
        return ResponseEntity.ok(ApiResponse.success("Gán nhân công thành công",
            productionService.assignWorkerAndSale(id, workerId, saleId, userId)));
    }

    // ─── Đặt lịch kế hoạch ───────────────────────────────────────────────────

    /**
     * Body (JSON): { "plannedStartDate": "2026-05-01", "plannedEndDate": "2026-05-05", "userId": 1 }
     */
    @PutMapping("/production/stages/{id}/schedule")
    public ResponseEntity<ApiResponse<ProductionStageDTO>> scheduleStage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        LocalDate start = body.containsKey("plannedStartDate")
            ? LocalDate.parse((String) body.get("plannedStartDate")) : null;
        LocalDate end   = body.containsKey("plannedEndDate")
            ? LocalDate.parse((String) body.get("plannedEndDate")) : null;
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Lịch kế hoạch đã được cập nhật",
            productionService.scheduleStage(id, start, end, userId)));
    }

    // ─── Hoa hồng ─────────────────────────────────────────────────────────────

    /**
     * Body (JSON): { "rate": 0.05, "amount": 500000, "reason": "...", "userId": 1 }
     */
    @PutMapping("/production/stages/{id}/commission")
    public ResponseEntity<ApiResponse<ProductionStageDTO>> updateCommission(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        BigDecimal rate   = body.containsKey("rate")   ? new BigDecimal(body.get("rate").toString())   : null;
        BigDecimal amount = body.containsKey("amount") ? new BigDecimal(body.get("amount").toString()) : null;
        String reason     = (String) body.get("reason");
        Long userId       = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Hoa hồng đã được cập nhật",
            productionService.updateCommission(id, rate, amount, reason, userId)));
    }

    // ─── Ghi chú ──────────────────────────────────────────────────────────────

    /**
     * Body (JSON): { "note": "...", "userId": 1 }
     */
    @PostMapping("/production/stages/{id}/notes")
    public ResponseEntity<ApiResponse<ProductionStageDTO>> addNote(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String note = (String) body.get("note");
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Ghi chú đã được thêm",
            productionService.addNote(id, note, userId)));
    }

    // ─── Nhật ký thay đổi ────────────────────────────────────────────────────

    @GetMapping("/production/stages/{id}/logs")
    public ResponseEntity<ApiResponse<List<ProductionStageLogDTO>>> getStageLogs(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", productionService.getStageLogs(id)));
    }

    // ─── Calendar APIs (4.7 / 4.8) ───────────────────────────────────────────

    @GetMapping("/production/calendar/order/{orderId}")
    public ResponseEntity<ApiResponse<List<ProductionCalendarDTO>>> calendarByOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("OK", productionService.getCalendarByOrder(orderId)));
    }

    @GetMapping("/production/calendar/staff/{staffId}")
    public ResponseEntity<ApiResponse<List<ProductionCalendarDTO>>> calendarByStaff(
            @PathVariable Long staffId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        // Default: current month
        LocalDateTime from = start != null ? start : LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime to   = end   != null ? end   : LocalDateTime.now().plusMonths(1);
        return ResponseEntity.ok(ApiResponse.success("OK",
            productionService.getCalendarByStaff(staffId, from, to)));
    }

    @GetMapping("/production/calendar")
    public ResponseEntity<ApiResponse<List<ProductionCalendarDTO>>> calendarAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        LocalDateTime from = start != null ? start : LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime to   = end   != null ? end   : LocalDateTime.now().plusMonths(1);
        return ResponseEntity.ok(ApiResponse.success("OK",
            productionService.getCalendarAll(from, to)));
    }

    // ─── Overview dashboard ────────────────────────────────────────────────────

    @GetMapping("/production/overview")
    public ResponseEntity<ApiResponse<Map<String, Long>>> overview() {
        return ResponseEntity.ok(ApiResponse.success("OK", productionService.getAlertOverview()));
    }
}
