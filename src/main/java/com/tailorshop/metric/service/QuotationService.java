package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.QuotationDTO;
import com.tailorshop.metric.dto.StaffCommissionDTO;
import com.tailorshop.metric.entity.*;
import com.tailorshop.metric.exception.BusinessException;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * QuotationService — Quản lý báo giá, đặt cọc, phân công 2 nhân viên, hoa hồng
 *
 * Luồng chính:
 *  1. createQuotation() — tạo báo giá ở trạng thái DRAFT
 *  2. sendQuotation()   — gửi cho khách (DRAFT → SENT)
 *  3. acceptQuotation() — khách đồng ý (SENT → ACCEPTED) → tự động tạo TailoringOrder
 *  4. rejectQuotation() — khách từ chối (SENT → REJECTED)
 *  5. (đơn hàng) confirmDeposit()         — xác nhận đặt cọc (depositStatus PENDING → CONFIRMED)
 *  6. (đơn hàng) assignStaffToOrder()     — gán 2 nhân viên chính/phụ
 *  7. (đơn hàng) calculateCommissions()   — tính hoa hồng cho 2 nhân viên
 *  8. (đơn hàng) updateCommissionManually() — override thủ công (3.3)
 *
 * Deposit gate (3.7):
 *   Phân hệ sản xuất (Phân hệ 4) phải gọi assertDepositConfirmed() trong TailoringOrderService
 *   trước khi tạo production stage đầu tiên.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationService {

    /** Tỉ lệ hoa hồng nhân viên phụ so với nhân viên chính (mặc định 50%) */
    @Value("${app.quotation.secondary-staff-commission-ratio:0.5}")
    private double secondaryCommissionRatio;

    private final QuotationRepository          quotationRepository;
    private final TailoringOrderRepository     orderRepository;
    private final CustomerRepository           customerRepository;
    private final StaffRepository              staffRepository;
    private final ChannelRepository            channelRepository;
    private final DiscountCodeRepository       discountCodeRepository;
    private final StaffCommissionRepository    staffCommissionRepository;
    private final AffiliateCommissionRepository affiliateCommissionRepository;
    private final AffiliateRepository          affiliateRepository;
    private final AffiliateService             affiliateService;

    // ─── Quotation CRUD & Workflow ────────────────────────────────────────────

    @Transactional
    public QuotationDTO createQuotation(QuotationDTO dto) {
        customerRepository.findById(dto.getCustomerId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng id=" + dto.getCustomerId()));

        Quotation q = new Quotation();
        q.setQuotationCode(generateQuotationCode());
        q.setCustomerId(dto.getCustomerId());
        q.setOrderType(dto.getOrderType());
        q.setFabricMaterial(dto.getFabricMaterial());
        q.setFabricColor(dto.getFabricColor());
        q.setAccessories(dto.getAccessories());
        q.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 1);
        q.setUnitPrice(dto.getUnitPrice());
        q.setFabricCost(orZero(dto.getFabricCost()));
        q.setAccessoriesCost(orZero(dto.getAccessoriesCost()));
        q.setLaborCost(orZero(dto.getLaborCost()));
        q.setSourceChannelId(dto.getSourceChannelId());
        q.setValidUntil(dto.getValidUntil());
        q.setNotes(dto.getNotes());
        q.setStatus("DRAFT");
        q.setCreatedBy(dto.getCreatedBy());

        // Tính subtotal & áp mã giảm
        BigDecimal subtotal = computeSubtotal(q);
        q.setSubtotal(subtotal);
        applyDiscount(q, dto.getDiscountCodeId(), subtotal);

        Quotation saved = quotationRepository.save(q);
        log.info("Quotation created: {}", saved.getQuotationCode());
        return convertToDTO(saved);
    }

    @Transactional
    public QuotationDTO updateQuotation(Long id, QuotationDTO dto) {
        Quotation q = findQuotationById(id);
        if ("CONVERTED".equals(q.getStatus()) || "ACCEPTED".equals(q.getStatus())) {
            throw new BusinessException("QUOTATION_NOT_EDITABLE",
                "Không thể chỉnh sửa báo giá đã được chấp nhận hoặc chuyển đổi.");
        }
        q.setOrderType(dto.getOrderType());
        q.setFabricMaterial(dto.getFabricMaterial());
        q.setFabricColor(dto.getFabricColor());
        q.setAccessories(dto.getAccessories());
        q.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : q.getQuantity());
        q.setUnitPrice(dto.getUnitPrice());
        q.setFabricCost(orZero(dto.getFabricCost()));
        q.setAccessoriesCost(orZero(dto.getAccessoriesCost()));
        q.setLaborCost(orZero(dto.getLaborCost()));
        q.setSourceChannelId(dto.getSourceChannelId());
        q.setValidUntil(dto.getValidUntil());
        q.setNotes(dto.getNotes());
        q.setUpdatedBy(dto.getCreatedBy());

        BigDecimal subtotal = computeSubtotal(q);
        q.setSubtotal(subtotal);
        applyDiscount(q, dto.getDiscountCodeId(), subtotal);

        return convertToDTO(quotationRepository.save(q));
    }

    @Transactional
    public QuotationDTO sendQuotation(Long id) {
        Quotation q = findQuotationById(id);
        if (!"DRAFT".equals(q.getStatus())) {
            throw new BusinessException("INVALID_QUOTATION_STATUS",
                "Chỉ có thể gửi báo giá ở trạng thái DRAFT (hiện tại: " + q.getStatus() + ")");
        }
        q.setStatus("SENT");
        return convertToDTO(quotationRepository.save(q));
    }

    @Transactional
    public QuotationDTO acceptQuotation(Long id, Long createdByUserId) {
        Quotation q = findQuotationById(id);
        if (!"SENT".equals(q.getStatus())) {
            throw new BusinessException("INVALID_QUOTATION_STATUS",
                "Chỉ có thể chấp nhận báo giá ở trạng thái SENT (hiện tại: " + q.getStatus() + ")");
        }
        q.setStatus("ACCEPTED");

        // Tự động tạo TailoringOrder từ báo giá
        TailoringOrder order = createOrderFromQuotation(q, createdByUserId);
        q.setOrderId(order.getId());
        q.setStatus("CONVERTED");

        // Nếu có discount code → increment used count + ghi affiliate commission
        if (q.getDiscountCodeId() != null) {
            affiliateService.incrementUsedCount(q.getDiscountCodeId());
            recordAffiliateCommission(order, q);
        }

        quotationRepository.save(q);
        log.info("Quotation {} converted to order {}", q.getQuotationCode(), order.getOrderCode());
        return convertToDTO(q);
    }

    @Transactional
    public QuotationDTO rejectQuotation(Long id) {
        Quotation q = findQuotationById(id);
        if (!"SENT".equals(q.getStatus())) {
            throw new BusinessException("INVALID_QUOTATION_STATUS",
                "Chỉ có thể từ chối báo giá ở trạng thái SENT (hiện tại: " + q.getStatus() + ")");
        }
        q.setStatus("REJECTED");
        return convertToDTO(quotationRepository.save(q));
    }

    public QuotationDTO getQuotationById(Long id) {
        return convertToDTO(findQuotationById(id));
    }

    public List<QuotationDTO> getQuotationsByCustomer(Long customerId) {
        return quotationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public Page<QuotationDTO> listQuotations(String status, Pageable pageable) {
        Page<Quotation> page;
        if (status != null && !status.isBlank()) {
            page = quotationRepository.findByStatus(status, pageable);
        } else {
            page = quotationRepository.findAll(pageable);
        }
        List<QuotationDTO> dtos = page.getContent().stream().map(this::convertToDTO).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    // ─── Deposit confirmation (3.7) ───────────────────────────────────────────

    @Transactional
    public void confirmDeposit(Long orderId, BigDecimal depositAmount, Long confirmedByUserId) {
        TailoringOrder order = findOrderById(orderId);
        if ("CONFIRMED".equals(order.getDepositStatus())) {
            throw new BusinessException("DEPOSIT_ALREADY_CONFIRMED", "Đặt cọc đã được xác nhận trước đó.");
        }
        order.setDepositAmount(depositAmount != null ? depositAmount : orZero(order.getDepositAmount()));
        order.setDepositStatus("CONFIRMED");
        order.setDepositDate(LocalDate.now());
        order.setDepositConfirmedBy(confirmedByUserId);
        order.setDepositConfirmedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Deposit confirmed for order {}", order.getOrderCode());
    }

    // ─── 2-Staff assignment & Commissions (3.8 / 3.9) ────────────────────────

    @Transactional
    public void assignStaffToOrder(Long orderId, Long primaryStaffId, Long secondaryStaffId) {
        TailoringOrder order = findOrderById(orderId);
        staffRepository.findById(primaryStaffId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên chính id=" + primaryStaffId));
        if (secondaryStaffId != null) {
            staffRepository.findById(secondaryStaffId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên phụ id=" + secondaryStaffId));
        }
        order.setPrimaryStaffId(primaryStaffId);
        order.setSecondaryStaffId(secondaryStaffId);
        orderRepository.save(order);

        // Tính lại hoa hồng sau khi gán nhân viên
        calculateAndSaveCommissions(orderId);
    }

    @Transactional
    public List<StaffCommissionDTO> calculateAndSaveCommissions(Long orderId) {
        TailoringOrder order = findOrderById(orderId);
        BigDecimal base = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;

        // Xoá bản ghi cũ (nếu tái tính)
        List<StaffCommission> existing = staffCommissionRepository.findByOrderId(orderId);
        staffCommissionRepository.deleteAll(existing);

        // PRIMARY nhân viên
        if (order.getPrimaryStaffId() != null) {
            Staff primary = staffRepository.findById(order.getPrimaryStaffId()).orElse(null);
            if (primary != null) {
                BigDecimal rate = primary.getBaseCommissionRate();
                StaffCommission sc = new StaffCommission();
                sc.setOrderId(orderId);
                sc.setStaffId(primary.getId());
                sc.setStaffRoleType("PRIMARY");
                sc.setCommissionRate(rate);
                sc.setCommissionBase(base);
                sc.setCommissionAmount(base.multiply(rate).setScale(0, RoundingMode.HALF_UP));
                sc.setIsManualOverride(false);
                sc.setIsPaid(false);
                staffCommissionRepository.save(sc);
            }
        }

        // SECONDARY nhân viên — hoa hồng = primaryRate × secondaryCommissionRatio
        if (order.getSecondaryStaffId() != null) {
            Staff secondary = staffRepository.findById(order.getSecondaryStaffId()).orElse(null);
            if (secondary != null) {
                BigDecimal rate = secondary.getBaseCommissionRate()
                    .multiply(BigDecimal.valueOf(secondaryCommissionRatio))
                    .setScale(4, RoundingMode.HALF_UP);
                StaffCommission sc = new StaffCommission();
                sc.setOrderId(orderId);
                sc.setStaffId(secondary.getId());
                sc.setStaffRoleType("SECONDARY");
                sc.setCommissionRate(rate);
                sc.setCommissionBase(base);
                sc.setCommissionAmount(base.multiply(rate).setScale(0, RoundingMode.HALF_UP));
                sc.setIsManualOverride(false);
                sc.setIsPaid(false);
                staffCommissionRepository.save(sc);
            }
        }

        return getCommissionsByOrder(orderId);
    }

    /**
     * Override thủ công hoa hồng nhân viên (3.3)
     */
    @Transactional
    public StaffCommissionDTO updateCommissionManually(Long commissionId, BigDecimal newAmount, String reason, Long updatedBy) {
        StaffCommission sc = staffCommissionRepository.findById(commissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi hoa hồng id=" + commissionId));
        if (sc.getIsPaid()) {
            throw new BusinessException("COMMISSION_ALREADY_PAID", "Không thể chỉnh sửa hoa hồng đã thanh toán.");
        }
        sc.setCommissionAmount(newAmount);
        sc.setIsManualOverride(true);
        sc.setOverrideReason(reason);
        return convertCommissionToDTO(staffCommissionRepository.save(sc));
    }

    public List<StaffCommissionDTO> getCommissionsByOrder(Long orderId) {
        return staffCommissionRepository.findByOrderId(orderId).stream()
            .map(this::convertCommissionToDTO)
            .collect(Collectors.toList());
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private TailoringOrder createOrderFromQuotation(Quotation q, Long createdByUserId) {
        Customer customer = customerRepository.findById(q.getCustomerId())
            .orElseThrow(() -> new ResourceNotFoundException("Khách hàng không tồn tại id=" + q.getCustomerId()));

        TailoringOrder order = new TailoringOrder();
        order.setOrderCode(generateOrderCode());
        order.setCustomer(customer);
        order.setOrderDate(LocalDate.now());
        order.setOrderType(q.getOrderType());
        order.setFabricMaterial(q.getFabricMaterial());
        order.setFabricColor(q.getFabricColor());
        order.setAccessories(q.getAccessories());
        order.setQuantity(q.getQuantity());
        order.setUnitPrice(q.getUnitPrice());
        order.setTotalPrice(q.getTotalAmount());
        order.setSourceChannelId(q.getSourceChannelId());
        order.setQuotationId(q.getId());
        order.setDiscountCodeId(q.getDiscountCodeId());
        order.setDiscountAmount(orZero(q.getDiscountAmount()));
        order.setStatus("PENDING");
        order.setPaymentStatus("UNPAID");
        order.setDepositStatus("NONE");
        order.setIsArchived(false);
        return orderRepository.save(order);
    }

    private void applyDiscount(Quotation q, Long discountCodeId, BigDecimal subtotal) {
        if (discountCodeId != null) {
            Optional<DiscountCode> dcOpt = discountCodeRepository.findById(discountCodeId);
            if (dcOpt.isPresent()) {
                DiscountCode dc = dcOpt.get();
                BigDecimal discount = affiliateService.calculateDiscount(dc, subtotal);
                q.setDiscountCodeId(discountCodeId);
                q.setDiscountAmount(discount);
                q.setTotalAmount(subtotal.subtract(discount).max(BigDecimal.ZERO));
                return;
            }
        }
        q.setDiscountCodeId(null);
        q.setDiscountAmount(BigDecimal.ZERO);
        q.setTotalAmount(subtotal);
    }

    private void recordAffiliateCommission(TailoringOrder order, Quotation q) {
        if (q.getDiscountCodeId() == null) return;
        discountCodeRepository.findById(q.getDiscountCodeId()).ifPresent(dc -> {
            if (dc.getAffiliateId() == null) return;
            affiliateRepository.findById(dc.getAffiliateId()).ifPresent(aff -> {
                BigDecimal commission = order.getTotalPrice()
                    .multiply(aff.getCommissionRate())
                    .setScale(0, RoundingMode.HALF_UP);
                AffiliateCommission ac = new AffiliateCommission();
                ac.setOrderId(order.getId());
                ac.setAffiliateId(aff.getId());
                ac.setDiscountCodeId(dc.getId());
                ac.setDiscountGiven(orZero(q.getDiscountAmount()));
                ac.setCommissionRate(aff.getCommissionRate());
                ac.setCommissionAmount(commission);
                ac.setIsPaid(false);
                affiliateCommissionRepository.save(ac);
                log.info("Affiliate commission recorded: {} → {}", aff.getAffiliateCode(), commission);
            });
        });
    }

    private BigDecimal computeSubtotal(Quotation q) {
        BigDecimal qty = BigDecimal.valueOf(q.getQuantity() != null ? q.getQuantity() : 1);
        BigDecimal unit = orZero(q.getUnitPrice());
        BigDecimal fabric = orZero(q.getFabricCost());
        BigDecimal acc = orZero(q.getAccessoriesCost());
        BigDecimal labor = orZero(q.getLaborCost());
        return unit.multiply(qty).add(fabric).add(acc).add(labor);
    }

    private Quotation findQuotationById(Long id) {
        return quotationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo giá id=" + id));
    }

    private TailoringOrder findOrderById(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng id=" + id));
    }

    private BigDecimal orZero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private String generateQuotationCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rnd  = String.format("%04d", new Random().nextInt(10000));
        String candidate = "Q-" + date + "-" + rnd;
        return quotationRepository.existsByQuotationCode(candidate) ? generateQuotationCode() : candidate;
    }

    private String generateOrderCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rnd  = String.format("%04d", new Random().nextInt(10000));
        return "ORD-" + date + "-" + rnd;
    }

    // ─── Converters ───────────────────────────────────────────────────────────

    public QuotationDTO convertToDTO(Quotation q) {
        String customerName = customerRepository.findById(q.getCustomerId())
            .map(c -> c.getFirstName() + " " + c.getLastName()).orElse("");
        String channelName = q.getSourceChannelId() != null
            ? channelRepository.findById(q.getSourceChannelId()).map(Channel::getDisplayName).orElse(null) : null;
        String discountCodeVal = q.getDiscountCodeId() != null
            ? discountCodeRepository.findById(q.getDiscountCodeId()).map(DiscountCode::getCode).orElse(null) : null;

        return QuotationDTO.builder()
            .id(q.getId())
            .quotationCode(q.getQuotationCode())
            .orderId(q.getOrderId())
            .customerId(q.getCustomerId())
            .customerName(customerName)
            .orderType(q.getOrderType())
            .fabricMaterial(q.getFabricMaterial())
            .fabricColor(q.getFabricColor())
            .accessories(q.getAccessories())
            .quantity(q.getQuantity())
            .unitPrice(q.getUnitPrice())
            .fabricCost(q.getFabricCost())
            .accessoriesCost(q.getAccessoriesCost())
            .laborCost(q.getLaborCost())
            .subtotal(q.getSubtotal())
            .discountCodeId(q.getDiscountCodeId())
            .discountCodeValue(discountCodeVal)
            .discountAmount(q.getDiscountAmount())
            .totalAmount(q.getTotalAmount())
            .sourceChannelId(q.getSourceChannelId())
            .sourceChannelName(channelName)
            .status(q.getStatus())
            .validUntil(q.getValidUntil())
            .notes(q.getNotes())
            .createdAt(q.getCreatedAt())
            .updatedAt(q.getUpdatedAt())
            .createdBy(q.getCreatedBy())
            .build();
    }

    private StaffCommissionDTO convertCommissionToDTO(StaffCommission sc) {
        String staffName = staffRepository.findById(sc.getStaffId())
            .map(Staff::getFullName).orElse("");
        String orderCode = orderRepository.findById(sc.getOrderId())
            .map(TailoringOrder::getOrderCode).orElse("");
        return StaffCommissionDTO.builder()
            .id(sc.getId())
            .orderId(sc.getOrderId())
            .orderCode(orderCode)
            .staffId(sc.getStaffId())
            .staffName(staffName)
            .staffRoleType(sc.getStaffRoleType())
            .commissionRate(sc.getCommissionRate())
            .commissionBase(sc.getCommissionBase())
            .commissionAmount(sc.getCommissionAmount())
            .isManualOverride(sc.getIsManualOverride())
            .overrideReason(sc.getOverrideReason())
            .isPaid(sc.getIsPaid())
            .paidAt(sc.getPaidAt())
            .createdAt(sc.getCreatedAt())
            .build();
    }
}
