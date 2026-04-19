package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.ExpenseDTO;
import com.tailorshop.metric.dto.StaffCommissionDTO;
import com.tailorshop.metric.service.FinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * FinanceController — REST API Phân hệ 7: Tài chính & Hoa hồng
 *
 * ─── Overview ─────────────────────────────────────────────────────────────
 *   GET  /api/finance/overview                            — tổng quan tài chính tháng
 *
 * ─── Staff Commissions (7.1–7.3) ──────────────────────────────────────────
 *   POST /api/finance/commissions/calculate/{orderId}     — tính hoa hồng cho đơn (7.2)
 *   GET  /api/finance/commissions                         — danh sách (filter: staffId/isPaid/year/month)
 *   GET  /api/finance/commissions/{id}                    — chi tiết 1 bản ghi
 *   GET  /api/finance/commissions/by-order/{orderId}      — tất cả hoa hồng của 1 đơn
 *   GET  /api/finance/commissions/by-staff/{staffId}      — tất cả hoa hồng của 1 nhân viên
 *   GET  /api/finance/commissions/summary                 — tổng hợp nhóm theo NV
 *   PUT  /api/finance/commissions/{id}/override           — chỉnh sửa thủ công (7.3)
 *   PUT  /api/finance/commissions/{id}/mark-paid          — xác nhận đã trả hoa hồng
 *   PUT  /api/finance/commissions/bulk-mark-paid          — trả nhiều cùng lúc
 *
 * ─── Expenses (7.4) ────────────────────────────────────────────────────────
 *   POST   /api/finance/expenses                          — tạo chi phí mới
 *   GET    /api/finance/expenses                          — danh sách (filter: category/status/year/month)
 *   GET    /api/finance/expenses/{id}                     — chi tiết 1 chi phí
 *   PUT    /api/finance/expenses/{id}                     — cập nhật
 *   DELETE /api/finance/expenses/{id}                     — xóa
 *   PUT    /api/finance/expenses/{id}/approve             — phê duyệt
 *   PUT    /api/finance/expenses/{id}/reject              — từ chối
 *   GET    /api/finance/expenses/summary                  — tổng theo danh mục
 */
@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
@Slf4j
public class FinanceController {

    private final FinanceService financeService;

