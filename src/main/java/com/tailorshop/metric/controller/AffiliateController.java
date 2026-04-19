package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.AffiliateDTO;
import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.DiscountCodeDTO;
import com.tailorshop.metric.service.AffiliateService;
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
 * AffiliateController — Quản lý đối tác và mã giảm giá
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AffiliateController {

    private final AffiliateService affiliateService;

    // ─── Affiliate CRUD ───────────────────────────────────────────────────────

    @PostMapping("/affiliates")
    public ResponseEntity<ApiResponse<AffiliateDTO>> createAffiliate(@RequestBody AffiliateDTO dto) {
        AffiliateDTO created = affiliateService.createAffiliate(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Đối tác đã được tạo", created));
    }

    @GetMapping("/affiliates/{id}")
    public ResponseEntity<ApiResponse<AffiliateDTO>> getAffiliate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", affiliateService.getAffiliateById(id)));
    }

    @GetMapping("/affiliates")
    public ResponseEntity<ApiResponse<List<AffiliateDTO>>> listAffiliates(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (activeOnly) {
            Page<AffiliateDTO> data = affiliateService.getAllActiveAffiliates(PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.success("OK", data.getContent()));
        }
        return ResponseEntity.ok(ApiResponse.success("OK", affiliateService.getAllAffiliates()));
    }

    @PutMapping("/affiliates/{id}")
    public ResponseEntity<ApiResponse<AffiliateDTO>> updateAffiliate(
            @PathVariable Long id, @RequestBody AffiliateDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", affiliateService.updateAffiliate(id, dto)));
    }

    @DeleteMapping("/affiliates/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateAffiliate(@PathVariable Long id) {
        affiliateService.deactivateAffiliate(id);
        return ResponseEntity.ok(ApiResponse.success("Đã vô hiệu hóa đối tác", null));
    }

    // ─── Discount Code Management ─────────────────────────────────────────────

    @PostMapping("/affiliates/{affiliateId}/discount-codes")
    public ResponseEntity<ApiResponse<DiscountCodeDTO>> createDiscountCode(
            @PathVariable Long affiliateId, @RequestBody DiscountCodeDTO dto) {
        DiscountCodeDTO created = affiliateService.createDiscountCode(affiliateId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Mã giảm giá đã được tạo", created));
    }

    /** Tạo mã giảm giá không thuộc affiliate */
    @PostMapping("/discount-codes")
    public ResponseEntity<ApiResponse<DiscountCodeDTO>> createStandaloneDiscountCode(@RequestBody DiscountCodeDTO dto) {
        DiscountCodeDTO created = affiliateService.createDiscountCode(null, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Mã giảm giá đã được tạo", created));
    }

    @PutMapping("/discount-codes/{id}")
    public ResponseEntity<ApiResponse<DiscountCodeDTO>> updateDiscountCode(
            @PathVariable Long id, @RequestBody DiscountCodeDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", affiliateService.updateDiscountCode(id, dto)));
    }

    @GetMapping("/affiliates/{affiliateId}/discount-codes")
    public ResponseEntity<ApiResponse<List<DiscountCodeDTO>>> listDiscountCodes(@PathVariable Long affiliateId) {
        return ResponseEntity.ok(ApiResponse.success("OK", affiliateService.getDiscountCodesByAffiliate(affiliateId)));
    }

    @PostMapping("/discount-codes/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateDiscountCode(@PathVariable Long id) {
        affiliateService.deactivateDiscountCode(id);
        return ResponseEntity.ok(ApiResponse.success("Mã giảm giá đã bị vô hiệu hóa", null));
    }

    /**
     * Preview mã giảm giá — không thay đổi trạng thái
     * GET /discount-codes/validate?code=XXX&orderTotal=500000
     */
    @GetMapping("/discount-codes/validate")
    public ResponseEntity<ApiResponse<DiscountCodeDTO>> validateDiscountCode(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal) {
        DiscountCodeDTO result = affiliateService.validateAndPreview(code, orderTotal);
        return ResponseEntity.ok(ApiResponse.success("Mã hợp lệ", result));
    }
}
