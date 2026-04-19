package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.FollowUpLogDTO;
import com.tailorshop.metric.dto.FollowUpReminderDTO;
import com.tailorshop.metric.entity.*;
import com.tailorshop.metric.exception.BusinessException;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AfterSalesService — Chăm sóc Sau Bán Hàng (Phân hệ 6)
 *
 * ─── Luồng tự động ────────────────────────────────────────────────────────
 *  Scheduler 6h sáng mỗi ngày:
 *    → Quét các đơn COMPLETED trong vòng 12 ngày qua chưa có reminder nào
 *    → Tạo 3 reminder: DAY_3 (+3), DAY_7 (+7), DAY_10 (+10) tính từ completedDate
 *    → Gán cho nhân viên chính của đơn hàng (primaryStaffId); nếu không có → null
 *
 * ─── Luồng thủ công ──────────────────────────────────────────────────────
 *  createManualReminder()     — nhân viên tạo reminder CUSTOM
 *  autoCreateForOrder()       — gọi ngay khi confirm delivery (tránh chờ đến 6h hôm sau)
 *
 * ─── Luồng chăm sóc ──────────────────────────────────────────────────────
 *  1. Nhân viên xem getTodayReminders() → danh sách việc cần làm hôm nay
 *  2. Thực hiện liên hệ → gọi logContact() (6.4) — ghi nhận kết quả CALL/MSG
 *  3. Đánh dấu markDone() hoặc skipReminder()
 *  4. Nếu outcome = REPEAT_ORDER → tạo Lead mới (nhân viên tự xử lý trên UI)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AfterSalesService {

    private final FollowUpReminderRepository reminderRepository;
    private final FollowUpLogRepository      logRepository;
    private final TailoringOrderRepository   orderRepository;
    private final CustomerRepository         customerRepository;
    private final StaffRepository            staffRepository;
    private final UserRepository             userRepository;

    // ─── Label maps ────────────────────────────────────────────────────────

    private static final Map<String, String> REMINDER_TYPE_LABELS = Map.of(
        "DAY_3",  "3 ngày sau giao",
        "DAY_7",  "7 ngày sau giao",
        "DAY_10", "10 ngày sau giao",
        "CUSTOM", "Tùy chỉnh"
    );

    private static final Map<String, String> STATUS_LABELS = Map.of(
        "PENDING",   "Chờ thực hiện",
        "DONE",      "Đã hoàn thành",
        "SKIPPED",   "Bỏ qua",
        "CANCELLED", "Đã hủy"
    );

    private static final Map<String, String> PRIORITY_LABELS = Map.of(
        "HIGH",   "Cao",
        "MEDIUM", "Trung bình",
        "LOW",    "Thấp"
    );

    private static final Map<String, String> CONTACT_TYPE_LABELS = Map.of(
        "CALL",     "Gọi điện",
        "MESSAGE",  "Tin nhắn",
        "EMAIL",    "Email",
        "VISIT",    "Ghé thăm",
        "ZALO",     "Zalo",
        "FACEBOOK", "Facebook"
    );

    private static final Map<String, String> OUTCOME_LABELS = Map.of(
        "ANSWERED",       "Có phản hồi",
        "NO_ANSWER",      "Không liên lạc được",
        "CALLBACK",       "Sẽ gọi lại",
        "LEFT_MESSAGE",   "Để lại tin nhắn",
        "SATISFIED",      "Hài lòng",
        "COMPLAINED",     "Có khiếu nại",
        "REPEAT_ORDER",   "Muốn đặt thêm",
        "NOT_INTERESTED", "Không có nhu cầu"
    );

    // ════════════════════════════════════════════════════════════════════════
    // PHẦN 1 — TỰ ĐỘNG TẠO REMINDER (SCHEDULER - 6.2)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Scheduler chạy lúc 6:00 AM mỗi ngày.
     * Quét các đơn COMPLETED trong vòng 12 ngày qua và tạo reminders tự động.
     * Sử dụng constraint UNIQUE (order_id, reminder_type) để tránh trùng lặp.
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void scheduledAutoCreateReminders() {
        LocalDate today = LocalDate.now();
        // Quét đơn completed từ 12 ngày trước (đảm bảo bắt được đơn cần tạo DAY_10)
        LocalDate from = today.minusDays(12);

        List<TailoringOrder> completedOrders =
            orderRepository.findByStatusAndCompletedDateBetween("COMPLETED", from, today);

        int created = 0;
        for (TailoringOrder order : completedOrders) {
            try {
                int count = autoCreateForOrder(order, null);
                created += count;
            } catch (Exception e) {
                log.error("Lỗi tạo reminder tự động cho đơn {}: {}", order.getOrderCode(), e.getMessage());
            }
        }
        if (created > 0) {
            log.info("[Scheduler After-Sales] Đã tạo {} reminders mới cho {} đơn hàng hoàn thành.",
                created, completedOrders.size());
        }
    }

    /**
     * Tạo reminders tự động cho 1 đơn hàng vừa hoàn thành.
     * Có thể gọi trực tiếp từ QCService.confirmDelivered() để không phải chờ đến 6h sáng.
     *
     * @param order     Đơn hàng đã COMPLETED
     * @param createdBy userId của người kích hoạt (null nếu từ scheduler)
     * @return Số reminder đã tạo mới
     */
    @Transactional
    public int autoCreateForOrder(TailoringOrder order, Long createdBy) {
        if (order.getCompletedDate() == null) return 0;

        // Loại reminder cần tạo: { type, offsetDays, priority }
        Object[][] types = {
            {"DAY_3",  3,  "HIGH"},
            {"DAY_7",  7,  "MEDIUM"},
            {"DAY_10", 10, "LOW"}
        };

        int created = 0;
        for (Object[] t : types) {
            String type     = (String) t[0];
            int offsetDays  = (Integer) t[1];
            String priority = (String) t[2];

            // Constraint UNIQUE đã xử lý ở DB nhưng check trước để tránh exception
            if (reminderRepository.existsByOrderIdAndReminderType(order.getId(), type)) continue;

            FollowUpReminder r = new FollowUpReminder();
            r.setOrderId(order.getId());
            r.setCustomerId(order.getCustomer().getId());
            r.setAssignedStaffId(order.getPrimaryStaffId()); // có thể null
            r.setReminderDate(order.getCompletedDate().plusDays(offsetDays));
            r.setReminderType(type);
            r.setStatus("PENDING");
            r.setPriority(priority);
            r.setCareNotes(buildDefaultCareNotes(order, type));
            r.setCreatedBy(createdBy);
            reminderRepository.save(r);
            created++;
        }
        if (created > 0) {
            log.info("Tạo {} reminders tự động cho đơn hàng {}", created, order.getOrderCode());
        }
        return created;
    }

    /**
     * Tạo tự động cho 1 orderId (tiện gọi từ controller).
     */
    @Transactional
    public List<FollowUpReminderDTO> autoCreateForOrderById(Long orderId, Long userId) {
        TailoringOrder order = findOrder(orderId);
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException("ORDER_NOT_COMPLETED",
                "Đơn hàng chưa hoàn thành. Chỉ tạo reminder cho đơn COMPLETED.");
        }
        autoCreateForOrder(order, userId);
        return reminderRepository.findByOrderIdOrderByReminderDateAsc(orderId)
            .stream().map(r -> convertReminderToDTO(r, false)).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════════════
    // PHẦN 2 — CRUD REMINDER THỦ CÔNG (6.1)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Tạo reminder thủ công (CUSTOM) cho bất kỳ đơn hàng nào.
     */
    @Transactional
    public FollowUpReminderDTO createManualReminder(FollowUpReminderDTO dto, Long userId) {
        if (dto.getOrderId() == null) throw new BusinessException("ORDER_REQUIRED", "Phải chọn đơn hàng.");
        if (dto.getReminderDate() == null) throw new BusinessException("DATE_REQUIRED", "Phải chọn ngày nhắc.");

        TailoringOrder order = findOrder(dto.getOrderId());

        FollowUpReminder r = new FollowUpReminder();
        r.setOrderId(order.getId());
        r.setCustomerId(order.getCustomer().getId());
        r.setAssignedStaffId(dto.getAssignedStaffId() != null
            ? dto.getAssignedStaffId() : order.getPrimaryStaffId());
        r.setReminderDate(dto.getReminderDate());
        r.setReminderType("CUSTOM");
        r.setStatus("PENDING");
        r.setPriority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM");
        r.setCareNotes(dto.getCareNotes());
        r.setCreatedBy(userId);

        FollowUpReminder saved = reminderRepository.save(r);
        log.info("Tạo reminder CUSTOM cho đơn {} vào ngày {}", order.getOrderCode(), saved.getReminderDate());
        return convertReminderToDTO(saved, true);
    }

    /**
     * Cập nhật reminder (ngày, nhân viên, ghi chú).
     */
    @Transactional
    public FollowUpReminderDTO updateReminder(Long reminderId, FollowUpReminderDTO dto, Long userId) {
        FollowUpReminder r = findReminder(reminderId);
        if ("DONE".equals(r.getStatus()) || "CANCELLED".equals(r.getStatus())) {
            throw new BusinessException("REMINDER_CLOSED", "Không thể sửa reminder đã hoàn thành hoặc đã hủy.");
        }
        if (dto.getReminderDate() != null) r.setReminderDate(dto.getReminderDate());
        if (dto.getAssignedStaffId() != null) r.setAssignedStaffId(dto.getAssignedStaffId());
        if (dto.getPriority() != null) r.setPriority(dto.getPriority());
        if (dto.getCareNotes() != null) r.setCareNotes(dto.getCareNotes());
        return convertReminderToDTO(reminderRepository.save(r), true);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PHẦN 3 — THỰC HIỆN CHĂM SÓC (6.3 + 6.4)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Ghi nhận 1 lần liên hệ chăm sóc (6.4).
     * Cập nhật contactCount trên reminder.
     * Nếu outcome = SATISFIED / REPEAT_ORDER → tự động tính rating.
     */
    @Transactional
    public FollowUpLogDTO logContact(Long reminderId, FollowUpLogDTO dto, Long userId) {
        FollowUpReminder reminder = findReminder(reminderId);

        if ("CANCELLED".equals(reminder.getStatus())) {
            throw new BusinessException("REMINDER_CANCELLED", "Reminder đã bị hủy.");
        }

        FollowUpLog log = new FollowUpLog();
        log.setReminderId(reminderId);
        log.setOrderId(reminder.getOrderId());
        log.setCustomerId(reminder.getCustomerId());
        log.setStaffId(dto.getStaffId() != null ? dto.getStaffId() : userId);
        log.setContactType(dto.getContactType() != null ? dto.getContactType() : "CALL");
        log.setOutcome(dto.getOutcome() != null ? dto.getOutcome() : "ANSWERED");
        log.setContent(dto.getContent());
        log.setCustomerFeedback(dto.getCustomerFeedback());
        log.setCustomerRating(dto.getCustomerRating());
        log.setNextAction(dto.getNextAction());
        log.setContactedAt(dto.getContactedAt() != null ? dto.getContactedAt() : LocalDateTime.now());

        FollowUpLog saved = logRepository.save(log);

        // Cập nhật contactCount và customerRating trên reminder
        reminder.setContactCount(reminder.getContactCount() + 1);
        if (dto.getCustomerRating() != null) {
            reminder.setCustomerRating(dto.getCustomerRating());
        }
        reminderRepository.save(reminder);

        this.log.info("Ghi nhận liên hệ {} cho reminder {} — kết quả: {}",
            log.getContactType(), reminderId, log.getOutcome());
        return convertLogToDTO(saved);
    }

    /**
     * Đánh dấu reminder DONE + tùy chọn ghi log cuối cùng (6.3 / 6.4).
     */
    @Transactional
    public FollowUpReminderDTO markDone(Long reminderId, FollowUpLogDTO logDTO, Long userId) {
        FollowUpReminder reminder = findReminder(reminderId);
        if ("DONE".equals(reminder.getStatus())) {
            throw new BusinessException("ALREADY_DONE", "Reminder đã được hoàn thành rồi.");
        }

        // Ghi log nếu có thông tin liên hệ
        if (logDTO != null && logDTO.getContactType() != null) {
            logContact(reminderId, logDTO, userId);
        }

        reminder.setStatus("DONE");
        reminder.setCompletedAt(LocalDateTime.now());
        reminder.setCompletedBy(userId);
        log.info("Đánh dấu DONE reminder id={} cho đơn {}", reminderId, reminder.getOrderId());
        return convertReminderToDTO(reminderRepository.save(reminder), true);
    }

    /**
     * Bỏ qua / hủy reminder.
     */
    @Transactional
    public FollowUpReminderDTO skipReminder(Long reminderId, String reason, String newStatus, Long userId) {
        FollowUpReminder reminder = findReminder(reminderId);
        if ("DONE".equals(reminder.getStatus())) {
            throw new BusinessException("ALREADY_DONE", "Reminder đã hoàn thành, không thể bỏ qua.");
        }
        String targetStatus = "CANCELLED".equals(newStatus) ? "CANCELLED" : "SKIPPED";
        reminder.setStatus(targetStatus);
        reminder.setSkipReason(reason);
        return convertReminderToDTO(reminderRepository.save(reminder), false);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PHẦN 4 — ĐỌC DỮ LIỆU (6.3)
    // ════════════════════════════════════════════════════════════════════════

    /** Danh sách hôm nay cần chăm sóc (status = PENDING, reminderDate = today). */
    public List<FollowUpReminderDTO> getTodayReminders() {
        return reminderRepository.findTodayPending(LocalDate.now())
            .stream().map(r -> convertReminderToDTO(r, false)).collect(Collectors.toList());
    }

    /** Danh sách quá hạn. */
    public List<FollowUpReminderDTO> getOverdueReminders() {
        return reminderRepository.findOverdue(LocalDate.now())
            .stream().map(r -> convertReminderToDTO(r, false)).collect(Collectors.toList());
    }

    /** Danh sách sắp đến hạn (N ngày tới). */
    public List<FollowUpReminderDTO> getUpcomingReminders(int days) {
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to   = LocalDate.now().plusDays(days);
        return reminderRepository.findUpcoming(from, to)
            .stream().map(r -> convertReminderToDTO(r, false)).collect(Collectors.toList());
    }

    /** Danh sách phân trang, lọc theo status. */
    public Page<FollowUpReminderDTO> listReminders(String status, Pageable pageable) {
        Page<FollowUpReminder> page = (status != null && !status.isBlank())
            ? reminderRepository.findByStatus(status, pageable)
            : reminderRepository.findAllByOrderByReminderDateDesc(pageable);
        return page.map(r -> convertReminderToDTO(r, false));
    }

    /** Tất cả reminders của 1 đơn hàng. */
    public List<FollowUpReminderDTO> getRemindersByOrder(Long orderId) {
        return reminderRepository.findByOrderIdOrderByReminderDateAsc(orderId)
            .stream().map(r -> convertReminderToDTO(r, true)).collect(Collectors.toList());
    }

    /** Chi tiết 1 reminder kèm log. */
    public FollowUpReminderDTO getReminderById(Long reminderId) {
        return convertReminderToDTO(findReminder(reminderId), true);
    }

    /** Danh sách log của 1 reminder. */
    public List<FollowUpLogDTO> getLogsByReminder(Long reminderId) {
        return logRepository.findByReminderIdOrderByContactedAtDesc(reminderId)
            .stream().map(this::convertLogToDTO).collect(Collectors.toList());
    }

    /** Tất cả logs phân trang. */
    public Page<FollowUpLogDTO> listAllLogs(Pageable pageable) {
        return logRepository.findAllByOrderByContactedAtDesc(pageable).map(this::convertLogToDTO);
    }

    /** Thống kê tổng quan (dashboard). */
    public Map<String, Object> getOverviewStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay   = today.plusDays(1).atStartOfDay();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayPending",  reminderRepository.countTodayPending(today));
        stats.put("overdue",       reminderRepository.countOverdue(today));
        stats.put("totalPending",  reminderRepository.countByStatus("PENDING"));
        stats.put("totalDone",     reminderRepository.countByStatus("DONE"));
        stats.put("totalSkipped",  reminderRepository.countByStatus("SKIPPED"));
        stats.put("contactsToday", logRepository.countContactsToday(startOfDay, endOfDay));
        stats.put("avgRating",     logRepository.findAverageCustomerRating());
        stats.put("repeatOrders",  logRepository.findByOutcomeOrderByContactedAtDesc("REPEAT_ORDER").size());
        return stats;
    }

    // ─── Internal helpers ─────────────────────────────────────────────────

    private TailoringOrder findOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng id=" + id));
    }

    private FollowUpReminder findReminder(Long id) {
        return reminderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy reminder id=" + id));
    }

    private String buildDefaultCareNotes(TailoringOrder order, String type) {
        String customerName = order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName();
        Map<String, String> notes = Map.of(
            "DAY_3",  "Hỏi thăm khách " + customerName + " về sản phẩm sau 3 ngày nhận hàng. " +
                      "Đơn: " + order.getOrderCode() + " — " + order.getOrderType() + ". " +
                      "Hỏi: form có vừa không, chất lượng ra sao, có gì cần điều chỉnh không.",
            "DAY_7",  "Chăm sóc khách sau 1 tuần. Hỏi về sự hài lòng, chụp ảnh sản phẩm nếu được. " +
                      "Khách: " + customerName + " — " + order.getOrderCode(),
            "DAY_10", "Nhắc chăm sóc lần cuối đợt này. Hỏi có nhu cầu may thêm không, " +
                      "xin phản hồi để review. Khách: " + customerName
        );
        return notes.getOrDefault(type, "Chăm sóc khách " + customerName + " — đơn " + order.getOrderCode());
    }

    // ─── Converters ──────────────────────────────────────────────────────

    public FollowUpReminderDTO convertReminderToDTO(FollowUpReminder r, boolean includeLogs) {
        // Customer info
        String customerName  = null;
        String customerPhone = null;
        Customer customer = customerRepository.findById(r.getCustomerId()).orElse(null);
        if (customer != null) {
            customerName  = customer.getFirstName() + " " + customer.getLastName();
            customerPhone = customer.getPhone();
        }

        // Order info
        String orderCode = null;
        String orderType = null;
        TailoringOrder order = orderRepository.findById(r.getOrderId()).orElse(null);
        if (order != null) {
            orderCode = order.getOrderCode();
            orderType = order.getOrderType();
        }

        // Staff info
        String staffName = null;
        if (r.getAssignedStaffId() != null) {
            staffName = staffRepository.findById(r.getAssignedStaffId())
                .map(Staff::getFullName).orElse(null);
        }

        // Completed by
        String completedByName = null;
        if (r.getCompletedBy() != null) {
            completedByName = userRepository.findById(r.getCompletedBy())
                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null);
        }

        // Computed fields
        LocalDate today = LocalDate.now();
        boolean isOverdue = "PENDING".equals(r.getStatus()) && r.getReminderDate().isBefore(today);
        long daysOverdue  = isOverdue ? ChronoUnit.DAYS.between(r.getReminderDate(), today) : 0;
        long daysUntilDue = !isOverdue && "PENDING".equals(r.getStatus())
            ? ChronoUnit.DAYS.between(today, r.getReminderDate()) : 0;

        // Logs
        List<FollowUpLogDTO> logs = includeLogs
            ? logRepository.findByReminderIdOrderByContactedAtDesc(r.getId())
                           .stream().map(this::convertLogToDTO).collect(Collectors.toList())
            : null;

        return FollowUpReminderDTO.builder()
            .id(r.getId())
            .orderId(r.getOrderId())
            .orderCode(orderCode)
            .orderType(orderType)
            .customerId(r.getCustomerId())
            .customerName(customerName)
            .customerPhone(customerPhone)
            .assignedStaffId(r.getAssignedStaffId())
            .assignedStaffName(staffName)
            .reminderDate(r.getReminderDate())
            .reminderType(r.getReminderType())
            .reminderTypeLabel(REMINDER_TYPE_LABELS.getOrDefault(r.getReminderType(), r.getReminderType()))
            .status(r.getStatus())
            .statusLabel(STATUS_LABELS.getOrDefault(r.getStatus(), r.getStatus()))
            .priority(r.getPriority())
            .priorityLabel(PRIORITY_LABELS.getOrDefault(r.getPriority(), r.getPriority()))
            .careNotes(r.getCareNotes())
            .completedAt(r.getCompletedAt())
            .completedBy(r.getCompletedBy())
            .completedByName(completedByName)
            .skipReason(r.getSkipReason())
            .contactCount(r.getContactCount())
            .customerRating(r.getCustomerRating())
            .isOverdue(isOverdue)
            .daysOverdue(daysOverdue)
            .daysUntilDue(daysUntilDue)
            .logs(logs)
            .createdAt(r.getCreatedAt())
            .updatedAt(r.getUpdatedAt())
            .createdBy(r.getCreatedBy())
            .build();
    }

    public FollowUpLogDTO convertLogToDTO(FollowUpLog l) {
        String staffName = null;
        if (l.getStaffId() != null) {
            staffName = userRepository.findById(l.getStaffId())
                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null);
        }
        String customerName = null;
        String customerPhone = null;
        Customer customer = customerRepository.findById(l.getCustomerId()).orElse(null);
        if (customer != null) {
            customerName  = customer.getFirstName() + " " + customer.getLastName();
            customerPhone = customer.getPhone();
        }
        String orderCode = null;
        TailoringOrder order = orderRepository.findById(l.getOrderId()).orElse(null);
        if (order != null) orderCode = order.getOrderCode();

        return FollowUpLogDTO.builder()
            .id(l.getId())
            .reminderId(l.getReminderId())
            .orderId(l.getOrderId())
            .orderCode(orderCode)
            .customerId(l.getCustomerId())
            .customerName(customerName)
            .customerPhone(customerPhone)
            .staffId(l.getStaffId())
            .staffName(staffName)
            .contactType(l.getContactType())
            .contactTypeLabel(CONTACT_TYPE_LABELS.getOrDefault(l.getContactType(), l.getContactType()))
            .outcome(l.getOutcome())
            .outcomeLabel(OUTCOME_LABELS.getOrDefault(l.getOutcome(), l.getOutcome()))
            .content(l.getContent())
            .customerFeedback(l.getCustomerFeedback())
            .customerRating(l.getCustomerRating())
            .nextAction(l.getNextAction())
            .contactedAt(l.getContactedAt())
            .createdAt(l.getCreatedAt())
            .build();
    }
}
