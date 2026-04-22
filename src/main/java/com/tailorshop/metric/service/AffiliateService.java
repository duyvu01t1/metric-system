package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.AffiliateDTO;
import com.tailorshop.metric.dto.DiscountCodeDTO;
import com.tailorshop.metric.entity.Affiliate;
import com.tailorshop.metric.entity.DiscountCode;
import com.tailorshop.metric.exception.BusinessException;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.AffiliateCommissionRepository;
import com.tailorshop.metric.repository.AffiliateRepository;
import com.tailorshop.metric.repository.DiscountCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * AffiliateService — Quản lý đối tác giới thiệu và mã giảm giá
 *
 * Tính tiền giảm:
 *   PERCENT → min(orderTotal × rate, maxDiscountAmount)
 *   FIXED   → discountValue (nếu orderTotal ≥ minOrderValue)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AffiliateService {

    private final AffiliateRepository           affiliateRepository;
    private final DiscountCodeRepository        discountCodeRepository;
    private final AffiliateCommissionRepository affiliateCommissionRepository;

    // ─── Affiliate CRUD ───────────────────────────────────────────────────────

    @Transactional
    public AffiliateDTO createAffiliate(AffiliateDTO dto) {
        if (affiliateRepository.existsByAffiliateCode(dto.getAffiliateCode())) {
            throw new BusinessException("DUPLICATE_AFFILIATE_CODE", "Mã đối tác đã tồn tại: " + dto.getAffiliateCode());
        }
        Affiliate entity = new Affiliate();
        entity.setAffiliateCode(dto.getAffiliateCode() != null ? dto.getAffiliateCode() : generateAffiliateCode());
        entity.setCompanyName(dto.getCompanyName());
        entity.setContactName(dto.getContactName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setCommissionRate(dto.getCommissionRate() != null ? dto.getCommissionRate() : BigDecimal.valueOf(0.05));
        entity.setIsActive(true);
        entity.setNotes(dto.getNotes());
        Affiliate saved = affiliateRepository.save(entity);
        log.info("Affiliate created: {}", saved.getAffiliateCode());
        return convertAffiliateToDTO(saved);
    }

    @Transactional
    public AffiliateDTO updateAffiliate(Long id, AffiliateDTO dto) {
        Affiliate entity = affiliateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đối tác id=" + id));
        entity.setCompanyName(dto.getCompanyName());
        entity.setContactName(dto.getContactName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        if (dto.getCommissionRate() != null) entity.setCommissionRate(dto.getCommissionRate());
        entity.setNotes(dto.getNotes());
        return convertAffiliateToDTO(affiliateRepository.save(entity));
    }

    @Transactional
    public void deactivateAffiliate(Long id) {
        Affiliate entity = affiliateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đối tác id=" + id));
        entity.setIsActive(false);
        affiliateRepository.save(entity);
        log.info("Affiliate deactivated: {}", entity.getAffiliateCode());
    }

    public AffiliateDTO getAffiliateById(Long id) {
        return convertAffiliateToDTO(affiliateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đối tác id=" + id)));
    }

    public Page<AffiliateDTO> getAllActiveAffiliates(Pageable pageable) {
        Page<Affiliate> page = affiliateRepository.findByIsActiveTrue(pageable);
        List<AffiliateDTO> dtos = page.getContent().stream()
            .map(this::convertAffiliateToDTO)
            .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public List<AffiliateDTO> getAllAffiliates() {
        return affiliateRepository.findAll().stream()
            .map(this::convertAffiliateToDTO)
            .collect(Collectors.toList());
    }

    // ─── Discount Code CRUD ───────────────────────────────────────────────────

    @Transactional
    public DiscountCodeDTO createDiscountCode(Long affiliateId, DiscountCodeDTO dto) {
        if (affiliateId != null) {
            affiliateRepository.findById(affiliateId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đối tác id=" + affiliateId));
        }
        String code = dto.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = generateDiscountCode();
        } else {
            code = code.toUpperCase();
        }
        if (discountCodeRepository.findByCode(code).isPresent()) {
            throw new BusinessException("DUPLICATE_DISCOUNT_CODE", "Mã giảm giá đã tồn tại: " + code);
        }
        DiscountCode entity = new DiscountCode();
        entity.setCode(code);
        entity.setAffiliateId(affiliateId);
        entity.setDiscountType(dto.getDiscountType() != null ? dto.getDiscountType() : "PERCENT");
        entity.setDiscountValue(dto.getDiscountValue());
        entity.setMinOrderValue(dto.getMinOrderValue() != null ? dto.getMinOrderValue() : BigDecimal.ZERO);
        entity.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        entity.setMaxUses(dto.getMaxUses());
        entity.setValidFrom(dto.getValidFrom());
        entity.setValidUntil(dto.getValidUntil());
        entity.setIsActive(true);
        entity.setDescription(dto.getDescription());
        DiscountCode saved = discountCodeRepository.save(entity);
        log.info("Discount code created: {}", saved.getCode());
        return convertDiscountCodeToDTO(saved, affiliateId);
    }

    @Transactional
    public DiscountCodeDTO updateDiscountCode(Long id, DiscountCodeDTO dto) {
        DiscountCode entity = discountCodeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá id=" + id));
        entity.setDiscountType(dto.getDiscountType());
        entity.setDiscountValue(dto.getDiscountValue());
        entity.setMinOrderValue(dto.getMinOrderValue());
        entity.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        entity.setMaxUses(dto.getMaxUses());
        entity.setValidFrom(dto.getValidFrom());
        entity.setValidUntil(dto.getValidUntil());
        entity.setDescription(dto.getDescription());
        return convertDiscountCodeToDTO(discountCodeRepository.save(entity), entity.getAffiliateId());
    }

    @Transactional
    public void deactivateDiscountCode(Long id) {
        DiscountCode entity = discountCodeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá id=" + id));
        entity.setIsActive(false);
        discountCodeRepository.save(entity);
    }

    public List<DiscountCodeDTO> getDiscountCodesByAffiliate(Long affiliateId) {
        return discountCodeRepository.findByAffiliateIdAndIsActiveTrue(affiliateId).stream()
            .map(dc -> convertDiscountCodeToDTO(dc, affiliateId))
            .collect(Collectors.toList());
    }

    // ─── Discount Validation & Calculation ───────────────────────────────────

    /**
     * Preview mã giảm giá cho một giá trị đơn — không thay đổi trạng thái.
     *
     * @param code       mã giảm giá
     * @param orderTotal tổng giá trị đơn hàng
     * @return DTO kèm estimatedDiscount
     * @throws BusinessException nếu mã không hợp lệ
     */
    public DiscountCodeDTO validateAndPreview(String code, BigDecimal orderTotal) {
        DiscountCode dc = discountCodeRepository.findValidCode(
                code.toUpperCase(), LocalDate.now(), orderTotal)
            .orElseThrow(() -> new BusinessException("INVALID_DISCOUNT_CODE",
                "Mã giảm giá không hợp lệ, đã hết hạn, hoặc đơn hàng chưa đủ điều kiện áp dụng."));
        BigDecimal discount = calculateDiscount(dc, orderTotal);
        DiscountCodeDTO dto = convertDiscountCodeToDTO(dc, dc.getAffiliateId());
        dto.setEstimatedDiscount(discount);
        return dto;
    }

    /**
     * Tính số tiền giảm thực tế từ một DiscountCode.
     * Công thức:
     *   PERCENT → min(orderTotal × rate / 100, maxDiscountAmount)
     *   FIXED   → discountValue
     */
    public BigDecimal calculateDiscount(DiscountCode dc, BigDecimal orderTotal) {
        if ("PERCENT".equalsIgnoreCase(dc.getDiscountType())) {
            BigDecimal pct = dc.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal amount = orderTotal.multiply(pct).setScale(0, RoundingMode.HALF_UP);
            if (dc.getMaxDiscountAmount() != null) {
                amount = amount.min(dc.getMaxDiscountAmount());
            }
            return amount;
        } else {
            // FIXED
            return dc.getDiscountValue().min(orderTotal);
        }
    }

    /**
     * Tăng usedCount sau khi mã được áp vào đơn.
     * Gọi trong @Transactional của QuotationService.
     */
    @Transactional
    public void incrementUsedCount(Long discountCodeId) {
        DiscountCode dc = discountCodeRepository.findById(discountCodeId)
            .orElseThrow(() -> new ResourceNotFoundException("Discount code not found: " + discountCodeId));
        dc.setUsedCount(dc.getUsedCount() + 1);
        discountCodeRepository.save(dc);
    }

    /**
     * Tăng tổng stat của affiliate (totalOrders, totalCommissionPaid).
     * Gọi sau khi lưu AffiliateCommission.
     */
    @Transactional
    public void recordAffiliateOrder(Long affiliateId, BigDecimal paidCommission) {
        affiliateRepository.findById(affiliateId).ifPresent(a -> {
            a.setTotalOrders(a.getTotalOrders() + 1);
            a.setTotalCommissionPaid(a.getTotalCommissionPaid().add(paidCommission));
            affiliateRepository.save(a);
        });
    }

    // ─── Converters ───────────────────────────────────────────────────────────

    public AffiliateDTO convertAffiliateToDTO(Affiliate a) {
        return AffiliateDTO.builder()
            .id(a.getId())
            .affiliateCode(a.getAffiliateCode())
            .companyName(a.getCompanyName())
            .contactName(a.getContactName())
            .phone(a.getPhone())
            .email(a.getEmail())
            .commissionRate(a.getCommissionRate())
            .totalOrders(a.getTotalOrders())
            .totalCommissionPaid(a.getTotalCommissionPaid())
            .isActive(a.getIsActive())
            .notes(a.getNotes())
            .createdAt(a.getCreatedAt())
            .build();
    }

    public DiscountCodeDTO convertDiscountCodeToDTO(DiscountCode dc, Long affiliateId) {
        String affiliateName = null;
        if (affiliateId != null) {
            affiliateName = affiliateRepository.findById(affiliateId)
                .map(Affiliate::getCompanyName).orElse(null);
        }
        return DiscountCodeDTO.builder()
            .id(dc.getId())
            .code(dc.getCode())
            .affiliateId(dc.getAffiliateId())
            .affiliateName(affiliateName)
            .discountType(dc.getDiscountType())
            .discountValue(dc.getDiscountValue())
            .minOrderValue(dc.getMinOrderValue())
            .maxDiscountAmount(dc.getMaxDiscountAmount())
            .maxUses(dc.getMaxUses())
            .usedCount(dc.getUsedCount())
            .validFrom(dc.getValidFrom())
            .validUntil(dc.getValidUntil())
            .isActive(dc.getIsActive())
            .description(dc.getDescription())
            .createdAt(dc.getCreatedAt())
            .build();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String generateAffiliateCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rnd  = String.format("%04d", new Random().nextInt(10000));
        return "AFF-" + date + "-" + rnd;
    }

    private String generateDiscountCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rnd  = String.format("%04d", new Random().nextInt(10000));
        return "DISC-" + date + "-" + rnd;
    }
}
