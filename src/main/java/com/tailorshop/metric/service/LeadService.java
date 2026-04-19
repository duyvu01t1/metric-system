package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.LeadDTO;
import com.tailorshop.metric.dto.LeadInteractionDTO;
import com.tailorshop.metric.entity.Channel;
import com.tailorshop.metric.entity.Lead;
import com.tailorshop.metric.entity.LeadInteraction;
import com.tailorshop.metric.exception.BusinessException;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.ChannelRepository;
import com.tailorshop.metric.repository.LeadInteractionRepository;
import com.tailorshop.metric.repository.LeadRepository;
import com.tailorshop.metric.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lead Service — Nghiệp vụ quản lý khách hàng tiềm năng
 *
 * Bao gồm:
 * - CRUD lead
 * - Phát hiện số điện thoại / email tự động từ nội dung tin nhắn
 * - Xác định khách cũ (returning customer)
 * - Thêm tương tác (call, note, message)
 * - Chuyển đổi lead thành khách hàng (convert)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LeadService {

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("(\\+?84|0)[0-9]{8,10}");
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private final LeadRepository            leadRepository;
    private final LeadInteractionRepository interactionRepository;
    private final ChannelRepository         channelRepository;
    private final UserRepository            userRepository;

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Tạo lead mới — tự động bắt SĐT/Email từ sourceMessage nếu chưa điền
     */
    @Transactional
    public LeadDTO createLead(LeadDTO dto) {
        Channel channel = channelRepository.findById(dto.getChannelId())
            .orElseThrow(() -> new ResourceNotFoundException("Channel not found: " + dto.getChannelId()));

        Lead lead = new Lead();
        lead.setLeadCode(generateLeadCode());
        lead.setChannel(channel);
        lead.setFullName(dto.getFullName().trim());
        lead.setSourceMessage(dto.getSourceMessage());
        lead.setNeedType(dto.getNeedType());
        lead.setNeedDescription(dto.getNeedDescription());
        lead.setEstimatedBudget(dto.getEstimatedBudget());
        lead.setNotes(dto.getNotes());
        lead.setTags(dto.getTags());
        lead.setFollowupAt(dto.getFollowupAt());
        lead.setStatus("NEW");
        lead.setContactCount(0);

        // Tự động bắt SĐT / Email từ tin nhắn nguồn
        String phone = dto.getPhone();
        String email = dto.getEmail();
        if ((phone == null || phone.isBlank()) && dto.getSourceMessage() != null) {
            phone = extractPhone(dto.getSourceMessage());
        }
        if ((email == null || email.isBlank()) && dto.getSourceMessage() != null) {
            email = extractEmail(dto.getSourceMessage());
        }
        lead.setPhone(phone);
        lead.setEmail(email);

        // Xác định khách cũ dựa trên SĐT đã từng chốt đơn
        if (phone != null && !phone.isBlank()) {
            boolean returning = !leadRepository.findConvertedByPhone(phone).isEmpty();
            lead.setIsReturningCustomer(returning);
        } else {
            lead.setIsReturningCustomer(false);
        }

        // Gán nhân viên nếu có
        lead.setAssignedStaffId(dto.getAssignedStaffId());
        lead.setCreatedBy(dto.getCreatedBy());

        Lead saved = leadRepository.save(lead);
        log.info("Lead created: {} from channel: {}", saved.getLeadCode(), channel.getChannelCode());
        return toDTO(saved);
    }

    /**
     * Lấy lead theo ID
     */
    @Transactional(readOnly = true)
    public LeadDTO getLeadById(Long id) {
        Lead lead = findLeadById(id);
        return toDTO(lead);
    }

    /**
     * Lấy tất cả leads (có phân trang)
     */
    @Transactional(readOnly = true)
    public Page<LeadDTO> getAllLeads(Pageable pageable) {
        return leadRepository.findAll(pageable).map(this::toDTO);
    }

    /**
     * Lấy leads theo trạng thái
     */
    @Transactional(readOnly = true)
    public Page<LeadDTO> getLeadsByStatus(String status, Pageable pageable) {
        return leadRepository.findByStatus(status, pageable).map(this::toDTO);
    }

    /**
     * Lấy leads theo kênh
     */
    @Transactional(readOnly = true)
    public Page<LeadDTO> getLeadsByChannel(Long channelId, Pageable pageable) {
        return leadRepository.findByChannelId(channelId, pageable).map(this::toDTO);
    }

    /**
     * Lấy leads của một nhân viên
     */
    @Transactional(readOnly = true)
    public Page<LeadDTO> getLeadsByStaff(Long staffId, Pageable pageable) {
        return leadRepository.findByAssignedStaffId(staffId, pageable).map(this::toDTO);
    }

    /**
     * Tìm kiếm lead
     */
    @Transactional(readOnly = true)
    public Page<LeadDTO> searchLeads(String q, Pageable pageable) {
        return leadRepository.search(q, pageable).map(this::toDTO);
    }

    /**
     * Cập nhật thông tin lead
     */
    @Transactional
    public LeadDTO updateLead(Long id, LeadDTO dto) {
        Lead lead = findLeadById(id);

        if (dto.getChannelId() != null && !dto.getChannelId().equals(lead.getChannel().getId())) {
            Channel channel = channelRepository.findById(dto.getChannelId())
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found: " + dto.getChannelId()));
            lead.setChannel(channel);
        }

        lead.setFullName(dto.getFullName().trim());
        lead.setPhone(dto.getPhone());
        lead.setEmail(dto.getEmail());
        lead.setSourceMessage(dto.getSourceMessage());
        lead.setNeedType(dto.getNeedType());
        lead.setNeedDescription(dto.getNeedDescription());
        lead.setEstimatedBudget(dto.getEstimatedBudget());
        lead.setNotes(dto.getNotes());
        lead.setTags(dto.getTags());
        lead.setFollowupAt(dto.getFollowupAt());
        lead.setAssignedStaffId(dto.getAssignedStaffId());

        Lead updated = leadRepository.save(lead);
        log.info("Lead updated: {}", updated.getLeadCode());
        return toDTO(updated);
    }

    /**
     * Cập nhật trạng thái lead
     */
    @Transactional
    public LeadDTO updateLeadStatus(Long id, String newStatus, String lostReason) {
        Lead lead = findLeadById(id);
        String oldStatus = lead.getStatus();

        validateStatusTransition(oldStatus, newStatus);

        lead.setStatus(newStatus);
        if ("LOST".equals(newStatus)) {
            lead.setLostReason(lostReason);
        }
        if ("CONTACTED".equals(newStatus) || "NEGOTIATING".equals(newStatus)) {
            lead.setLastContactedAt(LocalDateTime.now());
            lead.setContactCount(lead.getContactCount() + 1);
        }

        Lead updated = leadRepository.save(lead);
        log.info("Lead {} status changed: {} -> {}", lead.getLeadCode(), oldStatus, newStatus);
        return toDTO(updated);
    }

    /**
     * Chuyển đổi lead thành khách hàng (CONVERT)
     * Gọi sau khi Customer và TailoringOrder đã được tạo thành công
     */
    @Transactional
    public LeadDTO convertLead(Long leadId, Long customerId, Long orderId) {
        Lead lead = findLeadById(leadId);
        if ("CONVERTED".equals(lead.getStatus())) {
            throw new BusinessException("LEAD_ALREADY_CONVERTED", "Lead đã được chuyển đổi trước đó");
        }
        lead.setStatus("CONVERTED");
        lead.setConvertedCustomerId(customerId);
        lead.setConvertedOrderId(orderId);
        lead.setConvertedAt(LocalDateTime.now());
        Lead updated = leadRepository.save(lead);
        log.info("Lead {} converted to customer {} / order {}", lead.getLeadCode(), customerId, orderId);
        return toDTO(updated);
    }

    /**
     * Lấy danh sách follow-up hôm nay
     */
    @Transactional(readOnly = true)
    public List<LeadDTO> getFollowUpToday() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end   = start.plusDays(1);
        return leadRepository.findFollowUpToday(start, end)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Thống kê tổng quan (dashboard)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLeadStats() {
        Map<String, Object> stats = new HashMap<>();
        long total = leadRepository.count();
        stats.put("total", total);

        List<Object[]> byStatus = leadRepository.countByStatus();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : byStatus) {
            statusMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("byStatus", statusMap);
        stats.put("newLeads",       statusMap.getOrDefault("NEW", 0L));
        stats.put("contacted",      statusMap.getOrDefault("CONTACTED", 0L));
        stats.put("qualified",      statusMap.getOrDefault("QUALIFIED", 0L));
        stats.put("negotiating",    statusMap.getOrDefault("NEGOTIATING", 0L));
        stats.put("converted",      statusMap.getOrDefault("CONVERTED", 0L));
        stats.put("lost",           statusMap.getOrDefault("LOST", 0L));

        // Tỷ lệ chuyển đổi
        long converted = statusMap.getOrDefault("CONVERTED", 0L);
        long qualifiable = total - statusMap.getOrDefault("LOST", 0L);
        double conversionRate = qualifiable > 0 ? (double) converted / qualifiable * 100 : 0;
        stats.put("conversionRate", Math.round(conversionRate * 10.0) / 10.0);

        List<Object[]> byChannel = leadRepository.countByChannel();
        Map<String, Long> channelMap = new HashMap<>();
        for (Object[] row : byChannel) {
            channelMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("byChannel", channelMap);

        // Follow-up hôm nay
        stats.put("followUpToday", getFollowUpToday().size());

        return stats;
    }

    // ─── INTERACTIONS ─────────────────────────────────────────────────────────

    /**
     * Thêm tương tác mới cho lead (gọi điện, ghi chú, tin nhắn, v.v.)
     */
    @Transactional
    public LeadInteractionDTO addInteraction(Long leadId, LeadInteractionDTO dto) {
        Lead lead = findLeadById(leadId);

        LeadInteraction interaction = new LeadInteraction();
        interaction.setLead(lead);
        interaction.setInteractionType(dto.getInteractionType().toUpperCase());
        interaction.setContent(dto.getContent());
        interaction.setOutcome(dto.getOutcome());
        interaction.setInteractedBy(dto.getInteractedBy());
        interaction.setInteractedAt(dto.getInteractedAt() != null ? dto.getInteractedAt() : LocalDateTime.now());

        LeadInteraction saved = interactionRepository.save(interaction);

        // Cập nhật last_contacted_at và contact_count
        lead.setLastContactedAt(saved.getInteractedAt());
        lead.setContactCount(lead.getContactCount() + 1);
        leadRepository.save(lead);

        log.info("Interaction added to lead {}: {}", lead.getLeadCode(), dto.getInteractionType());
        return toInteractionDTO(saved);
    }

    /**
     * Lấy lịch sử tương tác của lead
     */
    @Transactional(readOnly = true)
    public List<LeadInteractionDTO> getInteractions(Long leadId) {
        findLeadById(leadId); // validate exists
        return interactionRepository.findByLeadIdOrderByInteractedAtDesc(leadId)
            .stream().map(this::toInteractionDTO).collect(Collectors.toList());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Lead findLeadById(Long id) {
        return leadRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));
    }

    private void validateStatusTransition(String from, String to) {
        // Không cho chuyển ngược từ CONVERTED / LOST về trạng thái trước
        if ("CONVERTED".equals(from) || "LOST".equals(from)) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                "Không thể thay đổi trạng thái từ " + from + " sang " + to);
        }
    }

    /**
     * Tự động nhận diện số điện thoại trong văn bản
     */
    public String extractPhone(String text) {
        if (text == null) return null;
        Matcher m = PHONE_PATTERN.matcher(text);
        return m.find() ? m.group() : null;
    }

    /**
     * Tự động nhận diện email trong văn bản
     */
    public String extractEmail(String text) {
        if (text == null) return null;
        Matcher m = EMAIL_PATTERN.matcher(text);
        return m.find() ? m.group() : null;
    }

    /**
     * Sinh mã lead: L-YYYYMMDD-XXXX (XXXX = 4 ký tự từ timestamp millis)
     */
    private String generateLeadCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix   = String.format("%04d", System.currentTimeMillis() % 10000);
        return "L-" + datePart + "-" + suffix;
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    public LeadDTO toDTO(Lead l) {
        return LeadDTO.builder()
            .id(l.getId())
            .leadCode(l.getLeadCode())
            .channelId(l.getChannel() != null ? l.getChannel().getId() : null)
            .channelCode(l.getChannel() != null ? l.getChannel().getChannelCode() : null)
            .channelDisplayName(l.getChannel() != null ? l.getChannel().getDisplayName() : null)
            .channelIconClass(l.getChannel() != null ? l.getChannel().getIconClass() : null)
            .fullName(l.getFullName())
            .phone(l.getPhone())
            .email(l.getEmail())
            .sourceMessage(l.getSourceMessage())
            .needType(l.getNeedType())
            .needDescription(l.getNeedDescription())
            .estimatedBudget(l.getEstimatedBudget())
            .status(l.getStatus())
            .statusLabel(resolveStatusLabel(l.getStatus()))
            .lostReason(l.getLostReason())
            .assignedStaffId(l.getAssignedStaffId())
            .convertedCustomerId(l.getConvertedCustomerId())
            .convertedOrderId(l.getConvertedOrderId())
            .convertedAt(l.getConvertedAt())
            .followupAt(l.getFollowupAt())
            .lastContactedAt(l.getLastContactedAt())
            .contactCount(l.getContactCount())
            .notes(l.getNotes())
            .tags(l.getTags())
            .isReturningCustomer(l.getIsReturningCustomer())
            .createdAt(l.getCreatedAt())
            .updatedAt(l.getUpdatedAt())
            .createdBy(l.getCreatedBy())
            .build();
    }

    private LeadInteractionDTO toInteractionDTO(LeadInteraction i) {
        return LeadInteractionDTO.builder()
            .id(i.getId())
            .leadId(i.getLead() != null ? i.getLead().getId() : null)
            .interactionType(i.getInteractionType())
            .interactionTypeLabel(resolveInteractionTypeLabel(i.getInteractionType()))
            .content(i.getContent())
            .outcome(i.getOutcome())
            .interactedBy(i.getInteractedBy())
            .interactedAt(i.getInteractedAt())
            .createdAt(i.getCreatedAt())
            .build();
    }

    private String resolveStatusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "NEW"          -> "Mới tiếp nhận";
            case "CONTACTED"    -> "Đã liên hệ";
            case "QUALIFIED"    -> "Đã xác định nhu cầu";
            case "NEGOTIATING"  -> "Đang tư vấn/báo giá";
            case "CONVERTED"    -> "Đã chốt đơn";
            case "LOST"         -> "Bỏ lỡ";
            default             -> status;
        };
    }

    private String resolveInteractionTypeLabel(String type) {
        if (type == null) return "";
        return switch (type) {
            case "NOTE"     -> "Ghi chú";
            case "CALL"     -> "Gọi điện";
            case "MESSAGE"  -> "Nhắn tin";
            case "MEETING"  -> "Gặp mặt";
            case "EMAIL"    -> "Email";
            default         -> type;
        };
    }
}
