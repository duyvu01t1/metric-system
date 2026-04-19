package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.QuotationDTO;
import com.tailorshop.metric.dto.StaffCommissionDTO;
import com.tailorshop.metric.service.QuotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * QuotationController — Báo giá, Đặt cọc, Phân công nhân viên, Hoa hồng
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    // ─── Quotation CRUD & Workflow ────────────────────────────────────────────

    @PostMapping("/quotations")
    public ResponseEntity<ApiResponse<QuotationDTO>> createQuotation(@RequestBody QuotationDTO dto) {
        QuotationDTO created = quotationService.createQuotation(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Báo giá đã được tạo", created));
    }

    @GetMapping("/quotations/{id}")
    public ResponseEntity<ApiResponse<QuotationDTO>> getQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", quotationService.getQuotationById(id)));
    }

    @GetMapping("/quotations")
    public ResponseEntity<ApiResponse<Page<QuotationDTO>>> listQuotations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<QuotationDTO> data = quotationService.listQuotations(status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("OK", data));
    }

    @GetMapping("/quotations/by-customer/{customerId}")
    public ResponseEntity<ApiResponse<List<QuotationDTO>>> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success("OK", quotationService.getQuotationsByCustomer(customerId)));
    }

    @PutMapping("/quotations/{id}")
    public ResponseEntity<ApiResponse<QuotationDTO>> updateQuotation(
            @PathVariable Long id, @RequestBody QuotationDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", quotationService.updateQuotation(id, dto)));
    }

    @PostMapping("/quotations/{id}/send")
    public ResponseEntity<ApiResponse<QuotationDTO>> sendQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Đã gửi báo giá cho khách", quotationService.sendQuotation(id)));
    }

    @PostMapping("/quotations/{id}/accept")
    public ResponseEntity<ApiResponse<QuotationDTO>> acceptQuotation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = body != null && body.containsKey("userId")
            ? Long.valueOf(body.get("userId").toString()) : null;
        return ResponseEntity.ok(ApiResponse.success("Báo giá đã được chấp nhận — đơn hàng đã tạo",
            quotationService.acceptQuotation(id, userId)));
    }

    @PostMapping("/quotations/{id}/reject")
    public ResponseEntity<ApiResponse<QuotationDTO>> rejectQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Báo giá đã bị từ chối", quotationService.rejectQuotation(id)));
    }

    // ─── Deposit (3.7) ───────────────────────────────────────────────────────

    @PostMapping("/orders/{orderId}/deposit/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmDeposit(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = body.containsKey("depositAmount")
            ? new BigDecimal(body.get("depositAmount").toString()) : null;
        Long userId = body.containsKey("userId")
            ? Long.valueOf(body.get("userId").toString()) : null;
        quotationService.confirmDeposit(orderId, amount, userId);
        return ResponseEntity.ok(ApiResponse.success("Đặt cọc đã được xác nhận", null));
    }

    // ─── Staff assignment & Commissions (3.8 / 3.9) ─────────────────────────

    @PostMapping("/orders/{orderId}/assign-staff")
    public ResponseEntity<ApiResponse<List<StaffCommissionDTO>>> assignStaff(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> body) {
        Long primaryId   = Long.valueOf(body.get("primaryStaffId").toString());
        Long secondaryId = body.containsKey("secondaryStaffId") && body.get("secondaryStaffId") != null
            ? Long.valueOf(body.get("secondaryStaffId").toString()) : null;
        quotationService.assignStaffToOrder(orderId, primaryId, secondaryId);
        List<StaffCommissionDTO> commissions = quotationService.getCommissionsByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("Phân công nhân viên thành công", commissions));
    }

    @GetMapping("/orders/{orderId}/commissions")
    public ResponseEntity<ApiResponse<List<StaffCommissionDTO>>> getCommissions(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("OK", quotationService.getCommissionsByOrder(orderId)));
    }

    /**
     * Override thủ công hoa hồng (3.3)
     * PATCH /commissions/{id}
     */
    @PatchMapping("/commissions/{id}")
    public ResponseEntity<ApiResponse<StaffCommissionDTO>> overrideCommission(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        BigDecimal newAmount = new BigDecimal(body.get("commissionAmount").toString());
        String reason = body.containsKey("reason") ? body.get("reason").toString() : null;
        Long updatedBy = body.containsKey("updatedBy") ? Long.valueOf(body.get("updatedBy").toString()) : null;
        StaffCommissionDTO updated = quotationService.updateCommissionManually(id, newAmount, reason, updatedBy);
        return ResponseEntity.ok(ApiResponse.success("Hoa hồng đã được cập nhật", updated));
    }
}
