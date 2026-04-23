package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.CustomerInteractionDTO;
import com.tailorshop.metric.dto.LeadAssignmentDTO;
import com.tailorshop.metric.dto.StaffDTO;
import com.tailorshop.metric.entity.CustomerInteraction;
import com.tailorshop.metric.entity.Lead;
import com.tailorshop.metric.entity.LeadAssignment;
import com.tailorshop.metric.entity.Staff;
import com.tailorshop.metric.entity.User;
import com.tailorshop.metric.entity.UserRole;
import com.tailorshop.metric.exception.BusinessException;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.CustomerInteractionRepository;
import com.tailorshop.metric.repository.CustomerRepository;
import com.tailorshop.metric.repository.LeadAssignmentRepository;
import com.tailorshop.metric.repository.LeadRepository;
import com.tailorshop.metric.repository.StaffRepository;
import com.tailorshop.metric.repository.UserRepository;
import com.tailorshop.metric.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * StaffService — Quản lý nhân viên, phân công lead, approval workflow
 *
 * Lead Distribution Strategy:
 *  - AUTO_ROUND_ROBIN: phân cho nhân viên có ít lead nhất
 *  - AUTO_PERFORMANCE: phân cho nhân viên có performanceScore cao nhất
 *
 * Approval Workflow:
 *  - Khi nhân viên có performanceScore < PERFORMANCE_THRESHOLD (mặc định 40),
 *    assignment status = PENDING, cần manager duyệt.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StaffService {

    /** Ngưỡng điểm tối thiểu — dưới ngưỡng cần manager duyệt */
    @Value("${app.crm.performance-threshold:40.0}")
    private double performanceThreshold;

    private final StaffRepository               staffRepository;
    private final LeadRepository                leadRepository;
    private final LeadAssignmentRepository      assignmentRepository;
    private final CustomerRepository            customerRepository;
    private final CustomerInteractionRepository interactionRepository;
    private final UserRepository                userRepository;
    private final UserRoleRepository            userRoleRepository;
    private final PasswordEncoder               passwordEncoder;

    // ─── CRUD Staff ───────────────────────────────────────────────────────────

    @Transactional
    public StaffDTO createStaff(StaffDTO dto) {
        // inputUserId là effectively-final để dùng trong lambda
        final Long inputUserId = dto.getUserId();
        final Long resolvedUserId;

        if (inputUserId != null) {
            // Validate userId đã cung cấp tồn tại trong hệ thống
            userRepository.findById(inputUserId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                    "Tài khoản với ID " + inputUserId + " không tồn tại trong hệ thống"));
            if (staffRepository.findByUserId(inputUserId).isPresent()) {
                throw new BusinessException("STAFF_ALREADY_EXISTS", "User đã có profile nhân viên");
            }
            resolvedUserId = inputUserId;
        } else {
            // Auto-tạo tài khoản User mới cho nhân viên này
            resolvedUserId = autoCreateUserForStaff(dto).getId();
        }

        Staff staff = new Staff();
        staff.setStaffCode(generateStaffCode());
        staff.setUserId(resolvedUserId);
        staff.setFullName(dto.getFullName().trim());
        staff.setPhone(dto.getPhone());
        staff.setDepartment(dto.getDepartment());
        staff.setStaffRole(dto.getStaffRole() != null ? dto.getStaffRole() : "SALE");
        if (dto.getPerformanceScore() != null) staff.setPerformanceScore(dto.getPerformanceScore());
        if (dto.getMonthlyTarget() != null)    staff.setMonthlyTarget(dto.getMonthlyTarget());
        if (dto.getBaseCommissionRate() != null) staff.setBaseCommissionRate(dto.getBaseCommissionRate());
        staff.setNotes(dto.getNotes());
        staff.setIsActive(true);
        Staff saved = staffRepository.save(staff);
        log.info("Staff created: {}", saved.getStaffCode());
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public StaffDTO getStaffById(Long id) {
        return toDTO(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<StaffDTO> getAllActiveStaff(Pageable pageable) {
        return staffRepository.findByIsActiveTrue(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<StaffDTO> getStaffByRole(String role) {
        return staffRepository.findByStaffRoleAndIsActiveTrue(role)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public StaffDTO updateStaff(Long id, StaffDTO dto) {
        Staff staff = findById(id);
        if (dto.getFullName() != null)          staff.setFullName(dto.getFullName().trim());
        if (dto.getPhone() != null)             staff.setPhone(dto.getPhone());
        if (dto.getDepartment() != null)        staff.setDepartment(dto.getDepartment());
        if (dto.getStaffRole() != null)         staff.setStaffRole(dto.getStaffRole());
        if (dto.getMonthlyTarget() != null)     staff.setMonthlyTarget(dto.getMonthlyTarget());
        if (dto.getBaseCommissionRate() != null) staff.setBaseCommissionRate(dto.getBaseCommissionRate());
        if (dto.getNotes() != null)             staff.setNotes(dto.getNotes());
        return toDTO(staffRepository.save(staff));
    }

    @Transactional
    public StaffDTO updatePerformanceScore(Long staffId, BigDecimal score) {
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("INVALID_SCORE", "Điểm phải trong khoảng 0–100");
        }
        Staff staff = findById(staffId);
        staff.setPerformanceScore(score);
        log.info("Performance score updated for {}: {}", staff.getStaffCode(), score);
        return toDTO(staffRepository.save(staff));
    }

    @Transactional
    public void deactivateStaff(Long id) {
        Staff staff = findById(id);
        staff.setIsActive(false);
        staffRepository.save(staff);
    }

    // ─── Lead Distribution ────────────────────────────────────────────────────

    /**
     * Phân công lead cho nhân viên theo chiến lược.
     *
     * Ưu tiên nhận diện khách cũ: nếu lead đã từng có số điện thoại ≡ khách cũ
     * thì ưu tiên gán về nhân viên đã từng phụ trách.
     *
     * @param leadId         Lead cần phân
     * @param strategy       AUTO_ROUND_ROBIN | AUTO_PERFORMANCE | MANUAL
     * @param targetStaffId  Bắt buộc khi strategy = MANUAL
     * @param assignedByUserId  Người thực hiện phân (null = system)
     */
    @Transactional
    public LeadAssignmentDTO assignLead(Long leadId, String strategy,
                                        Long targetStaffId, Long assignedByUserId) {
        Lead lead = leadRepository.findById(leadId)
            .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));

        // 2.5 — nhận diện khách cũ
        Staff targetStaff = resolveTargetStaff(strategy, targetStaffId, lead);

        // Xác định trạng thái approval
        boolean needsApproval = targetStaff.getPerformanceScore()
            .compareTo(BigDecimal.valueOf(performanceThreshold)) < 0;
        String approvalStatus = needsApproval ? "PENDING" : "APPROVED";

        // Hủy assignment hiện tại (nếu có)
        assignmentRepository.deactivateByLeadId(leadId);

        LeadAssignment assignment = new LeadAssignment();
        assignment.setLeadId(leadId);
        assignment.setStaffId(targetStaff.getId());
        assignment.setAssignedBy(assignedByUserId);
        assignment.setAssignmentType(strategy);
        assignment.setApprovalStatus(approvalStatus);
        assignment.setIsCurrent(true);
        LeadAssignment saved = assignmentRepository.save(assignment);

        // Cập nhật lead.assignedStaffId nếu đã được duyệt
        if ("APPROVED".equals(approvalStatus)) {
            lead.setAssignedStaffId(targetStaff.getId());
            leadRepository.save(lead);
            // Tăng counter tổng lead của nhân viên
            targetStaff.setTotalLeads(targetStaff.getTotalLeads() + 1);
            staffRepository.save(targetStaff);
        }

        log.info("Lead {} assigned to staff {} [{}] — status: {}",
            lead.getLeadCode(), targetStaff.getStaffCode(), strategy, approvalStatus);
        return toAssignmentDTO(saved, targetStaff, lead);
    }

    /**
     * 2.6 — Manager phê duyệt assignment
     */
    @Transactional
    public LeadAssignmentDTO approveAssignment(Long assignmentId, Long managerUserId) {
        LeadAssignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        if (!"PENDING".equals(assignment.getApprovalStatus())) {
            throw new BusinessException("NOT_PENDING", "Assignment không ở trạng thái chờ duyệt");
        }
        assignment.setApprovalStatus("APPROVED");
        assignment.setApprovedByManagerId(managerUserId);
        assignment.setApprovedAt(LocalDateTime.now());

        // Cập nhật lead
        Lead lead = leadRepository.findById(assignment.getLeadId())
            .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        lead.setAssignedStaffId(assignment.getStaffId());
        leadRepository.save(lead);

        Staff staff = findById(assignment.getStaffId());
        staff.setTotalLeads(staff.getTotalLeads() + 1);
        staffRepository.save(staff);

        log.info("Assignment {} approved by manager {}", assignmentId, managerUserId);
        return toAssignmentDTO(assignmentRepository.save(assignment), staff, lead);
    }

    /**
     * 2.6 — Manager từ chối assignment và tự phân lại
     */
    @Transactional
    public LeadAssignmentDTO rejectAssignment(Long assignmentId, Long managerUserId,
                                               String reason, Long newStaffId) {
        LeadAssignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        if (!"PENDING".equals(assignment.getApprovalStatus())) {
            throw new BusinessException("NOT_PENDING", "Assignment không ở trạng thái chờ duyệt");
        }
        assignment.setApprovalStatus("REJECTED");
        assignment.setApprovedByManagerId(managerUserId);
        assignment.setApprovedAt(LocalDateTime.now());
        assignment.setRejectionReason(reason);
        assignment.setIsCurrent(false);
        assignmentRepository.save(assignment);

        // Phân lại cho newStaffId nếu được chỉ định
        if (newStaffId != null) {
            return assignLead(assignment.getLeadId(), "MANUAL", newStaffId, managerUserId);
        }
        Lead lead = leadRepository.findById(assignment.getLeadId())
            .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        Staff staff = findById(assignment.getStaffId());
        return toAssignmentDTO(assignment, staff, lead);
    }

    /** Lấy danh sách assignment đang PENDING */
    @Transactional(readOnly = true)
    public List<LeadAssignmentDTO> getPendingApprovals() {
        return assignmentRepository.findByApprovalStatusAndIsCurrentTrue("PENDING")
            .stream()
            .map(la -> {
                Staff staff = staffRepository.findById(la.getStaffId()).orElse(null);
                Lead lead = leadRepository.findById(la.getLeadId()).orElse(null);
                return toAssignmentDTO(la, staff, lead);
            })
            .collect(Collectors.toList());
    }

    /** Lịch sử phân có của 1 lead */
    @Transactional(readOnly = true)
    public List<LeadAssignmentDTO> getAssignmentHistory(Long leadId) {
        return assignmentRepository.findByLeadIdOrderByCreatedAtDesc(leadId)
            .stream()
            .map(la -> {
                Staff staff = staffRepository.findById(la.getStaffId()).orElse(null);
                Lead lead = leadRepository.findById(la.getLeadId()).orElse(null);
                return toAssignmentDTO(la, staff, lead);
            })
            .collect(Collectors.toList());
    }

    // ─── Customer Interaction (2.7) ───────────────────────────────────────────

    @Transactional
    public CustomerInteractionDTO addInteraction(Long customerId, CustomerInteractionDTO dto) {
        // Check customer exists
        customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        CustomerInteraction interaction = new CustomerInteraction();
        interaction.setCustomerId(customerId);
        interaction.setStaffId(dto.getStaffId());
        interaction.setInteractionType(dto.getInteractionType());
        interaction.setContent(dto.getContent());
        interaction.setOutcome(dto.getOutcome());
        interaction.setNextFollowupAt(dto.getNextFollowupAt());
        CustomerInteraction saved = interactionRepository.save(interaction);

        // Update customer counter + lastInteractionAt
        customerRepository.findById(customerId).ifPresent(c -> {
            c.setInteractionCount(c.getInteractionCount() + 1);
            c.setLastInteractionAt(LocalDateTime.now());
            customerRepository.save(c);
        });

        return toInteractionDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerInteractionDTO> getInteractions(Long customerId) {
        return interactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
            .stream().map(this::toInteractionDTO).collect(Collectors.toList());
    }

    // ─── Stats ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getStaffStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActive", staffRepository.findByIsActiveTrue(Pageable.unpaged()).getTotalElements());
        stats.put("totalSale",   staffRepository.findByStaffRoleAndIsActiveTrue("SALE").size());
        stats.put("totalTailor", staffRepository.findByStaffRoleAndIsActiveTrue("STAFF").size());
        stats.put("pendingApprovals", assignmentRepository.findByApprovalStatusAndIsCurrentTrue("PENDING").size());
        return stats;
    }

    /** Cập nhật conversionRate sau khi lead chuyển đổi thành công */
    @Transactional
    public void recalculateConversionRate(Long staffId) {
        Staff staff = findById(staffId);
        if (staff.getTotalLeads() > 0) {
            BigDecimal rate = BigDecimal.valueOf(staff.getTotalConverted())
                .divide(BigDecimal.valueOf(staff.getTotalLeads()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
            staff.setConversionRate(rate);
            staffRepository.save(staff);
        }
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    /**
     * Giải quyết nhân viên mục tiêu:
     *  1. Nếu MANUAL → dùng targetStaffId
     *  2. Kiểm tra khách cũ — nếu có SĐT trùng customer đã convert → ưu tiên NV cũ
     *  3. AUTO_PERFORMANCE → NV điểm cao nhất
     *  4. AUTO_ROUND_ROBIN → NV ít lead nhất
     */
    private Staff resolveTargetStaff(String strategy, Long targetStaffId, Lead lead) {
        if ("MANUAL".equals(strategy) && targetStaffId != null) {
            return findById(targetStaffId);
        }

        // 2.5 Nhận diện khách cũ qua SĐT
        if (lead.getPhone() != null) {
            Optional<Staff> previousStaff = staffRepository.findPreviousStaffForCustomer(
                lead.getConvertedCustomerId() != null ? lead.getConvertedCustomerId() : -1L);
            // Nếu chưa convert nhưng phone trùng customer cũ
            if (previousStaff.isEmpty() && lead.getPhone() != null) {
                previousStaff = customerRepository.findAll().stream()
                    .filter(c -> lead.getPhone().equals(c.getPhone())
                                 && c.getAssignedStaffId() != null)
                    .findFirst()
                    .flatMap(c -> staffRepository.findById(c.getAssignedStaffId()));
            }
            if (previousStaff.isPresent() && previousStaff.get().getIsActive()) {
                log.info("Returning customer detected — assigning to previous staff: {}",
                    previousStaff.get().getStaffCode());
                return previousStaff.get();
            }
        }

        List<Staff> candidates = "AUTO_PERFORMANCE".equals(strategy)
            ? staffRepository.findActiveOrderByPerformanceDesc()
            : staffRepository.findActiveForDistributionByLeadCount();

        if (candidates.isEmpty()) {
            throw new BusinessException("NO_STAFF", "Không có nhân viên hoạt động để phân công");
        }
        return candidates.get(0);
    }

    /**
     * Tự động tạo tài khoản User cho nhân viên mới.
     * Username: phone (nếu có) hoặc chuỗi chuẩn hoá từ họ tên + suffix nếu trùng.
     * Password mặc định: TailorShop@2024 (admin nên đổi sau khi tạo).
     */
    private User autoCreateUserForStaff(StaffDTO dto) {
        String baseUsername = (dto.getPhone() != null && !dto.getPhone().isBlank())
            ? dto.getPhone().replaceAll("[^0-9]", "")
            : normalizeToUsername(dto.getFullName());

        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix++;
        }

        // Tạo email placeholder nếu không có
        String email = username + "@tailorshop.local";
        while (userRepository.existsByEmail(email)) {
            email = username + System.currentTimeMillis() % 10000 + "@tailorshop.local";
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("TailorShop@2024"));
        user.setFirstName(dto.getFullName().contains(" ")
            ? dto.getFullName().substring(dto.getFullName().lastIndexOf(' ') + 1)
            : dto.getFullName());
        user.setLastName(dto.getFullName().contains(" ")
            ? dto.getFullName().substring(0, dto.getFullName().lastIndexOf(' '))
            : "");
        user.setPhone(dto.getPhone());
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setOauthProvider("LOCAL");

        // Gán role STAFF (fallback sang USER nếu chưa có role STAFF)
        UserRole role = userRoleRepository.findByName("STAFF")
            .or(() -> userRoleRepository.findByName("USER"))
            .orElse(null);
        if (role != null) {
            Set<UserRole> roles = new HashSet<>();
            roles.add(role);
            user.setRoles(roles);
        }

        User saved = userRepository.save(user);
        log.info("Auto-created user '{}' for new staff '{}'", saved.getUsername(), dto.getFullName());
        return saved;
    }

    /** Chuẩn hoá tên tiếng Việt thành username ASCII lowercase không dấu */
    private String normalizeToUsername(String name) {
        if (name == null || name.isBlank()) return "nhanvien";
        String normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .toLowerCase()
            .replaceAll("[^a-z0-9]", "");
        return normalized.isBlank() ? "nhanvien" : normalized;
    }

    private String generateStaffCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = staffRepository.count() + 1;
        return String.format("S-%s-%04d", date, count);
    }

    private Staff findById(Long id) {
        return staffRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + id));
    }

    // ─── Converters ──────────────────────────────────────────────────────────

    public StaffDTO toDTO(Staff s) {
        StaffDTO dto = StaffDTO.builder()
            .id(s.getId())
            .staffCode(s.getStaffCode())
            .userId(s.getUserId())
            .fullName(s.getFullName())
            .phone(s.getPhone())
            .department(s.getDepartment())
            .staffRole(s.getStaffRole())
            .performanceScore(s.getPerformanceScore())
            .monthlyTarget(s.getMonthlyTarget())
            .baseCommissionRate(s.getBaseCommissionRate())
            .totalLeads(s.getTotalLeads())
            .totalConverted(s.getTotalConverted())
            .conversionRate(s.getConversionRate())
            .totalRevenue(s.getTotalRevenue())
            .isActive(s.getIsActive())
            .notes(s.getNotes())
            .createdAt(s.getCreatedAt())
            .updatedAt(s.getUpdatedAt())
            .build();
        // Populate username from User
        if (s.getUserId() != null) {
            userRepository.findById(s.getUserId())
                .ifPresent(u -> dto.setUsername(u.getUsername()));
        }
        return dto;
    }

    private CustomerInteractionDTO toInteractionDTO(CustomerInteraction i) {
        CustomerInteractionDTO dto = CustomerInteractionDTO.builder()
            .id(i.getId())
            .customerId(i.getCustomerId())
            .staffId(i.getStaffId())
            .interactionType(i.getInteractionType())
            .content(i.getContent())
            .outcome(i.getOutcome())
            .nextFollowupAt(i.getNextFollowupAt())
            .createdAt(i.getCreatedAt())
            .build();
        if (i.getStaffId() != null) {
            staffRepository.findById(i.getStaffId())
                .ifPresent(st -> dto.setStaffName(st.getFullName()));
        }
        return dto;
    }

    private LeadAssignmentDTO toAssignmentDTO(LeadAssignment la, Staff staff, Lead lead) {
        return LeadAssignmentDTO.builder()
            .id(la.getId())
            .leadId(la.getLeadId())
            .leadCode(lead != null ? lead.getLeadCode() : null)
            .leadName(lead != null ? lead.getFullName() : null)
            .staffId(la.getStaffId())
            .staffName(staff != null ? staff.getFullName() : null)
            .assignedBy(la.getAssignedBy())
            .assignmentType(la.getAssignmentType())
            .approvalStatus(la.getApprovalStatus())
            .approvedByManagerId(la.getApprovedByManagerId())
            .approvedAt(la.getApprovedAt())
            .rejectionReason(la.getRejectionReason())
            .notes(la.getNotes())
            .isCurrent(la.getIsCurrent())
            .createdAt(la.getCreatedAt())
            .build();
    }
}
