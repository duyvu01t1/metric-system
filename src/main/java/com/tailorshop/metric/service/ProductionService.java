package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.ProductionCalendarDTO;
import com.tailorshop.metric.dto.ProductionStageDTO;
import com.tailorshop.metric.dto.ProductionStageLogDTO;
import com.tailorshop.metric.entity.*;
import com.tailorshop.metric.exception.BusinessException;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ProductionService — Quản lý Sản xuất & Tiến độ (Phân hệ 4)
 *
 * Luồng chính:
 *  1. initializeStages(orderId) — tạo 4 công đoạn mặc định sau khi deposit CONFIRMED
 *  2. updateStageStatus()       — chuyển trạng thái (tuần tự: CUT→ASSEMBLE→FITTING→DELIVERY)
 *  3. assignWorkerAndSale()     — gán thợ may + nhân viên sale cho từng công đoạn
 *  4. scheduleStage()           — đặt ngày kế hoạch, tự tính alertStatus
 *  5. updateCommission()        — override thủ công hoa hồng cho công đoạn
 *  6. addNote()                 — thêm ghi chú cho công đoạn
 *  7. getCalendar*()            — lấy sự kiện calendar theo order/staff/khoảng thời gian
 *
 * Alert logic (4.6):
 *   GREEN  = chưa có planned_end_date, hoặc today < plannedEndDate - yellowThreshold
 *   YELLOW = today ≤ plannedEndDate ≤ today + yellowThreshold (gần hết hạn)
 *   RED    = today > plannedEndDate AND status ≠ COMPLETED/SKIPPED
 *
 * Calendar color mapping:
 *   GREEN  → #66bb6a
 *   YELLOW → #ffb74d
 *   RED    → #ef5350
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionService {

    // Ngưỡng cảnh báo vàng (ngày): default 3 ngày trước deadline → YELLOW
    @Value("${app.production.yellow-threshold-days:3}")
    private int yellowThresholdDays;

    private final ProductionStageRepository    stageRepository;
    private final ProductionStageLogRepository logRepository;
    private final ProductionCalendarRepository calendarRepository;
    private final TailoringOrderRepository     orderRepository;
    private final TailoringOrderService        orderService;   // dùng assertDepositConfirmed()
    private final StaffRepository              staffRepository;
    private final CustomerRepository           customerRepository;

    // ─── Tên hiển thị tĩnh ──────────────────────────────────────────────────

    private static final Map<String, String> STAGE_NAMES = Map.of(
        "CUT",      "Cắt vải",
        "ASSEMBLE", "Ráp may",
        "FITTING",  "Thử & Chỉnh",
        "DELIVERY", "Giao hàng"
    );

    private static final Map<String, String> STATUS_LABELS = Map.of(
        "PENDING",     "Chờ thực hiện",
        "IN_PROGRESS", "Đang thực hiện",
        "COMPLETED",   "Hoàn thành",
        "SKIPPED",     "Bỏ qua"
    );

    private static final Map<String, String> ALERT_COLORS = Map.of(
        "GREEN",  "#66bb6a",
        "YELLOW", "#ffb74d",
        "RED",    "#ef5350"
    );

    private static final Map<String, String> CHANGE_TYPE_LABELS = Map.of(
        "STATUS_CHANGED",     "Cập nhật trạng thái",
        "WORKER_ASSIGNED",    "Gán thợ may",
        "SALE_ASSIGNED",      "Gán nhân viên sale",
        "COMMISSION_UPDATED", "Cập nhật hoa hồng",
        "SCHEDULE_CHANGED",   "Thay đổi lịch kế hoạch",
        "NOTE_ADDED",         "Thêm ghi chú"
    );

    // ─── 1. Khởi tạo 4 công đoạn ────────────────────────────────────────────

    /**
     * Tạo 4 công đoạn mặc định cho đơn hàng.
     * Yêu cầu: depositStatus = CONFIRMED.
     */
    @Transactional
    public List<ProductionStageDTO> initializeStages(Long orderId, Long createdByUserId) {
        // Kiểm tra deposit gate
        orderService.assertDepositConfirmed(orderId);

        // Không cho tạo lại nếu đã có
        if (stageRepository.existsByOrderId(orderId)) {
            throw new BusinessException("STAGES_ALREADY_EXIST",
                "Công đoạn sản xuất đã được khởi tạo cho đơn hàng này.");
        }

        TailoringOrder order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng id=" + orderId));

        // Tạo 4 stages theo thứ tự cố định
        String[][] stageData = {
            {"CUT",      "1"},
            {"ASSEMBLE", "2"},
            {"FITTING",  "3"},
            {"DELIVERY", "4"}
        };

        List<ProductionStage> stages = new java.util.ArrayList<>();
        for (String[] data : stageData) {
            ProductionStage stage = new ProductionStage();
            stage.setOrderId(orderId);
            stage.setStageType(data[0]);
            stage.setStageOrder(Integer.parseInt(data[1]));
            stage.setStatus("PENDING");
            stage.setAlertStatus("GREEN");
            stage.setCommissionRate(BigDecimal.ZERO);
            stage.setCommissionAmount(BigDecimal.ZERO);
            stage.setIsCommissionOverride(false);
            stage.setCreatedBy(createdByUserId);
            stages.add(stage);
        }

        List<ProductionStage> saved = stageRepository.saveAll(stages);

        // Tạo ORDER-type calendar events (ngày hứa giao hàng)
        if (order.getPromisedDate() != null) {
            ProductionCalendar calEvent = buildOrderCalendarEvent(order, saved.get(3)); // delivery stage
            calendarRepository.save(calEvent);
        }

        // Log sự kiện khởi tạo cho stage đầu tiên
        writeLog(saved.get(0).getId(), orderId, "STATUS_CHANGED",
            null, "PENDING", "Khởi tạo công đoạn sản xuất", createdByUserId);

        log.info("Đã khởi tạo {} công đoạn sản xuất cho đơn hàng {}", saved.size(), orderId);
        return saved.stream().map(s -> convertStageToDTO(s, order)).collect(Collectors.toList());
    }

    // ─── 2. Cập nhật trạng thái ─────────────────────────────────────────────

    /**
     * Chuyển trạng thái công đoạn.
     * Rule tuần tự: công đoạn N chỉ được IN_PROGRESS khi công đoạn N-1 đã COMPLETED.
     */
    @Transactional
    public ProductionStageDTO updateStageStatus(Long stageId, String newStatus, Long userId, String note) {
        ProductionStage stage = findStageById(stageId);
        String oldStatus = stage.getStatus();

        validateStatusTransition(stage, newStatus);

        stage.setStatus(newStatus);
        if ("IN_PROGRESS".equals(newStatus) && stage.getActualStartDate() == null) {
            stage.setActualStartDate(LocalDate.now());
        }
        if ("COMPLETED".equals(newStatus)) {
            stage.setActualEndDate(LocalDate.now());
            stage.setCompletedBy(userId);
            stage.setCompletedAt(LocalDateTime.now());
        }

        // Tái tính alertStatus
        stage.setAlertStatus(calculateAlertStatus(stage));

        ProductionStage saved = stageRepository.save(stage);

        // Log
        writeLog(stageId, saved.getOrderId(), "STATUS_CHANGED", oldStatus, newStatus, note, userId);

        // Cập nhật màu calendar
        syncCalendarColor(saved);

        TailoringOrder order = orderRepository.findById(saved.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return convertStageToDTO(saved, order);
    }

    // ─── 3. Gán thợ may + sale ───────────────────────────────────────────────

    @Transactional
    public ProductionStageDTO assignWorkerAndSale(Long stageId, Long workerId, Long saleId, Long userId) {
        ProductionStage stage = findStageById(stageId);

        Long oldWorkerId = stage.getAssignedWorkerId();
        Long oldSaleId   = stage.getAssignedSaleId();

        if (workerId != null) {
            staffRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thợ may id=" + workerId));
            stage.setAssignedWorkerId(workerId);
        }
        if (saleId != null) {
            staffRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên sale id=" + saleId));
            stage.setAssignedSaleId(saleId);
        }

        ProductionStage saved = stageRepository.save(stage);

        if (workerId != null && !workerId.equals(oldWorkerId)) {
            writeLog(stageId, saved.getOrderId(), "WORKER_ASSIGNED",
                oldWorkerId != null ? oldWorkerId.toString() : null,
                workerId.toString(), null, userId);
        }
        if (saleId != null && !saleId.equals(oldSaleId)) {
            writeLog(stageId, saved.getOrderId(), "SALE_ASSIGNED",
                oldSaleId != null ? oldSaleId.toString() : null,
                saleId.toString(), null, userId);
        }

        // Đồng bộ calendar cho worker/sale mới
        rebuildStageCalendars(saved);

        TailoringOrder order = orderRepository.findById(saved.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return convertStageToDTO(saved, order);
    }

    // ─── 4. Đặt lịch kế hoạch ────────────────────────────────────────────────

    @Transactional
    public ProductionStageDTO scheduleStage(Long stageId, LocalDate plannedStart,
                                             LocalDate plannedEnd, Long userId) {
        ProductionStage stage = findStageById(stageId);
        String oldValue = stage.getPlannedStartDate() + " → " + stage.getPlannedEndDate();

        stage.setPlannedStartDate(plannedStart);
        stage.setPlannedEndDate(plannedEnd);
        stage.setAlertStatus(calculateAlertStatus(stage));

        ProductionStage saved = stageRepository.save(stage);

        String newValue = plannedStart + " → " + plannedEnd;
        writeLog(stageId, saved.getOrderId(), "SCHEDULE_CHANGED", oldValue, newValue, null, userId);

        // Sync calendar
        rebuildStageCalendars(saved);

        TailoringOrder order = orderRepository.findById(saved.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return convertStageToDTO(saved, order);
    }

    // ─── 5. Cập nhật hoa hồng ────────────────────────────────────────────────

    @Transactional
    public ProductionStageDTO updateCommission(Long stageId, BigDecimal rate, BigDecimal amount,
                                               String reason, Long userId) {
        ProductionStage stage = findStageById(stageId);
        String oldValue = "rate=" + stage.getCommissionRate() + ",amount=" + stage.getCommissionAmount();

        stage.setCommissionRate(rate != null ? rate : stage.getCommissionRate());
        stage.setCommissionAmount(amount != null ? amount : stage.getCommissionAmount());
        stage.setIsCommissionOverride(true);
        stage.setCommissionOverrideReason(reason);

        ProductionStage saved = stageRepository.save(stage);

        String newValue = "rate=" + saved.getCommissionRate() + ",amount=" + saved.getCommissionAmount();
        writeLog(stageId, saved.getOrderId(), "COMMISSION_UPDATED", oldValue, newValue, reason, userId);

        TailoringOrder order = orderRepository.findById(saved.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return convertStageToDTO(saved, order);
    }

    // ─── 6. Thêm ghi chú ─────────────────────────────────────────────────────

    @Transactional
    public ProductionStageDTO addNote(Long stageId, String noteText, Long userId) {
        ProductionStage stage = findStageById(stageId);
        String existing = stage.getNotes();
        // Append note với timestamp
        String combined = (existing != null && !existing.isBlank())
            ? existing + "\n[" + LocalDateTime.now().toString().substring(0, 16) + "] " + noteText
            : "[" + LocalDateTime.now().toString().substring(0, 16) + "] " + noteText;
        stage.setNotes(combined);

        ProductionStage saved = stageRepository.save(stage);
        writeLog(stageId, saved.getOrderId(), "NOTE_ADDED", null, noteText, null, userId);

        TailoringOrder order = orderRepository.findById(saved.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return convertStageToDTO(saved, order);
    }

    // ─── 7. Đọc dữ liệu ──────────────────────────────────────────────────────

    public List<ProductionStageDTO> getStagesByOrder(Long orderId) {
        TailoringOrder order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng id=" + orderId));
        return stageRepository.findByOrderIdOrderByStageOrderAsc(orderId)
            .stream()
            .map(s -> convertStageToDTO(s, order))
            .collect(Collectors.toList());
    }

    public ProductionStageDTO getStageById(Long stageId) {
        ProductionStage stage = findStageById(stageId);
        TailoringOrder order = orderRepository.findById(stage.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return convertStageToDTO(stage, order);
    }

    public List<ProductionStageLogDTO> getStageLogs(Long stageId) {
        return logRepository.findByStageIdOrderByChangedAtDesc(stageId)
            .stream()
            .map(this::convertLogToDTO)
            .collect(Collectors.toList());
    }

    // ─── 8. Calendar APIs (4.7 / 4.8) ───────────────────────────────────────

    public List<ProductionCalendarDTO> getCalendarByOrder(Long orderId) {
        return calendarRepository.findByOrderId(orderId)
            .stream().map(this::convertCalToDTO).collect(Collectors.toList());
    }

    public List<ProductionCalendarDTO> getCalendarByStaff(Long staffId,
                                                            LocalDateTime start,
                                                            LocalDateTime end) {
        return calendarRepository.findByStaffIdAndEventStartBetween(staffId, start, end)
            .stream().map(this::convertCalToDTO).collect(Collectors.toList());
    }

    public List<ProductionCalendarDTO> getCalendarAll(LocalDateTime start, LocalDateTime end) {
        return calendarRepository.findByEventStartBetween(start, end)
            .stream().map(this::convertCalToDTO).collect(Collectors.toList());
    }

    // ─── 9. Overview dashboard ────────────────────────────────────────────────

    public Map<String, Long> getAlertOverview() {
        List<ProductionStage> allActive = stageRepository.findAllPendingOrInProgress();
        long green  = allActive.stream().filter(s -> "GREEN".equals(s.getAlertStatus())).count();
        long yellow = allActive.stream().filter(s -> "YELLOW".equals(s.getAlertStatus())).count();
        long red    = allActive.stream().filter(s -> "RED".equals(s.getAlertStatus())).count();
        return Map.of("GREEN", green, "YELLOW", yellow, "RED", red);
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private ProductionStage findStageById(Long id) {
        return stageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công đoạn sản xuất id=" + id));
    }

    /**
     * Tính alertStatus dựa trên ngày kế hoạch và trạng thái hiện tại.
     */
    private String calculateAlertStatus(ProductionStage stage) {
        if ("COMPLETED".equals(stage.getStatus()) || "SKIPPED".equals(stage.getStatus())) {
            return "GREEN";
        }
        if (stage.getPlannedEndDate() == null) {
            return "GREEN";
        }
        LocalDate today = LocalDate.now();
        LocalDate deadline = stage.getPlannedEndDate();
        if (today.isAfter(deadline)) {
            return "RED";
        }
        if (!today.plusDays(yellowThresholdDays).isBefore(deadline)) {
            return "YELLOW";
        }
        return "GREEN";
    }

    /**
     * Xác thực việc chuyển trạng thái có hợp lệ không:
     * - Công đoạn N chỉ được IN_PROGRESS khi công đoạn N-1 là COMPLETED
     * - CUT (stageOrder=1) chỉ cần deposit CONFIRMED (đã được kiểm tra khi init)
     */
    private void validateStatusTransition(ProductionStage stage, String newStatus) {
        String current = stage.getStatus();
        if ("COMPLETED".equals(current) && !"SKIPPED".equals(newStatus)) {
            throw new BusinessException("INVALID_TRANSITION",
                "Công đoạn đã hoàn thành, không thể thay đổi trạng thái.");
        }
        if ("IN_PROGRESS".equals(newStatus) && stage.getStageOrder() > 1) {
            int prevOrder = stage.getStageOrder() - 1;
            Optional<ProductionStage> prevStage =
                stageRepository.findByOrderIdAndStageOrder(stage.getOrderId(), prevOrder);
            prevStage.ifPresent(prev -> {
                if (!"COMPLETED".equals(prev.getStatus()) && !"SKIPPED".equals(prev.getStatus())) {
                    throw new BusinessException("PREVIOUS_STAGE_NOT_DONE",
                        "Phải hoàn thành công đoạn '" + STAGE_NAMES.getOrDefault(prev.getStageType(), prev.getStageType())
                        + "' trước khi bắt đầu công đoạn này.");
                }
            });
        }
    }

    /** Ghi log thay đổi */
    private void writeLog(Long stageId, Long orderId, String changeType,
                          String oldValue, String newValue, String note, Long userId) {
        ProductionStageLog log = new ProductionStageLog();
        log.setStageId(stageId);
        log.setOrderId(orderId);
        log.setChangeType(changeType);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setChangeNote(note);
        log.setChangedBy(userId);
        log.setChangedAt(LocalDateTime.now());
        logRepository.save(log);
    }

    /** Xây dựng lại tất cả calendar events cho 1 stage (ORDER + WORKER + SALE) */
    private void rebuildStageCalendars(ProductionStage stage) {
        calendarRepository.deleteByStageId(stage.getId());

        if (stage.getPlannedStartDate() == null || stage.getPlannedEndDate() == null) {
            return; // Chưa có lịch kế hoạch
        }

        TailoringOrder order = orderRepository.findById(stage.getOrderId()).orElse(null);
        if (order == null) return;

        String color = ALERT_COLORS.getOrDefault(stage.getAlertStatus(), "#1976d2");
        String stageName = STAGE_NAMES.getOrDefault(stage.getStageType(), stage.getStageType());
        LocalDateTime eventStart = stage.getPlannedStartDate().atStartOfDay();
        LocalDateTime eventEnd   = stage.getPlannedEndDate().atTime(LocalTime.MAX);

        // ORDER calendar event
        ProductionCalendar orderCal = new ProductionCalendar();
        orderCal.setOrderId(order.getId());
        orderCal.setStageId(stage.getId());
        orderCal.setCalendarType("ORDER");
        orderCal.setEventTitle("[" + order.getOrderCode() + "] " + stageName);
        orderCal.setEventStart(eventStart);
        orderCal.setEventEnd(eventEnd);
        orderCal.setEventColor(color);
        orderCal.setAllDay(true);
        calendarRepository.save(orderCal);

        // WORKER calendar event
        if (stage.getAssignedWorkerId() != null) {
            String workerName = staffRepository.findById(stage.getAssignedWorkerId())
                .map(Staff::getFullName).orElse("Thợ may");
            ProductionCalendar workerCal = new ProductionCalendar();
            workerCal.setOrderId(order.getId());
            workerCal.setStageId(stage.getId());
            workerCal.setStaffId(stage.getAssignedWorkerId());
            workerCal.setCalendarType("WORKER");
            workerCal.setEventTitle("[" + workerName + "] " + stageName + " - " + order.getOrderCode());
            workerCal.setEventStart(eventStart);
            workerCal.setEventEnd(eventEnd);
            workerCal.setEventColor(color);
            workerCal.setAllDay(true);
            calendarRepository.save(workerCal);
        }

        // SALE calendar event
        if (stage.getAssignedSaleId() != null) {
            String saleName = staffRepository.findById(stage.getAssignedSaleId())
                .map(Staff::getFullName).orElse("Sale");
            ProductionCalendar saleCal = new ProductionCalendar();
            saleCal.setOrderId(order.getId());
            saleCal.setStageId(stage.getId());
            saleCal.setStaffId(stage.getAssignedSaleId());
            saleCal.setCalendarType("SALE");
            saleCal.setEventTitle("[" + saleName + "] " + stageName + " - " + order.getOrderCode());
            saleCal.setEventStart(eventStart);
            saleCal.setEventEnd(eventEnd);
            saleCal.setEventColor(color);
            saleCal.setAllDay(true);
            calendarRepository.save(saleCal);
        }
    }

    /** Chỉ cập nhật màu của các calendar events hiện có (khi alert thay đổi) */
    private void syncCalendarColor(ProductionStage stage) {
        String newColor = ALERT_COLORS.getOrDefault(stage.getAlertStatus(), "#1976d2");
        calendarRepository.findByOrderId(stage.getOrderId()).stream()
            .filter(c -> stage.getId().equals(c.getStageId()))
            .forEach(c -> {
                c.setEventColor(newColor);
                calendarRepository.save(c);
            });
    }

    /** Xây dựng calendar event tổng cho đơn hàng (promised date) */
    private ProductionCalendar buildOrderCalendarEvent(TailoringOrder order, ProductionStage deliveryStage) {
        ProductionCalendar cal = new ProductionCalendar();
        cal.setOrderId(order.getId());
        cal.setStageId(deliveryStage != null ? deliveryStage.getId() : null);
        cal.setCalendarType("ORDER");
        cal.setEventTitle("Hẹn giao: " + order.getOrderCode());
        cal.setEventStart(order.getPromisedDate().atStartOfDay());
        cal.setEventEnd(order.getPromisedDate().atTime(23, 59));
        cal.setEventColor("#1976d2");
        cal.setAllDay(true);
        return cal;
    }

    // ─── Converters ──────────────────────────────────────────────────────────

    public ProductionStageDTO convertStageToDTO(ProductionStage s, TailoringOrder order) {
        String workerName = s.getAssignedWorkerId() != null
            ? staffRepository.findById(s.getAssignedWorkerId()).map(Staff::getFullName).orElse(null) : null;
        String saleName = s.getAssignedSaleId() != null
            ? staffRepository.findById(s.getAssignedSaleId()).map(Staff::getFullName).orElse(null) : null;
        String completedByName = s.getCompletedBy() != null
            ? staffRepository.findById(s.getCompletedBy()).map(Staff::getFullName).orElse(null) : null;
        String customerName = order.getCustomer() != null
            ? order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName() : null;

        return ProductionStageDTO.builder()
            .id(s.getId())
            .orderId(s.getOrderId())
            .orderCode(order.getOrderCode())
            .customerName(customerName)
            .stageType(s.getStageType())
            .stageTypeName(STAGE_NAMES.getOrDefault(s.getStageType(), s.getStageType()))
            .stageOrder(s.getStageOrder())
            .status(s.getStatus())
            .statusLabel(STATUS_LABELS.getOrDefault(s.getStatus(), s.getStatus()))
            .assignedWorkerId(s.getAssignedWorkerId())
            .workerName(workerName)
            .assignedSaleId(s.getAssignedSaleId())
            .saleName(saleName)
            .plannedStartDate(s.getPlannedStartDate())
            .plannedEndDate(s.getPlannedEndDate())
            .actualStartDate(s.getActualStartDate())
            .actualEndDate(s.getActualEndDate())
            .alertStatus(s.getAlertStatus())
            .alertColor(ALERT_COLORS.getOrDefault(s.getAlertStatus(), "#1976d2"))
            .commissionRate(s.getCommissionRate())
            .commissionAmount(s.getCommissionAmount())
            .isCommissionOverride(s.getIsCommissionOverride())
            .commissionOverrideReason(s.getCommissionOverrideReason())
            .notes(s.getNotes())
            .createdBy(s.getCreatedBy())
            .completedBy(s.getCompletedBy())
            .completedByName(completedByName)
            .completedAt(s.getCompletedAt())
            .createdAt(s.getCreatedAt())
            .updatedAt(s.getUpdatedAt())
            .build();
    }

    private ProductionStageLogDTO convertLogToDTO(ProductionStageLog l) {
        String changedByName = l.getChangedBy() != null
            ? staffRepository.findById(l.getChangedBy()).map(Staff::getFullName).orElse("Hệ thống") : "Hệ thống";
        TailoringOrder order = orderRepository.findById(l.getOrderId()).orElse(null);
        return ProductionStageLogDTO.builder()
            .id(l.getId())
            .stageId(l.getStageId())
            .orderId(l.getOrderId())
            .orderCode(order != null ? order.getOrderCode() : null)
            .changeType(l.getChangeType())
            .changeTypeLabel(CHANGE_TYPE_LABELS.getOrDefault(l.getChangeType(), l.getChangeType()))
            .oldValue(l.getOldValue())
            .newValue(l.getNewValue())
            .changeNote(l.getChangeNote())
            .changedBy(l.getChangedBy())
            .changedByName(changedByName)
            .changedAt(l.getChangedAt())
            .build();
    }

    private ProductionCalendarDTO convertCalToDTO(ProductionCalendar c) {
        String staffName = c.getStaffId() != null
            ? staffRepository.findById(c.getStaffId()).map(Staff::getFullName).orElse(null) : null;
        TailoringOrder order = orderRepository.findById(c.getOrderId()).orElse(null);
        String orderCode = order != null ? order.getOrderCode() : null;

        ProductionStage stage = c.getStageId() != null
            ? stageRepository.findById(c.getStageId()).orElse(null) : null;
        String stageType     = stage != null ? stage.getStageType() : null;
        String stageTypeName = stageType != null ? STAGE_NAMES.getOrDefault(stageType, stageType) : null;
        String stageStatus   = stage != null ? stage.getStatus() : null;
        String alertStatus   = stage != null ? stage.getAlertStatus() : null;

        return ProductionCalendarDTO.builder()
            .id(c.getId())
            .orderId(c.getOrderId())
            .stageId(c.getStageId())
            .staffId(c.getStaffId())
            .calendarType(c.getCalendarType())
            .title(c.getEventTitle())
            .start(c.getEventStart())
            .end(c.getEventEnd())
            .color(c.getEventColor())
            .allDay(c.getAllDay())
            .orderCode(orderCode)
            .staffName(staffName)
            .stageType(stageType)
            .stageTypeName(stageTypeName)
            .stageStatus(stageStatus)
            .alertStatus(alertStatus)
            .notes(c.getNotes())
            .createdAt(c.getCreatedAt())
            .build();
    }
}
