package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.DeliveryDTO;
import com.tailorshop.metric.dto.QCCheckDTO;
import com.tailorshop.metric.dto.QCItemDTO;
import com.tailorshop.metric.entity.*;
import com.tailorshop.metric.exception.BusinessException;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * QCService — Kiểm soát Chất lượng (QC) & Giao hàng (Phân hệ 5)
 *
 * ─── Luồng QC ────────────────────────────────────────────────────────────
 *  1. createQCCheck(orderId)    — tạo phiếu QC với 8 tiêu chí mặc định [PENDING]
 *  2. startQC(qcId)             — bắt đầu kiểm [PENDING → IN_PROGRESS]
 *  3. updateItemResult(itemId)  — đánh giá PASS/FAIL/NA từng tiêu chí
 *  4. finalizeQC(qcId)          — tổng hợp kết quả → PASSED hoặc FAILED
 *  5. (nếu FAILED) createRecheck(orderId) — tạo phiếu QC lần tiếp theo
 *
 * ─── Luồng Giao hàng (5.3) ───────────────────────────────────────────────
 *  1. createDelivery(dto)       — assertQCPassed trước, tạo phiếu giao [SCHEDULED]
 *  2. setOutForDelivery(id)     — SCHEDULED → OUT_FOR_DELIVERY (đang giao)
 *  3. confirmDelivered(id)      — xác nhận giao xong:
 *       a. Thu phần tiền còn lại → ghi Payment
 *       b. Cập nhật order.status = COMPLETED, paymentStatus = PAID/PARTIAL
 *       c. Cập nhật production DELIVERY stage = COMPLETED (nếu có)
 *  4. markReturned(id)          — DELIVERED/OUT_FOR_DELIVERY → RETURNED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QCService {

    private final QCCheckRepository          qcCheckRepository;
    private final QCItemRepository           qcItemRepository;
    private final DeliveryRepository         deliveryRepository;
    private final TailoringOrderRepository   orderRepository;
    private final PaymentRepository          paymentRepository;
    private final ProductionStageRepository  productionStageRepository;
    private final UserRepository             userRepository;

    // ─── QC tiêu chí mặc định ────────────────────────────────────────────

    /** Danh sách 8 tiêu chí QC mặc định */
    private static final Object[][] DEFAULT_ITEMS = {
        // { itemCode, itemName, category, description, sortOrder }
        {"CHI_THUA",        "Chỉ thừa / chỉ bung",               "THREAD",       "Kiểm tra không còn chỉ thừa, các đường may chắc chắn, không bị bung mũi chỉ", 1},
        {"PHAN_KE",         "Phấn kẻ còn dính",                  "CHALK_MARK",   "Đảm bảo không còn vết phấn kẻ nhìn thấy được trên vải sau khi hoàn thiện",   2},
        {"VAI_CHAT_LUONG",  "Chất lượng / màu vải",               "FABRIC",       "Vải đúng loại, đúng màu, không bị lỗi sợi, không ngả màu",                  3},
        {"DO_MAY_CHUAN",    "Đường may thẳng & chính xác",        "STITCHING",    "Các đường may thẳng, đồng đều, không bị lệch, không bị nhăn",                4},
        {"SO_DO_KHOP",      "Số đo khớp yêu cầu khách",           "MEASUREMENT",  "Đo kiểm thực tế khớp với số đo đã ghi trong phiếu đo của khách",             5},
        {"PHU_LIEU_DAY_DU", "Phụ liệu đầy đủ",                    "ACCESSORIES",  "Nút, khóa kéo, lót, vai đệm đầy đủ, đúng chủng loại & màu sắc",             6},
        {"VE_SINH",         "Vệ sinh sản phẩm",                   "FINISHING",    "Sản phẩm sạch, không bám bẩn, xơ vải, không có mùi bất thường",              7},
        {"UI_PHANG",        "Ủi phẳng & bóng đẹp",                "FINISHING",    "Sản phẩm được ủi phẳng, form đẹp, không bị bóng sáng do ủi quá nhiệt",       8},
    };

    private static final Map<String, String> STATUS_LABELS = Map.of(
        "PENDING",            "Chờ kiểm tra",
        "IN_PROGRESS",        "Đang kiểm tra",
        "PASSED",             "Đạt chất lượng",
        "FAILED",             "Không đạt"
    );

    private static final Map<String, String> RESULT_LABELS = Map.of(
        "PASS", "Đạt",
        "FAIL", "Không đạt",
        "NA",   "Không áp dụng"
    );

    private static final Map<String, String> CATEGORY_LABELS = Map.of(
        "THREAD",      "Chỉ may",
        "CHALK_MARK",  "Phấn kẻ",
        "FABRIC",      "Chất liệu vải",
        "STITCHING",   "Đường may",
        "MEASUREMENT", "Số đo",
        "ACCESSORIES", "Phụ liệu",
        "FINISHING",   "Hoàn thiện",
        "OTHER",       "Khác"
    );

    private static final Map<String, String> DELIVERY_STATUS_LABELS = Map.of(
        "SCHEDULED",         "Đã lên lịch",
        "OUT_FOR_DELIVERY",  "Đang giao",
        "DELIVERED",         "Đã giao",
        "RETURNED",          "Trả lại",
        "CANCELLED",         "Đã hủy"
    );

    private static final Map<String, String> METHOD_LABELS = Map.of(
        "PICKUP",         "Khách tự đến lấy",
        "SHIP",           "Gửi qua vận chuyển",
        "STAFF_DELIVERY", "Nhân viên đem đến"
    );

    // ════════════════════════════════════════════════════════════════════════
    // PHẦN 1 — QC CHECKLIST
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Tạo phiếu QC mới với 8 tiêu chí mặc định.
     * Mỗi đơn hàng có thể có nhiều phiếu (re-check sau khi sửa lỗi).
     */
    @Transactional
    public QCCheckDTO createQCCheck(Long orderId, Long userId) {
        TailoringOrder order = findOrder(orderId);

        // Tính lần kiểm tra thứ mấy
        int currentRound = (int) qcCheckRepository.findByOrderIdOrderByCheckRoundDesc(orderId).stream().count();
        int nextRound = currentRound + 1;

        // Tạo phiếu QC
        QCCheck qcCheck = new QCCheck();
        qcCheck.setOrderId(orderId);
        qcCheck.setQcNumber(generateQcNumber(orderId, nextRound));
        qcCheck.setStatus("PENDING");
        qcCheck.setCheckRound(nextRound);

        QCCheck saved = qcCheckRepository.save(qcCheck);

        // Tạo 8 items mặc định
        List<QCItem> items = new ArrayList<>();
        for (Object[] row : DEFAULT_ITEMS) {
            QCItem item = new QCItem();
            item.setQcCheckId(saved.getId());
            item.setOrderId(orderId);
            item.setItemCode((String) row[0]);
            item.setItemName((String) row[1]);
            item.setCategory((String) row[2]);
            item.setDescription((String) row[3]);
            item.setResult("NA");
            item.setSortOrder((Integer) row[4]);
            items.add(item);
        }
        qcItemRepository.saveAll(items);

        log.info("Tạo phiếu QC {} (round {}) cho đơn hàng {}", saved.getQcNumber(), nextRound, orderId);
        return convertCheckToDTO(saved, order, qcItemRepository.findByQcCheckIdOrderBySortOrderAsc(saved.getId()));
    }

    /**
     * Bắt đầu kiểm tra: PENDING → IN_PROGRESS
     */
    @Transactional
    public QCCheckDTO startQC(Long qcId, Long userId) {
        QCCheck qc = findQcCheck(qcId);
        if (!"PENDING".equals(qc.getStatus())) {
            throw new BusinessException("INVALID_QC_STATUS", "Phiếu QC phải ở trạng thái PENDING mới có thể bắt đầu.");
        }
        qc.setStatus("IN_PROGRESS");
        qc.setCheckedBy(userId);
        qc.setCheckedAt(LocalDateTime.now());
        QCCheck saved = qcCheckRepository.save(qc);
        TailoringOrder order = findOrder(saved.getOrderId());
        List<QCItem> items = qcItemRepository.findByQcCheckIdOrderBySortOrderAsc(saved.getId());
        return convertCheckToDTO(saved, order, items);
    }

    /**
     * Cập nhật kết quả 1 tiêu chí: PASS | FAIL | NA.
     * Nếu result = FAIL thì failNote bắt buộc.
     */
    @Transactional
    public QCItemDTO updateItemResult(Long itemId, String result, String failNote, Long userId) {
        QCItem item = qcItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tiêu chí QC id=" + itemId));

        if ("FAIL".equals(result) && (failNote == null || failNote.isBlank())) {
            throw new BusinessException("FAIL_NOTE_REQUIRED", "Phải ghi rõ lỗi khi đánh dấu tiêu chí Không đạt.");
        }

        // Kiểm tra phiếu QC đang IN_PROGRESS
        QCCheck qc = findQcCheck(item.getQcCheckId());
        if (!"IN_PROGRESS".equals(qc.getStatus())) {
            throw new BusinessException("QC_NOT_IN_PROGRESS", "Phiếu QC không ở trạng thái đang kiểm tra.");
        }

        item.setResult(result);
        item.setFailNote("FAIL".equals(result) ? failNote : null);
        item.setCheckedBy(userId);
        item.setCheckedAt(LocalDateTime.now());
        return convertItemToDTO(qcItemRepository.save(item));
    }

    /**
     * Tổng hợp kết quả QC:
     *   - Nếu có item FAIL → FAILED
     *   - Nếu tất cả items PASS hoặc NA → PASSED
     * Cập nhật overall_result và status của phiếu QC.
     */
    @Transactional
    public QCCheckDTO finalizeQC(Long qcId, Long userId, String overallNotes, String internalNotes) {
        QCCheck qc = findQcCheck(qcId);
        if (!"IN_PROGRESS".equals(qc.getStatus())) {
            throw new BusinessException("QC_NOT_IN_PROGRESS", "Phiếu QC phải đang ở trạng thái Đang kiểm tra.");
        }

        boolean hasFail = qcItemRepository.existsByQcCheckIdAndResult(qcId, "FAIL");
        String result = hasFail ? "FAIL" : "PASS";
        String newStatus = hasFail ? "FAILED" : "PASSED";

        qc.setOverallResult(result);
        qc.setStatus(newStatus);
        qc.setOverallNotes(overallNotes);
        qc.setInternalNotes(internalNotes);
        qc.setApprovedBy(userId);
        qc.setApprovedAt(LocalDateTime.now());

        QCCheck saved = qcCheckRepository.save(qc);
        TailoringOrder order = findOrder(saved.getOrderId());
        List<QCItem> items = qcItemRepository.findByQcCheckIdOrderBySortOrderAsc(saved.getId());

        log.info("Phiếu QC {} kết quả: {} ({})", saved.getQcNumber(), result, newStatus);
        return convertCheckToDTO(saved, order, items);
    }

    /**
     * Supervisor override kết quả QC thủ công (đặc biệt khi cần phê duyệt bypass).
     */
    @Transactional
    public QCCheckDTO approveQCOverride(Long qcId, String overallResult, Long userId, String notes) {
        QCCheck qc = findQcCheck(qcId);
        String newStatus = "PASS".equals(overallResult) ? "PASSED" : "FAILED";
        qc.setOverallResult(overallResult);
        qc.setStatus(newStatus);
        qc.setApprovedBy(userId);
        qc.setApprovedAt(LocalDateTime.now());
        if (notes != null) qc.setInternalNotes(notes);
        QCCheck saved = qcCheckRepository.save(qc);
        TailoringOrder order = findOrder(saved.getOrderId());
        List<QCItem> items = qcItemRepository.findByQcCheckIdOrderBySortOrderAsc(saved.getId());
        return convertCheckToDTO(saved, order, items);
    }

    // ─── QC Gate ─────────────────────────────────────────────────────────

    /**
     * Kiểm tra đơn hàng có phiếu QC đã PASSED không.
     * Phải gọi trước khi tạo Delivery.
     */
    public void assertQCPassed(Long orderId) {
        boolean passed = qcCheckRepository.existsByOrderIdAndStatus(orderId, "PASSED");
        if (!passed) {
            throw new BusinessException("QC_NOT_PASSED",
                "Đơn hàng chưa có phiếu QC đạt chất lượng. Không thể tạo phiếu giao hàng.");
        }
    }

    // ─── Đọc dữ liệu QC ──────────────────────────────────────────────────

    public QCCheckDTO getQCCheckById(Long qcId) {
        QCCheck qc = findQcCheck(qcId);
        TailoringOrder order = findOrder(qc.getOrderId());
        List<QCItem> items = qcItemRepository.findByQcCheckIdOrderBySortOrderAsc(qcId);
        return convertCheckToDTO(qc, order, items);
    }

    public List<QCCheckDTO> getQCChecksByOrder(Long orderId) {
        TailoringOrder order = findOrder(orderId);
        return qcCheckRepository.findByOrderIdOrderByCheckRoundDesc(orderId)
            .stream()
            .map(qc -> convertCheckToDTO(qc, order,
                qcItemRepository.findByQcCheckIdOrderBySortOrderAsc(qc.getId())))
            .collect(Collectors.toList());
    }

    public Page<QCCheckDTO> listQCChecks(String status, Pageable pageable) {
        Page<QCCheck> page = (status != null && !status.isBlank())
            ? qcCheckRepository.findByStatus(status, pageable)
            : qcCheckRepository.findAll(pageable);
        List<QCCheckDTO> dtos = page.getContent().stream()
            .map(qc -> {
                TailoringOrder order = orderRepository.findById(qc.getOrderId()).orElse(null);
                List<QCItem> items = qcItemRepository.findByQcCheckIdOrderBySortOrderAsc(qc.getId());
                return convertCheckToDTO(qc, order, items);
            }).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public Map<String, Long> getQCOverview() {
        return Map.of(
            "PENDING",     qcCheckRepository.countByStatus("PENDING"),
            "IN_PROGRESS", qcCheckRepository.countByStatus("IN_PROGRESS"),
            "PASSED",      qcCheckRepository.countByStatus("PASSED"),
            "FAILED",      qcCheckRepository.countByStatus("FAILED")
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    // PHẦN 2 — GIAO HÀNG & TẤT TOÁN (5.3)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Tạo phiếu giao hàng.
     * Yêu cầu: đơn hàng phải có phiếu QC đã PASSED.
     */
    @Transactional
    public DeliveryDTO createDelivery(DeliveryDTO dto) {
        Long orderId = dto.getOrderId();
        TailoringOrder order = findOrder(orderId);

        // QC gate
        assertQCPassed(orderId);

        // Chỉ cho 1 phiếu giao đang hoạt động tại 1 thời điểm
        if (deliveryRepository.existsByOrderIdAndStatus(orderId, "SCHEDULED")
            || deliveryRepository.existsByOrderIdAndStatus(orderId, "OUT_FOR_DELIVERY")) {
            throw new BusinessException("DELIVERY_ALREADY_ACTIVE",
                "Đơn hàng đã có phiếu giao đang hoạt động. Hủy phiếu cũ trước khi tạo mới.");
        }

        // Tìm phiếu QC PASSED mới nhất để liên kết
        QCCheck passedQC = qcCheckRepository
            .findTopByOrderIdAndStatusOrderByCheckRoundDesc(orderId, "PASSED")
            .orElse(null);

        // Tính số tiền còn lại
        BigDecimal totalPaid = calcTotalPaid(orderId);
        BigDecimal totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal remaining = totalPrice.subtract(totalPaid).max(BigDecimal.ZERO);

        Delivery delivery = new Delivery();
        delivery.setOrderId(orderId);
        delivery.setQcCheckId(passedQC != null ? passedQC.getId() : null);
        delivery.setDeliveryCode(generateDeliveryCode());
        delivery.setStatus("SCHEDULED");
        delivery.setScheduledDate(dto.getScheduledDate());
        delivery.setDeliveryMethod(dto.getDeliveryMethod() != null ? dto.getDeliveryMethod() : "PICKUP");
        delivery.setRecipientName(dto.getRecipientName() != null ? dto.getRecipientName()
            : order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName());
        delivery.setRecipientPhone(dto.getRecipientPhone());
        delivery.setDeliveryAddress(dto.getDeliveryAddress());
        delivery.setRemainingAmount(dto.getRemainingAmount() != null ? dto.getRemainingAmount() : remaining);
        delivery.setAmountCollected(BigDecimal.ZERO);
        delivery.setIsFullyPaid(false);
        delivery.setNotes(dto.getNotes());

        Delivery saved = deliveryRepository.save(delivery);
        log.info("Tạo phiếu giao hàng {} cho đơn {}", saved.getDeliveryCode(), orderId);
        return convertDeliveryToDTO(saved, order);
    }

    /**
     * Cập nhật trạng thái giao hàng: SCHEDULED → OUT_FOR_DELIVERY
     */
    @Transactional
    public DeliveryDTO setOutForDelivery(Long deliveryId, Long userId) {
        Delivery delivery = findDelivery(deliveryId);
        if (!"SCHEDULED".equals(delivery.getStatus())) {
            throw new BusinessException("INVALID_DELIVERY_STATUS", "Phiếu giao phải ở trạng thái Đã lên lịch.");
        }
        delivery.setStatus("OUT_FOR_DELIVERY");
        TailoringOrder order = findOrder(delivery.getOrderId());
        return convertDeliveryToDTO(deliveryRepository.save(delivery), order);
    }

    /**
     * Xác nhận giao hàng thành công & tất toán (5.3):
     *  1. Ghi nhận amountCollected
     *  2. Tạo Payment record nếu amountCollected > 0
     *  3. Cập nhật order: status = COMPLETED, completedDate = today, paymentStatus = PAID/PARTIAL
     *  4. Cập nhật production DELIVERY stage = COMPLETED nếu có
     */
    @Transactional
    public DeliveryDTO confirmDelivered(Long deliveryId, BigDecimal amountCollected,
                                        String paymentMethod, Long userId, String notes) {
        Delivery delivery = findDelivery(deliveryId);
        if (!"SCHEDULED".equals(delivery.getStatus()) && !"OUT_FOR_DELIVERY".equals(delivery.getStatus())) {
            throw new BusinessException("INVALID_DELIVERY_STATUS", "Phiếu giao phải ở trạng thái Đã lên lịch hoặc Đang giao.");
        }

        TailoringOrder order = findOrder(delivery.getOrderId());
        BigDecimal collected = amountCollected != null ? amountCollected : BigDecimal.ZERO;

        // 1. Cập nhật delivery
        delivery.setStatus("DELIVERED");
        delivery.setActualDeliveryDate(LocalDate.now());
        delivery.setAmountCollected(collected);
        delivery.setPaymentMethod(paymentMethod);
        delivery.setConfirmedBy(userId);
        delivery.setConfirmedAt(LocalDateTime.now());
        if (notes != null) delivery.setNotes(notes);

        // 2. Tạo Payment record nếu thu tiền
        if (collected.compareTo(BigDecimal.ZERO) > 0) {
            Payment payment = new Payment();
            payment.setTailoringOrder(order);
            payment.setAmount(collected);
            payment.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH");
            payment.setTransactionReference("DEL-" + delivery.getDeliveryCode());
            payment.setPaymentDate(LocalDateTime.now());
            payment.setNotes("Thu tiền khi giao hàng — " + delivery.getDeliveryCode());
            payment.setCreatedBy(userId);
            paymentRepository.save(payment);
            log.info("Tạo Payment {} VNĐ khi giao hàng {}", collected, delivery.getDeliveryCode());
        }

        // 3. Tính lại tổng đã thanh toán
        BigDecimal totalPaid = calcTotalPaid(delivery.getOrderId()); // bao gồm payment vừa tạo
        BigDecimal totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        boolean isFullyPaid = totalPaid.compareTo(totalPrice) >= 0;

        delivery.setIsFullyPaid(isFullyPaid);
        delivery.setRemainingAmount(totalPrice.subtract(totalPaid).max(BigDecimal.ZERO));

        // 4. Cập nhật đơn hàng → COMPLETED
        order.setStatus("COMPLETED");
        order.setCompletedDate(LocalDate.now());
        order.setPaymentStatus(isFullyPaid ? "PAID" : (totalPaid.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "UNPAID"));
        orderRepository.save(order);
        log.info("Đơn hàng {} → COMPLETED, paymentStatus={}", order.getOrderCode(), order.getPaymentStatus());

        // 5. Cập nhật production stage DELIVERY → COMPLETED (nếu có)
        productionStageRepository.findByOrderIdAndStageType(order.getId(), "DELIVERY")
            .ifPresent(stage -> {
                if (!"COMPLETED".equals(stage.getStatus())) {
                    stage.setStatus("COMPLETED");
                    stage.setAlertStatus("GREEN");
                    stage.setActualEndDate(LocalDate.now());
                    stage.setCompletedBy(userId);
                    stage.setCompletedAt(LocalDateTime.now());
                    productionStageRepository.save(stage);
                    log.info("Production stage DELIVERY → COMPLETED cho đơn {}", order.getOrderCode());
                }
            });

        Delivery saved = deliveryRepository.save(delivery);
        return convertDeliveryToDTO(saved, order);
    }

    /**
     * Đánh dấu trả lại hàng.
     */
    @Transactional
    public DeliveryDTO markReturned(Long deliveryId, String returnNotes, Long userId) {
        Delivery delivery = findDelivery(deliveryId);
        if ("DELIVERED".equals(delivery.getStatus()) || "CANCELLED".equals(delivery.getStatus())) {
            throw new BusinessException("INVALID_DELIVERY_STATUS", "Không thể đánh dấu trả lại khi đã xác nhận giao hoặc đã hủy.");
        }
        delivery.setStatus("RETURNED");
        delivery.setNotes(returnNotes);
        TailoringOrder order = findOrder(delivery.getOrderId());
        return convertDeliveryToDTO(deliveryRepository.save(delivery), order);
    }

    /**
     * Hủy phiếu giao hàng (chưa giao đi).
     */
    @Transactional
    public DeliveryDTO cancelDelivery(Long deliveryId, Long userId) {
        Delivery delivery = findDelivery(deliveryId);
        if ("DELIVERED".equals(delivery.getStatus())) {
            throw new BusinessException("CANNOT_CANCEL_DELIVERED", "Không thể hủy phiếu đã giao thành công.");
        }
        delivery.setStatus("CANCELLED");
        TailoringOrder order = findOrder(delivery.getOrderId());
        return convertDeliveryToDTO(deliveryRepository.save(delivery), order);
    }

    // ─── Đọc dữ liệu Delivery ────────────────────────────────────────────

    public DeliveryDTO getDeliveryById(Long id) {
        Delivery d = findDelivery(id);
        TailoringOrder order = findOrder(d.getOrderId());
        return convertDeliveryToDTO(d, order);
    }

    public List<DeliveryDTO> getDeliveriesByOrder(Long orderId) {
        TailoringOrder order = findOrder(orderId);
        return deliveryRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
            .stream().map(d -> convertDeliveryToDTO(d, order)).collect(Collectors.toList());
    }

    public Page<DeliveryDTO> listDeliveries(String status, Pageable pageable) {
        Page<Delivery> page = (status != null && !status.isBlank())
            ? deliveryRepository.findByStatus(status, pageable)
            : deliveryRepository.findAll(pageable);
        List<DeliveryDTO> dtos = page.getContent().stream()
            .map(d -> convertDeliveryToDTO(d, orderRepository.findById(d.getOrderId()).orElse(null)))
            .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public List<DeliveryDTO> getTodayPendingDeliveries() {
        return deliveryRepository.findTodayPending(LocalDate.now())
            .stream()
            .map(d -> convertDeliveryToDTO(d, orderRepository.findById(d.getOrderId()).orElse(null)))
            .collect(Collectors.toList());
    }

    public Map<String, Long> getDeliveryOverview() {
        return Map.of(
            "SCHEDULED",        deliveryRepository.countByStatus("SCHEDULED"),
            "OUT_FOR_DELIVERY", deliveryRepository.countByStatus("OUT_FOR_DELIVERY"),
            "DELIVERED",        deliveryRepository.countByStatus("DELIVERED"),
            "RETURNED",         deliveryRepository.countByStatus("RETURNED")
        );
    }

    // ─── Internal helpers ─────────────────────────────────────────────────

    private TailoringOrder findOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng id=" + id));
    }

    private QCCheck findQcCheck(Long id) {
        return qcCheckRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu QC id=" + id));
    }

    private Delivery findDelivery(Long id) {
        return deliveryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu giao hàng id=" + id));
    }

    /** Tổng tiền đã thanh toán cho đơn hàng (kể cả deposit + tất cả payments) */
    private BigDecimal calcTotalPaid(Long orderId) {
        TailoringOrder order = orderRepository.findById(orderId).orElse(null);
        BigDecimal deposit = (order != null && order.getDepositAmount() != null
            && "CONFIRMED".equals(order.getDepositStatus())) ? order.getDepositAmount() : BigDecimal.ZERO;
        BigDecimal payments = paymentRepository.findByTailoringOrderId(orderId)
            .stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return deposit.add(payments);
    }

    private String generateQcNumber(Long orderId, int round) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "QC-" + date + "-" + String.format("%05d", orderId) + "-R" + round;
    }

    private String generateDeliveryCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "DEL-" + date + "-" + System.currentTimeMillis() % 100000;
    }

    // ─── Converters ──────────────────────────────────────────────────────

    public QCCheckDTO convertCheckToDTO(QCCheck qc, TailoringOrder order, List<QCItem> items) {
        String customerName = order != null && order.getCustomer() != null
            ? order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName() : null;

        String checkedByName = qc.getCheckedBy() != null
            ? userRepository.findById(qc.getCheckedBy()).map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null) : null;
        String approvedByName = qc.getApprovedBy() != null
            ? userRepository.findById(qc.getApprovedBy()).map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null) : null;

        List<QCItemDTO> itemDtos = items != null ? items.stream().map(this::convertItemToDTO).collect(Collectors.toList()) : List.of();

        long passCount = items != null ? items.stream().filter(i -> "PASS".equals(i.getResult())).count() : 0;
        long failCount = items != null ? items.stream().filter(i -> "FAIL".equals(i.getResult())).count() : 0;
        long naCount   = items != null ? items.stream().filter(i -> "NA".equals(i.getResult())).count() : 0;

        return QCCheckDTO.builder()
            .id(qc.getId())
            .orderId(qc.getOrderId())
            .orderCode(order != null ? order.getOrderCode() : null)
            .customerName(customerName)
            .qcNumber(qc.getQcNumber())
            .status(qc.getStatus())
            .statusLabel(STATUS_LABELS.getOrDefault(qc.getStatus(), qc.getStatus()))
            .overallResult(qc.getOverallResult())
            .overallResultLabel(qc.getOverallResult() != null ? RESULT_LABELS.getOrDefault(qc.getOverallResult(), qc.getOverallResult()) : null)
            .checkedBy(qc.getCheckedBy())
            .checkedByName(checkedByName)
            .checkedAt(qc.getCheckedAt())
            .approvedBy(qc.getApprovedBy())
            .approvedByName(approvedByName)
            .approvedAt(qc.getApprovedAt())
            .checkRound(qc.getCheckRound())
            .overallNotes(qc.getOverallNotes())
            .internalNotes(qc.getInternalNotes())
            .totalItems(items != null ? items.size() : 0)
            .passCount((int) passCount)
            .failCount((int) failCount)
            .naCount((int) naCount)
            .items(itemDtos)
            .createdAt(qc.getCreatedAt())
            .updatedAt(qc.getUpdatedAt())
            .build();
    }

    public QCItemDTO convertItemToDTO(QCItem item) {
        String checkedByName = item.getCheckedBy() != null
            ? userRepository.findById(item.getCheckedBy())
                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null) : null;
        return QCItemDTO.builder()
            .id(item.getId())
            .qcCheckId(item.getQcCheckId())
            .orderId(item.getOrderId())
            .itemCode(item.getItemCode())
            .itemName(item.getItemName())
            .category(item.getCategory())
            .categoryLabel(CATEGORY_LABELS.getOrDefault(item.getCategory(), item.getCategory()))
            .description(item.getDescription())
            .result(item.getResult())
            .resultLabel(RESULT_LABELS.getOrDefault(item.getResult(), item.getResult()))
            .failNote(item.getFailNote())
            .imageUrl(item.getImageUrl())
            .checkedBy(item.getCheckedBy())
            .checkedByName(checkedByName)
            .checkedAt(item.getCheckedAt())
            .sortOrder(item.getSortOrder())
            .createdAt(item.getCreatedAt())
            .build();
    }

    public DeliveryDTO convertDeliveryToDTO(Delivery d, TailoringOrder order) {
        String customerName = order != null && order.getCustomer() != null
            ? order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName() : null;
        String confirmedByName = d.getConfirmedBy() != null
            ? userRepository.findById(d.getConfirmedBy()).map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null) : null;
        QCCheck qc = d.getQcCheckId() != null ? qcCheckRepository.findById(d.getQcCheckId()).orElse(null) : null;

        return DeliveryDTO.builder()
            .id(d.getId())
            .orderId(d.getOrderId())
            .orderCode(order != null ? order.getOrderCode() : null)
            .customerName(customerName)
            .totalPrice(order != null ? order.getTotalPrice() : null)
            .paymentStatus(order != null ? order.getPaymentStatus() : null)
            .qcCheckId(d.getQcCheckId())
            .qcNumber(qc != null ? qc.getQcNumber() : null)
            .deliveryCode(d.getDeliveryCode())
            .status(d.getStatus())
            .statusLabel(DELIVERY_STATUS_LABELS.getOrDefault(d.getStatus(), d.getStatus()))
            .scheduledDate(d.getScheduledDate())
            .actualDeliveryDate(d.getActualDeliveryDate())
            .deliveryMethod(d.getDeliveryMethod())
            .deliveryMethodLabel(METHOD_LABELS.getOrDefault(d.getDeliveryMethod(), d.getDeliveryMethod()))
            .recipientName(d.getRecipientName())
            .recipientPhone(d.getRecipientPhone())
            .deliveryAddress(d.getDeliveryAddress())
            .remainingAmount(d.getRemainingAmount())
            .amountCollected(d.getAmountCollected())
            .paymentMethod(d.getPaymentMethod())
            .isFullyPaid(d.getIsFullyPaid())
            .confirmedBy(d.getConfirmedBy())
            .confirmedByName(confirmedByName)
            .confirmedAt(d.getConfirmedAt())
            .receiptSignature(d.getReceiptSignature())
            .notes(d.getNotes())
            .createdAt(d.getCreatedAt())
            .updatedAt(d.getUpdatedAt())
            .build();
    }
}