    // ════════════════════════════════════════════════════════════════════════
    // OVERVIEW
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(
            @RequestParam(defaultValue = "0") short year,
            @RequestParam(defaultValue = "0") short month) {
        if (year  == 0) year  = (short) LocalDate.now().getYear();
        if (month == 0) month = (short) LocalDate.now().getMonthValue();
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getFinanceOverview(year, month)));
    }

    // ════════════════════════════════════════════════════════════════════════
    // STAFF COMMISSIONS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Tính hoa hồng tự động cho đơn hàng (7.2).
     * Body: { "userId": 1 }
     */
    @PostMapping("/commissions/calculate/{orderId}")
    public ResponseEntity<ApiResponse<List<StaffCommissionDTO>>> calculateCommissions(
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = body != null && body.containsKey("userId")
            ? Long.valueOf(body.get("userId").toString()) : 1L;
        List<StaffCommissionDTO> result = financeService.calculateCommissionsForOrder(orderId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Đã tính " + result.size() + " hoa hồng cho đơn #" + orderId, result));
    }

    /**
     * Danh sách hoa hồng với filter.
     * Query params: staffId, isPaid (true/false), year, month, page, size
     */
    @GetMapping("/commissions")
    public ResponseEntity<ApiResponse<Page<StaffCommissionDTO>>> listCommissions(
            @RequestParam(required = false) Long    staffId,
            @RequestParam(required = false) Boolean isPaid,
            @RequestParam(required = false) Short   year,
            @RequestParam(required = false) Short   month,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK",
            financeService.listCommissions(staffId, isPaid, year, month, pageable)));
    }

    @GetMapping("/commissions/{id}")
    public ResponseEntity<ApiResponse<StaffCommissionDTO>> getCommission(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getCommissionById(id)));
    }

    @GetMapping("/commissions/by-order/{orderId}")
    public ResponseEntity<ApiResponse<List<StaffCommissionDTO>>> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getCommissionsByOrder(orderId)));
    }

    @GetMapping("/commissions/by-staff/{staffId}")
    public ResponseEntity<ApiResponse<List<StaffCommissionDTO>>> getByStaff(@PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getCommissionsByStaff(staffId)));
    }

    /**
     * Tổng hợp hoa hồng nhóm theo nhân viên trong kỳ.
     * Query params: year, month
     */
    @GetMapping("/commissions/summary")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSummary(
            @RequestParam(defaultValue = "0") short year,
            @RequestParam(defaultValue = "0") short month) {
        if (year  == 0) year  = (short) LocalDate.now().getYear();
        if (month == 0) month = (short) LocalDate.now().getMonthValue();
        return ResponseEntity.ok(ApiResponse.success("OK",
            financeService.getCommissionSummaryByPeriod(year, month)));
    }

    /**
     * Chỉnh sửa hoa hồng thủ công (7.3).
     * Body: { "amount": 500000, "reason": "...", "productionStageId": 3, "userId": 1 }
     */
    @PutMapping("/commissions/{id}/override")
    public ResponseEntity<ApiResponse<StaffCommissionDTO>> overrideCommission(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.getOrDefault("amount", "0").toString());
        String reason     = (String) body.getOrDefault("reason", null);
        Long stageId      = body.containsKey("productionStageId")
            ? Long.valueOf(body.get("productionStageId").toString()) : null;
        Long userId       = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật hoa hồng",
            financeService.overrideCommission(id, amount, reason, stageId, userId)));
    }

    /**
     * Xác nhận đã trả hoa hồng.
     * Body: { "userId": 1 }
     */
    @PutMapping("/commissions/{id}/mark-paid")
    public ResponseEntity<ApiResponse<StaffCommissionDTO>> markPaid(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = body != null && body.containsKey("userId")
            ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận thanh toán hoa hồng",
            financeService.markPaid(id, userId)));
    }

    /**
     * Trả hoa hồng hàng loạt.
     * Body: { "ids": [1,2,3], "userId": 1 }
     */
    @PutMapping("/commissions/bulk-mark-paid")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkMarkPaid(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) body.getOrDefault("ids", List.of());
        List<Long> ids = rawIds.stream().map(o -> Long.valueOf(o.toString())).toList();
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        int count = financeService.markPaidBulk(ids, userId);
        return ResponseEntity.ok(ApiResponse.success("Đã thanh toán " + count + " hoa hồng",
            Map.of("paid", count)));
    }

    // ════════════════════════════════════════════════════════════════════════
    // EXPENSES
    // ════════════════════════════════════════════════════════════════════════

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<ExpenseDTO>> createExpense(
            @RequestBody ExpenseDTO dto,
            @RequestParam(defaultValue = "1") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Đã tạo chi phí " + dto.getTitle(),
                financeService.createExpense(dto, userId)));
    }

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<Page<ExpenseDTO>>> listExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Short  year,
            @RequestParam(required = false) Short  month,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK",
            financeService.listExpenses(category, status, year, month, pageable)));
    }

    @GetMapping("/expenses/{id}")
    public ResponseEntity<ApiResponse<ExpenseDTO>> getExpense(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getExpenseById(id)));
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<ApiResponse<ExpenseDTO>> updateExpense(
            @PathVariable Long id,
            @RequestBody ExpenseDTO dto,
            @RequestParam(defaultValue = "1") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật chi phí",
            financeService.updateExpense(id, dto, userId)));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable Long id) {
        financeService.deleteExpense(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa chi phí", null));
    }

    @PutMapping("/expenses/{id}/approve")
    public ResponseEntity<ApiResponse<ExpenseDTO>> approveExpense(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Đã duyệt chi phí",
            financeService.approveExpense(id, userId)));
    }

    @PutMapping("/expenses/{id}/reject")
    public ResponseEntity<ApiResponse<ExpenseDTO>> rejectExpense(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String reason = (String) body.getOrDefault("reason", null);
        Long userId   = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối chi phí",
            financeService.rejectExpense(id, reason, userId)));
    }

    @GetMapping("/expenses/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExpenseSummary(
            @RequestParam(defaultValue = "0") short year,
            @RequestParam(defaultValue = "0") short month) {
        if (year  == 0) year  = (short) LocalDate.now().getYear();
        if (month == 0) month = (short) LocalDate.now().getMonthValue();
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getExpenseSummary(year, month)));
    }
}
