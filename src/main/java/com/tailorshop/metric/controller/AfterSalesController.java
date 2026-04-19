package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.FollowUpLogDTO;
import com.tailorshop.metric.dto.FollowUpReminderDTO;
import com.tailorshop.metric.service.AfterSalesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AfterSalesController — REST API cho Phân hệ 6 (Chăm sóc Sau Bán Hàng)
 *
 * ─── Reminder (6.1 / 6.2 / 6.3) ─────────────────────────────────────────
 *   POST   /api/orders/{orderId}/after-sales/auto     — Kích hoạt tạo reminder tự động
 *   POST   /api/after-sales/reminders                 — Tạo reminder thủ công (CUSTOM)
 *   PUT    /api/after-sales/reminders/{id}            — Cập nhật reminder
 *   PUT    /api/after-sales/reminders/{id}/done       — Đánh dấu DONE + log cuối
 *   PUT    /api/after-sales/reminders/{id}/skip       — Bỏ qua / Hủy
 *   GET    /api/after-sales/reminders/today           — Việc cần làm hôm nay
 *   GET    /api/after-sales/reminders/overdue         — Danh sách quá hạn
 *   GET    /api/after-sales/reminders/upcoming        — Sắp đến hạn (7 ngày tới)
 *   GET    /api/after-sales/reminders                 — Tất cả (phân trang, filter status)
 *   GET    /api/after-sales/reminders/{id}            — Chi tiết 1 reminder + logs
 *   GET    /api/orders/{orderId}/after-sales          — Reminders của 1 đơn hàng
 *
 * ─── Contact Log (6.4) ────────────────────────────────────────────────────
 *   POST   /api/after-sales/reminders/{id}/logs       — Ghi nhận 1 lần liên hệ
 *   GET    /api/after-sales/reminders/{id}/logs       — Logs của 1 reminder
 *   GET    /api/after-sales/logs                      — Tất cả logs (phân trang)
 *
 * ─── Overview ─────────────────────────────────────────────────────────────
 *   GET    /api/after-sales/overview                  — Thống kê tổng quan
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AfterSalesController {

    private final AfterSalesService afterSalesService;

    // ════════════════════════════════════════════════════════════════════════
    // REMINDER
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Kích hoạt tạo reminder tự động cho 1 đơn hàng vừa COMPLETED.
     * Body: { "userId": 1 }
     */
    @PostMapping("/orders/{orderId}/after-sales/auto")
    public ResponseEntity<ApiResponse<List<FollowUpReminderDTO>>> autoCreateReminders(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> body) {
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        List<FollowUpReminderDTO> result = afterSalesService.autoCreateForOrderById(orderId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã tạo " + result.size() + " reminder chăm sóc sau bán hàng", result));
    }

    /**
     * Tạo reminder thủ công.
     * Body: FollowUpReminderDTO (orderId, reminderDate, priority, careNotes, assignedStaffId)
     */
    @PostMapping("/after-sales/reminders")
    public ResponseEntity<ApiResponse<FollowUpReminderDTO>> createManualReminder(
            @RequestBody FollowUpReminderDTO dto,
            @RequestParam(defaultValue = "1") Long userId) {
        FollowUpReminderDTO result = afterSalesService.createManualReminder(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã tạo reminder chăm sóc", result));
    }

    /**
     * Cập nhật reminder (ngày, nhân viên, ghi chú).
     * Body: FollowUpReminderDTO (các field muốn cập nhật)
     */
    @PutMapping("/after-sales/reminders/{id}")
    public ResponseEntity<ApiResponse<FollowUpReminderDTO>> updateReminder(
            @PathVariable Long id,
            @RequestBody FollowUpReminderDTO dto,
            @RequestParam(defaultValue = "1") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật reminder",
                afterSalesService.updateReminder(id, dto, userId)));
    }

    /**
     * Đánh dấu DONE + ghi log liên hệ cuối cùng (tùy chọn).
     * Body: FollowUpLogDTO (contactType, outcome, content, customerFeedback, customerRating, nextAction)
     *       + trường userId ở top level
     * Nếu không cần log → body = { "userId": 1 }
     */
    @PutMapping("/after-sales/reminders/{id}/done")
    public ResponseEntity<ApiResponse<FollowUpReminderDTO>> markDone(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;

        // Parse log DTO nếu có
        FollowUpLogDTO logDTO = null;
        if (body.containsKey("contactType")) {
            logDTO = FollowUpLogDTO.builder()
                .contactType((String) body.get("contactType"))
                .outcome((String) body.get("outcome"))
                .content((String) body.get("content"))
                .customerFeedback((String) body.get("customerFeedback"))
                .customerRating(body.containsKey("customerRating")
                    ? Integer.valueOf(body.get("customerRating").toString()) : null)
                .nextAction((String) body.get("nextAction"))
                .build();
        }
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu hoàn thành",
                afterSalesService.markDone(id, logDTO, userId)));
    }

    /**
     * Bỏ qua / Hủy reminder.
     * Body: { "reason": "...", "status": "SKIPPED|CANCELLED", "userId": 1 }
     */
    @PutMapping("/after-sales/reminders/{id}/skip")
    public ResponseEntity<ApiResponse<FollowUpReminderDTO>> skipReminder(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String reason    = (String) body.getOrDefault("reason", null);
        String newStatus = (String) body.getOrDefault("status", "SKIPPED");
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Đã bỏ qua reminder",
                afterSalesService.skipReminder(id, reason, newStatus, userId)));
    }

    // ─── Read ──────────────────────────────────────────────────────────────

    @GetMapping("/after-sales/reminders/today")
    public ResponseEntity<ApiResponse<List<FollowUpReminderDTO>>> getTodayReminders() {
        return ResponseEntity.ok(ApiResponse.success("OK", afterSalesService.getTodayReminders()));
    }

    @GetMapping("/after-sales/reminders/overdue")
    public ResponseEntity<ApiResponse<List<FollowUpReminderDTO>>> getOverdueReminders() {
        return ResponseEntity.ok(ApiResponse.success("OK", afterSalesService.getOverdueReminders()));
    }

    @GetMapping("/after-sales/reminders/upcoming")
    public ResponseEntity<ApiResponse<List<FollowUpReminderDTO>>> getUpcomingReminders(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.success("OK", afterSalesService.getUpcomingReminders(days)));
    }

    @GetMapping("/after-sales/reminders")
    public ResponseEntity<ApiResponse<Page<FollowUpReminderDTO>>> listReminders(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                afterSalesService.listReminders(status, pageable)));
    }

    @GetMapping("/after-sales/reminders/{id}")
    public ResponseEntity<ApiResponse<FollowUpReminderDTO>> getReminderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", afterSalesService.getReminderById(id)));
    }

    @GetMapping("/orders/{orderId}/after-sales")
    public ResponseEntity<ApiResponse<List<FollowUpReminderDTO>>> getRemindersByOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                afterSalesService.getRemindersByOrder(orderId)));
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONTACT LOGS (6.4)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Ghi nhận 1 lần liên hệ chăm sóc (6.4).
     * Body: FollowUpLogDTO (contactType, outcome, content, customerFeedback,
     *        customerRating, nextAction, staffId, contactedAt)
     */
    @PostMapping("/after-sales/reminders/{id}/logs")
    public ResponseEntity<ApiResponse<FollowUpLogDTO>> logContact(
            @PathVariable Long id,
            @RequestBody FollowUpLogDTO dto,
            @RequestParam(defaultValue = "1") Long userId) {
        FollowUpLogDTO result = afterSalesService.logContact(id, dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã ghi nhận liên hệ: " + result.getContactTypeLabel()
                    + " — " + result.getOutcomeLabel(), result));
    }

    @GetMapping("/after-sales/reminders/{id}/logs")
    public ResponseEntity<ApiResponse<List<FollowUpLogDTO>>> getLogsByReminder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", afterSalesService.getLogsByReminder(id)));
    }

    @GetMapping("/after-sales/logs")
    public ResponseEntity<ApiResponse<Page<FollowUpLogDTO>>> listAllLogs(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", afterSalesService.listAllLogs(pageable)));
    }

    // ════════════════════════════════════════════════════════════════════════
    // OVERVIEW
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/after-sales/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success("OK", afterSalesService.getOverviewStats()));
    }
}
