package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.DeliveryDTO;
import com.tailorshop.metric.dto.QCCheckDTO;
import com.tailorshop.metric.dto.QCItemDTO;
import com.tailorshop.metric.service.QCService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * QCController — REST API cho Phân hệ 5 (Kiểm soát Chất lượng & Giao hàng)
 *
 * ─── QC Checklist ────────────────────────────────────────────────────────────
 *   POST   /api/orders/{orderId}/qc              — Tạo phiếu QC mới (8 tiêu chí mặc định)
 *   PUT    /api/qc/{qcId}/start                  — Bắt đầu kiểm tra [PENDING → IN_PROGRESS]
 *   PUT    /api/qc/{qcId}/items/{itemId}         — Cập nhật kết quả 1 tiêu chí (PASS/FAIL/NA)
 *   PUT    /api/qc/{qcId}/finalize               — Tổng hợp & kết luận phiếu QC
 *   PUT    /api/qc/{qcId}/override               — Supervisor override kết quả (bypass)
 *   GET    /api/orders/{orderId}/qc              — Tất cả phiếu QC của 1 đơn hàng
 *   GET    /api/qc/{qcId}                        — Chi tiết 1 phiếu QC (kèm items)
 *   GET    /api/qc                               — Danh sách phiếu QC (phân trang, lọc status)
 *   GET    /api/qc/overview                      — Thống kê tổng quan QC
 *
 * ─── Giao hàng (5.3) ─────────────────────────────────────────────────────────
 *   POST   /api/orders/{orderId}/delivery        — Tạo phiếu giao (yêu cầu QC đã PASSED)
 *   PUT    /api/delivery/{id}/out-for-delivery   — Chuyển sang Đang giao
 *   PUT    /api/delivery/{id}/confirm-delivered  — Xác nhận giao & tất toán thanh toán
 *   PUT    /api/delivery/{id}/return             — Đánh dấu trả lại
 *   PUT    /api/delivery/{id}/cancel             — Hủy phiếu giao
 *   GET    /api/delivery/{id}                    — Chi tiết 1 phiếu giao
 *   GET    /api/orders/{orderId}/delivery        — Tất cả phiếu giao của 1 đơn hàng
 *   GET    /api/delivery                         — Danh sách phiếu giao (phân trang, lọc status)
 *   GET    /api/delivery/today                   — Phiếu hẹn giao hôm nay chưa giao
 *   GET    /api/delivery/overview                — Thống kê tổng quan giao hàng
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class QCController {

    private final QCService qcService;

    // ════════════════════════════════════════════════════════════════════════
    // QC CHECKLIST
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Tạo phiếu QC mới với 8 tiêu chí mặc định.
     * Body: { "userId": 1 }
     */
    @PostMapping("/orders/{orderId}/qc")
    public ResponseEntity<ApiResponse<QCCheckDTO>> createQCCheck(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> body) {
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        QCCheckDTO result = qcService.createQCCheck(orderId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã tạo phiếu QC " + result.getQcNumber(), result));
    }

    /**
     * Bắt đầu kiểm tra: PENDING → IN_PROGRESS.
     * Body: { "userId": 1 }
     */
    @PutMapping("/qc/{qcId}/start")
    public ResponseEntity<ApiResponse<QCCheckDTO>> startQC(
            @PathVariable Long qcId,
            @RequestBody Map<String, Object> body) {
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Đã bắt đầu kiểm tra QC",
                qcService.startQC(qcId, userId)));
    }

    /**
     * Cập nhật kết quả 1 tiêu chí QC.
     * Body: { "result": "PASS|FAIL|NA", "failNote": "...", "userId": 1 }
     */
    @PutMapping("/qc/{qcId}/items/{itemId}")
    public ResponseEntity<ApiResponse<QCItemDTO>> updateItemResult(
            @PathVariable Long qcId,
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> body) {
        String result   = (String) body.get("result");
        String failNote = (String) body.get("failNote");
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật kết quả tiêu chí",
                qcService.updateItemResult(itemId, result, failNote, userId)));
    }

    /**
     * Tổng hợp kết quả phiếu QC → PASSED hoặc FAILED.
     * Body: { "overallNotes": "...", "internalNotes": "...", "userId": 1 }
     */
    @PutMapping("/qc/{qcId}/finalize")
    public ResponseEntity<ApiResponse<QCCheckDTO>> finalizeQC(
            @PathVariable Long qcId,
            @RequestBody Map<String, Object> body) {
        Long userId         = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        String overallNotes  = (String) body.get("overallNotes");
        String internalNotes = (String) body.get("internalNotes");
        return ResponseEntity.ok(ApiResponse.success("Đã tổng hợp kết quả QC",
                qcService.finalizeQC(qcId, userId, overallNotes, internalNotes)));
    }

    /**
     * Supervisor override kết quả QC.
     * Body: { "overallResult": "PASS|FAIL", "notes": "...", "userId": 1 }
     */
    @PutMapping("/qc/{qcId}/override")
    public ResponseEntity<ApiResponse<QCCheckDTO>> overrideQC(
            @PathVariable Long qcId,
            @RequestBody Map<String, Object> body) {
        String overallResult = (String) body.get("overallResult");
        String notes         = (String) body.get("notes");
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Đã override kết quả QC",
                qcService.approveQCOverride(qcId, overallResult, userId, notes)));
    }

    // ─── Đọc dữ liệu QC ──────────────────────────────────────────────────────

    @GetMapping("/orders/{orderId}/qc")
    public ResponseEntity<ApiResponse<List<QCCheckDTO>>> getQCChecksByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("OK", qcService.getQCChecksByOrder(orderId)));
    }

    @GetMapping("/qc/{qcId}")
    public ResponseEntity<ApiResponse<QCCheckDTO>> getQCCheck(@PathVariable Long qcId) {
        return ResponseEntity.ok(ApiResponse.success("OK", qcService.getQCCheckById(qcId)));
    }

    @GetMapping("/qc")
    public ResponseEntity<ApiResponse<Page<QCCheckDTO>>> listQCChecks(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", qcService.listQCChecks(status, pageable)));
    }

    @GetMapping("/qc/overview")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getQCOverview() {
        return ResponseEntity.ok(ApiResponse.success("OK", qcService.getQCOverview()));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GIAO HÀNG (5.3)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Tạo phiếu giao hàng. Yêu cầu đơn hàng đã có phiếu QC PASSED.
     * Body: DeliveryDTO fields (scheduledDate, deliveryMethod, recipientName, recipientPhone,
     *        deliveryAddress, remainingAmount, notes)
     */
    @PostMapping("/orders/{orderId}/delivery")
    public ResponseEntity<ApiResponse<DeliveryDTO>> createDelivery(
            @PathVariable Long orderId,
            @RequestBody DeliveryDTO dto) {
        dto.setOrderId(orderId);
        DeliveryDTO result = qcService.createDelivery(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã tạo phiếu giao hàng " + result.getDeliveryCode(), result));
    }

    /**
     * Chuyển phiếu giao sang Đang giao: SCHEDULED → OUT_FOR_DELIVERY.
     * Body: { "userId": 1 }
     */
    @PutMapping("/delivery/{id}/out-for-delivery")
    public ResponseEntity<ApiResponse<DeliveryDTO>> setOutForDelivery(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Đã chuyển sang Đang giao",
                qcService.setOutForDelivery(id, userId)));
    }

    /**
     * Xác nhận giao thành công & tất toán (5.3).
     * Body: { "amountCollected": 500000, "paymentMethod": "CASH", "userId": 1, "notes": "..." }
     */
    @PutMapping("/delivery/{id}/confirm-delivered")
    public ResponseEntity<ApiResponse<DeliveryDTO>> confirmDelivered(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = body.containsKey("amountCollected")
                ? new BigDecimal(body.get("amountCollected").toString()) : BigDecimal.ZERO;
        String paymentMethod = (String) body.get("paymentMethod");
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        String notes = (String) body.get("notes");
        return ResponseEntity.ok(ApiResponse.success("Giao hàng thành công. Đơn hàng đã được tất toán.",
                qcService.confirmDelivered(id, amount, paymentMethod, userId, notes)));
    }

    /**
     * Đánh dấu trả lại.
     * Body: { "returnNotes": "...", "userId": 1 }
     */
    @PutMapping("/delivery/{id}/return")
    public ResponseEntity<ApiResponse<DeliveryDTO>> markReturned(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String returnNotes = (String) body.get("returnNotes");
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu trả lại",
                qcService.markReturned(id, returnNotes, userId)));
    }

    /**
     * Hủy phiếu giao hàng.
     * Body: { "userId": 1 }
     */
    @PutMapping("/delivery/{id}/cancel")
    public ResponseEntity<ApiResponse<DeliveryDTO>> cancelDelivery(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : 1L;
        return ResponseEntity.ok(ApiResponse.success("Đã hủy phiếu giao hàng",
                qcService.cancelDelivery(id, userId)));
    }

    // ─── Đọc dữ liệu Delivery ────────────────────────────────────────────────

    @GetMapping("/delivery/{id}")
    public ResponseEntity<ApiResponse<DeliveryDTO>> getDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", qcService.getDeliveryById(id)));
    }

    @GetMapping("/orders/{orderId}/delivery")
    public ResponseEntity<ApiResponse<List<DeliveryDTO>>> getDeliveriesByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("OK", qcService.getDeliveriesByOrder(orderId)));
    }

    @GetMapping("/delivery")
    public ResponseEntity<ApiResponse<Page<DeliveryDTO>>> listDeliveries(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", qcService.listDeliveries(status, pageable)));
    }

    @GetMapping("/delivery/today")
    public ResponseEntity<ApiResponse<List<DeliveryDTO>>> getTodayDeliveries() {
        return ResponseEntity.ok(ApiResponse.success("OK", qcService.getTodayPendingDeliveries()));
    }

    @GetMapping("/delivery/overview")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getDeliveryOverview() {
        return ResponseEntity.ok(ApiResponse.success("OK", qcService.getDeliveryOverview()));
    }
}
