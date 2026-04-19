package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.CustomerInteractionDTO;
import com.tailorshop.metric.dto.LeadAssignmentDTO;
import com.tailorshop.metric.dto.StaffDTO;
import com.tailorshop.metric.service.CustomerService;
import com.tailorshop.metric.service.StaffService;
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
 * StaffController — CRUD Nhân viên, Phân công Lead, Approval Workflow, CRM Interactions
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class StaffController {

    private final StaffService    staffService;
    private final CustomerService customerService;

    // ─── Staff CRUD ───────────────────────────────────────────────────────────

    /** Tạo nhân viên mới */
    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<StaffDTO>> createStaff(@RequestBody StaffDTO dto) {
        StaffDTO created = staffService.createStaff(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Nhân viên đã được tạo", created));
    }

    /** Lấy thông tin 1 nhân viên */
    @GetMapping("/staff/{id}")
    public ResponseEntity<ApiResponse<StaffDTO>> getStaff(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", staffService.getStaffById(id)));
    }

    /** Danh sách nhân viên active phân trang */
    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<Page<StaffDTO>>> listStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<StaffDTO> data = staffService.getAllActiveStaff(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("OK", data));
    }

    /** Lọc nhân viên theo vai trò: SALE | STAFF | MANAGER */
    @GetMapping("/staff/by-role/{role}")
    public ResponseEntity<ApiResponse<List<StaffDTO>>> listByRole(@PathVariable String role) {
        return ResponseEntity.ok(ApiResponse.success("OK", staffService.getStaffByRole(role)));
    }

    /** Cập nhật thông tin nhân viên */
    @PutMapping("/staff/{id}")
    public ResponseEntity<ApiResponse<StaffDTO>> updateStaff(
            @PathVariable Long id,
            @RequestBody StaffDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", staffService.updateStaff(id, dto)));
    }

    /** Cập nhật điểm hiệu suất (Manager only) */
    @PatchMapping("/staff/{id}/performance-score")
    public ResponseEntity<ApiResponse<StaffDTO>> updateScore(
            @PathVariable Long id,
            @RequestParam BigDecimal score) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật điểm thành công",
            staffService.updatePerformanceScore(id, score)));
    }

    /** Vô hiệu hóa nhân viên */
    @DeleteMapping("/staff/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateStaff(@PathVariable Long id) {
        staffService.deactivateStaff(id);
        return ResponseEntity.ok(ApiResponse.success("Nhân viên đã bị vô hiệu hóa", null));
    }

    /** Thống kê tổng quan nhân sự */
    @GetMapping("/staff/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("OK", staffService.getStaffStats()));
    }

    // ─── Lead Distribution ────────────────────────────────────────────────────

    /**
     * Phân công lead cho nhân viên
     * strategy: AUTO_ROUND_ROBIN | AUTO_PERFORMANCE | MANUAL
     */
    @PostMapping("/leads/{leadId}/assign")
    public ResponseEntity<ApiResponse<LeadAssignmentDTO>> assignLead(
            @PathVariable Long leadId,
            @RequestParam(defaultValue = "AUTO_ROUND_ROBIN") String strategy,
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) Long assignedByUserId) {
        LeadAssignmentDTO result = staffService.assignLead(leadId, strategy, staffId, assignedByUserId);
        return ResponseEntity.ok(ApiResponse.success("Phân công thành công", result));
    }

    /** Lịch sử phân công của 1 lead */
    @GetMapping("/leads/{leadId}/assignments")
    public ResponseEntity<ApiResponse<List<LeadAssignmentDTO>>> getAssignmentHistory(
            @PathVariable Long leadId) {
        return ResponseEntity.ok(ApiResponse.success("OK", staffService.getAssignmentHistory(leadId)));
    }

    // ─── Approval Workflow (Manager) ─────────────────────────────────────────

    /** Danh sách assignment đang chờ duyệt */
    @GetMapping("/assignments/pending")
    public ResponseEntity<ApiResponse<List<LeadAssignmentDTO>>> getPendingApprovals() {
        return ResponseEntity.ok(ApiResponse.success("OK", staffService.getPendingApprovals()));
    }

    /** Duyệt 1 assignment */
    @PostMapping("/assignments/{id}/approve")
    public ResponseEntity<ApiResponse<LeadAssignmentDTO>> approveAssignment(
            @PathVariable Long id,
            @RequestParam Long managerUserId) {
        return ResponseEntity.ok(ApiResponse.success("Đã phê duyệt",
            staffService.approveAssignment(id, managerUserId)));
    }

    /** Từ chối và tùy chọn phân lại */
    @PostMapping("/assignments/{id}/reject")
    public ResponseEntity<ApiResponse<LeadAssignmentDTO>> rejectAssignment(
            @PathVariable Long id,
            @RequestParam Long managerUserId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Long newStaffId) {
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối",
            staffService.rejectAssignment(id, managerUserId, reason, newStaffId)));
    }

    // ─── Customer Interactions (2.7) ─────────────────────────────────────────

    /** Thêm tương tác mới với khách hàng */
    @PostMapping("/customers/{customerId}/interactions")
    public ResponseEntity<ApiResponse<CustomerInteractionDTO>> addInteraction(
            @PathVariable Long customerId,
            @RequestBody CustomerInteractionDTO dto) {
        dto.setCustomerId(customerId);
        CustomerInteractionDTO saved = staffService.addInteraction(customerId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Đã thêm tương tác", saved));
    }

    /** Lịch sử tương tác của 1 khách hàng */
    @GetMapping("/customers/{customerId}/interactions")
    public ResponseEntity<ApiResponse<List<CustomerInteractionDTO>>> getInteractions(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success("OK", staffService.getInteractions(customerId)));
    }

    /** Gán nhân viên phụ trách cho khách hàng */
    @PatchMapping("/customers/{customerId}/assign-staff")
    public ResponseEntity<ApiResponse<?>> assignStaffToCustomer(
            @PathVariable Long customerId,
            @RequestParam Long staffId) {
        return ResponseEntity.ok(ApiResponse.success("Đã gán nhân viên",
            customerService.assignStaff(customerId, staffId)));
    }
}
