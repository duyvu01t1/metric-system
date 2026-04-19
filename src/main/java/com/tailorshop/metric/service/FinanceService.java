package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.ExpenseDTO;
import com.tailorshop.metric.dto.StaffCommissionDTO;
import com.tailorshop.metric.entity.Expense;
import com.tailorshop.metric.entity.Staff;
import com.tailorshop.metric.entity.StaffCommission;
import com.tailorshop.metric.entity.TailoringOrder;
import com.tailorshop.metric.exception.BusinessException;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.ExpenseRepository;
import com.tailorshop.metric.repository.StaffCommissionRepository;
import com.tailorshop.metric.repository.StaffRepository;
import com.tailorshop.metric.repository.TailoringOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * FinanceService — Phân hệ 7: Tài chính & Hoa hồng
 *
 * Trách nhiệm:
 *  7.1  Quản lý hoa hồng nhân viên (StaffCommission)
 *  7.2  Tự động tính hoa hồng khi tạo/cập nhật đơn hàng
 *  7.3  Cho phép chỉnh sửa hoa hồng thủ công từng công đoạn
 *  7.4  Quản lý chi phí vận hành (Expense)
 *  7.5  Báo cáo hoa hồng theo nhân viên / kỳ
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceService {

    /* ─── default rates — overridden by Settings ─────────────────── */
    private static final BigDecimal DEFAULT_PRIMARY_RATE   = BigDecimal.valueOf(0.05);
    private static final BigDecimal DEFAULT_SECONDARY_RATE = BigDecimal.valueOf(0.02);

    private final StaffCommissionRepository commissionRepo;
    private final ExpenseRepository         expenseRepo;
    private final StaffRepository           staffRepo;
    private final TailoringOrderRepository  orderRepo;

    /* ──────────────────────────────────────────────────────────────
       7.2  AUTO-CALCULATE COMMISSIONS FOR ORDER
    ────────────────────────────────────────────────────────────── */

    /**
     * Tính & lưu hoa hồng cho đơn hàng mới / đã cập nhật staff.
     * Idempotent: ghi đè bản ghi cũ nếu đã tồn tại (chưa override).
     */
    @Transactional
    public List<StaffCommissionDTO> calculateCommissionsForOrder(Long orderId, Long userId) {
        TailoringOrder order = orderRepo.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("TailoringOrder", "id", orderId));

        if (order.getTotalPrice() == null || order.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_ORDER_PRICE",
                "Đơn hàng chưa có tổng giá — không thể tính hoa hồng");
        }

        LocalDate today = LocalDate.now();
        Short month = (short) today.getMonthValue();
        Short year  = (short) today.getYear();

        // Base = totalPrice (trừ discount_amount nếu có)
        BigDecimal discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal base = order.getTotalPrice().subtract(discountAmount);

        List<StaffCommission> saved = new java.util.ArrayList<>();

        // PRIMARY staff
        if (order.getPrimaryStaffId() != null) {
            saved.add(saveCommission(order, order.getPrimaryStaffId(), "PRIMARY",
                DEFAULT_PRIMARY_RATE, base, month, year, userId));
        }

        // SECONDARY staff
        if (order.getSecondaryStaffId() != null) {
            saved.add(saveCommission(order, order.getSecondaryStaffId(), "SECONDARY",
                DEFAULT_SECONDARY_RATE, base, month, year, userId));
        }

        if (saved.isEmpty()) {
            log.info("Order {} has no assigned staff — no commissions created", order.getOrderCode());
        }
        return saved.stream().map(c -> convertToDTO(c)).collect(Collectors.toList());
    }

    @Transactional
    private StaffCommission saveCommission(TailoringOrder order, Long staffId, String roleType,
            BigDecimal rate, BigDecimal base, Short month, Short year, Long userId) {

        Staff staff = staffRepo.findById(staffId).orElse(null);
        BigDecimal effectiveRate = (staff != null && staff.getBaseCommissionRate() != null
                && staff.getBaseCommissionRate().compareTo(BigDecimal.ZERO) > 0)
            ? (roleType.equals("PRIMARY") ? staff.getBaseCommissionRate() : DEFAULT_SECONDARY_RATE)
            : rate;

        BigDecimal amount = base.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);

        // Upsert: find existing non-overridden record
        StaffCommission sc = commissionRepo
            .findByOrderIdAndStaffIdAndStaffRoleType(order.getId(), staffId, roleType)
            .orElse(new StaffCommission());

        if (Boolean.TRUE.equals(sc.getIsManualOverride())) {
            log.info("Commission for order {} staff {} is manually overridden — skipping auto-recalc",
                order.getOrderCode(), staffId);
            return sc; // do NOT overwrite manual overrides
        }

        sc.setOrderId(order.getId());
        sc.setOrderCode(order.getOrderCode());
        sc.setStaffId(staffId);
        sc.setStaffName(staff != null ? staff.getFullName() : "Staff #" + staffId);
        sc.setStaffRoleType(roleType);
        sc.setCommissionRate(effectiveRate);
        sc.setCommissionBase(base);
        sc.setCommissionAmount(amount);
        sc.setIsManualOverride(false);
        sc.setPeriodMonth(month);
        sc.setPeriodYear(year);
        sc.setCreatedBy(userId);
        return commissionRepo.save(sc);
    }

    /* ──────────────────────────────────────────────────────────────
       7.3  MANUAL OVERRIDE
    ────────────────────────────────────────────────────────────── */

    /**
     * Chỉnh sửa hoa hồng thủ công (7.3).
     */
    @Transactional
    public StaffCommissionDTO overrideCommission(Long commissionId,
            BigDecimal newAmount, String reason, Long productionStageId, Long userId) {
        StaffCommission sc = commissionRepo.findById(commissionId)
            .orElseThrow(() -> new ResourceNotFoundException("StaffCommission", "id", commissionId));

        if (Boolean.TRUE.equals(sc.getIsPaid())) {
            throw new BusinessException("COMMISSION_ALREADY_PAID",
                "Không thể chỉnh sửa hoa hồng đã thanh toán");
        }
        if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("INVALID_COMMISSION_AMOUNT",
                "Số tiền hoa hồng không hợp lệ");
        }

        sc.setCommissionAmount(newAmount);
        sc.setIsManualOverride(true);
        sc.setOverrideReason(reason);
        sc.setProductionStageId(productionStageId);
        sc.setUpdatedBy(userId);
        return convertToDTO(commissionRepo.save(sc));
    }

    /**
     * Đánh dấu đã trả hoa hồng.
     */
    @Transactional
    public StaffCommissionDTO markPaid(Long commissionId, Long userId) {
        StaffCommission sc = commissionRepo.findById(commissionId)
            .orElseThrow(() -> new ResourceNotFoundException("StaffCommission", "id", commissionId));
        if (Boolean.TRUE.equals(sc.getIsPaid())) {
            throw new BusinessException("COMMISSION_ALREADY_PAID", "Hoa hồng đã được thanh toán trước đó");
        }
        sc.setIsPaid(true);
        sc.setPaidAt(LocalDateTime.now());
        sc.setUpdatedBy(userId);
        return convertToDTO(commissionRepo.save(sc));
    }

    /**
     * Đánh dấu trả nhiều hoa hồng cùng lúc.
     */
    @Transactional
    public int markPaidBulk(List<Long> commissionIds, Long userId) {
        int count = 0;
        for (Long id : commissionIds) {
            StaffCommission sc = commissionRepo.findById(id).orElse(null);
            if (sc != null && !Boolean.TRUE.equals(sc.getIsPaid())) {
                sc.setIsPaid(true);
                sc.setPaidAt(LocalDateTime.now());
                sc.setUpdatedBy(userId);
                commissionRepo.save(sc);
                count++;
            }
        }
        return count;
    }

    /* ──────────────────────────────────────────────────────────────
       7.5  COMMISSION QUERIES / REPORTS
    ────────────────────────────────────────────────────────────── */

    public Page<StaffCommissionDTO> listCommissions(Long staffId, Boolean isPaid,
            Short year, Short month, Pageable pageable) {
        return commissionRepo.findFiltered(staffId, isPaid, year, month, pageable)
            .map(this::convertToDTO);
    }

    public List<StaffCommissionDTO> getCommissionsByOrder(Long orderId) {
        return commissionRepo.findByOrderId(orderId).stream()
            .map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<StaffCommissionDTO> getCommissionsByStaff(Long staffId) {
        return commissionRepo.findByStaffId(staffId).stream()
            .map(this::convertToDTO).collect(Collectors.toList());
    }

    public StaffCommissionDTO getCommissionById(Long id) {
        return convertToDTO(commissionRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("StaffCommission", "id", id)));
    }

    /** Tổng hợp hoa hồng theo kỳ, nhóm theo nhân viên (7.5). */
    public List<Map<String, Object>> getCommissionSummaryByPeriod(short year, short month) {
        List<Object[]> rows = commissionRepo.summarizeByStaffForPeriod(year, month);
        return rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("staffId",    row[0]);
            m.put("staffName",  row[1]);
            m.put("totalCommission", row[2]);
            return m;
        }).collect(Collectors.toList());
    }

    /* ──────────────────────────────────────────────────────────────
       7.4  EXPENSE CRUD
    ────────────────────────────────────────────────────────────── */

    @Transactional
    public ExpenseDTO createExpense(ExpenseDTO dto, Long userId) {
        Expense e = new Expense();
        LocalDate today = LocalDate.now();
        e.setExpenseCode(generateExpenseCode());
        fillExpense(e, dto);
        if (e.getPeriodMonth() == null) e.setPeriodMonth((short) (dto.getExpenseDate() != null
            ? dto.getExpenseDate().getMonthValue() : today.getMonthValue()));
        if (e.getPeriodYear() == null)  e.setPeriodYear((short)  (dto.getExpenseDate() != null
            ? dto.getExpenseDate().getYear() : today.getYear()));
        e.setCreatedBy(userId);
        return convertExpenseToDTO(expenseRepo.save(e));
    }

    @Transactional
    public ExpenseDTO updateExpense(Long expenseId, ExpenseDTO dto, Long userId) {
        Expense e = expenseRepo.findById(expenseId)
            .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));
        if ("APPROVED".equals(e.getStatus())) {
            throw new BusinessException("EXPENSE_ALREADY_APPROVED", "Không thể sửa chi phí đã duyệt");
        }
        fillExpense(e, dto);
        e.setUpdatedBy(userId);
        return convertExpenseToDTO(expenseRepo.save(e));
    }

    @Transactional
    public ExpenseDTO approveExpense(Long expenseId, Long userId) {
        Expense e = expenseRepo.findById(expenseId)
            .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));
        e.setStatus("APPROVED");
        e.setApprovedBy(userId);
        e.setApprovedAt(LocalDateTime.now());
        e.setUpdatedBy(userId);
        return convertExpenseToDTO(expenseRepo.save(e));
    }

    @Transactional
    public ExpenseDTO rejectExpense(Long expenseId, String reason, Long userId) {
        Expense e = expenseRepo.findById(expenseId)
            .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));
        e.setStatus("REJECTED");
        e.setRejectReason(reason);
        e.setUpdatedBy(userId);
        return convertExpenseToDTO(expenseRepo.save(e));
    }

    @Transactional
    public void deleteExpense(Long expenseId) {
        Expense e = expenseRepo.findById(expenseId)
            .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));
        if ("APPROVED".equals(e.getStatus())) {
            throw new BusinessException("EXPENSE_ALREADY_APPROVED", "Không thể xóa chi phí đã duyệt");
        }
        expenseRepo.delete(e);
    }

    public Page<ExpenseDTO> listExpenses(String category, String status,
            Short year, Short month, Pageable pageable) {
        return expenseRepo.findFiltered(category, status, year, month, pageable)
            .map(this::convertExpenseToDTO);
    }

    public ExpenseDTO getExpenseById(Long id) {
        return convertExpenseToDTO(expenseRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id)));
    }

    public Map<String, Object> getExpenseSummary(short year, short month) {
        List<Object[]> rows = expenseRepo.sumByCategory(year, month);
        Map<String, Object> result = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String cat    = (String) row[0];
            BigDecimal amt = (BigDecimal) row[1];
            byCategory.put(cat, amt);
            byCategory.put(cat + "_label", null); // will be set in controller or front-end
            total = total.add(amt);
        }
        result.put("year",       year);
        result.put("month",      month);
        result.put("total",      total);
        result.put("byCategory", byCategory);
        result.put("pendingCount", expenseRepo.countByStatus("PENDING"));
        return result;
    }

    /* ──────────────────────────────────────────────────────────────
       FINANCE OVERVIEW (dashboard)
    ────────────────────────────────────────────────────────────── */

    public Map<String, Object> getFinanceOverview(short year, short month) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Commission totals
        BigDecimal totalCommission    = commissionRepo.sumByPeriod(year, month);
        BigDecimal unpaidCommission   = commissionRepo.sumAllUnpaid();
        Long       unpaidCount        = commissionRepo.countByIsPaid(false);

        // Expenses
        BigDecimal totalExpenses      = expenseRepo.sumApprovedByPeriod(year, month);
        Long       pendingExpenseCount = expenseRepo.countByStatus("PENDING");

        // Commissions by staff for period
        List<Map<String, Object>> commissionByStaff = getCommissionSummaryByPeriod(year, month);

        // Expense summary
        List<Object[]> expenseSummaryRows = expenseRepo.sumByCategory(year, month);
        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();
        for (Object[] row : expenseSummaryRows) {
            expenseByCategory.put((String) row[0], (BigDecimal) row[1]);
        }

        result.put("year",                  year);
        result.put("month",                 month);
        result.put("totalCommissionPeriod", totalCommission);
        result.put("unpaidCommissionTotal", unpaidCommission);
        result.put("unpaidCommissionCount", unpaidCount);
        result.put("totalExpensesPeriod",   totalExpenses);
        result.put("pendingExpenseCount",   pendingExpenseCount);
        result.put("commissionByStaff",     commissionByStaff);
        result.put("expenseByCategory",     expenseByCategory);
        return result;
    }

    /* ──────────────────────────────────────────────────────────────
       CONVERTERS
    ────────────────────────────────────────────────────────────── */

    private StaffCommissionDTO convertToDTO(StaffCommission sc) {
        return StaffCommissionDTO.builder()
            .id(sc.getId())
            .orderId(sc.getOrderId())
            .orderCode(sc.getOrderCode())
            .staffId(sc.getStaffId())
            .staffName(sc.getStaffName())
            .staffRoleType(sc.getStaffRoleType())
            .roleTypeLabel("PRIMARY".equals(sc.getStaffRoleType()) ? "Nhân viên chính" : "Nhân viên phụ")
            .commissionRate(sc.getCommissionRate())
            .commissionBase(sc.getCommissionBase())
            .commissionAmount(sc.getCommissionAmount())
            .isManualOverride(sc.getIsManualOverride())
            .overrideReason(sc.getOverrideReason())
            .isPaid(sc.getIsPaid())
            .paidAt(sc.getPaidAt())
            .productionStageId(sc.getProductionStageId())
            .periodMonth(sc.getPeriodMonth())
            .periodYear(sc.getPeriodYear())
            .notes(sc.getNotes())
            .createdAt(sc.getCreatedAt())
            .updatedAt(sc.getUpdatedAt())
            .build();
    }

    private ExpenseDTO convertExpenseToDTO(Expense e) {
        return ExpenseDTO.builder()
            .id(e.getId())
            .expenseCode(e.getExpenseCode())
            .category(e.getCategory())
            .categoryLabel(CATEGORY_LABELS.getOrDefault(e.getCategory(), e.getCategory()))
            .title(e.getTitle())
            .description(e.getDescription())
            .amount(e.getAmount())
            .expenseDate(e.getExpenseDate())
            .periodMonth(e.getPeriodMonth())
            .periodYear(e.getPeriodYear())
            .isRecurring(e.getIsRecurring())
            .approvedBy(e.getApprovedBy())
            .approvedAt(e.getApprovedAt())
            .status(e.getStatus())
            .statusLabel(STATUS_LABELS.getOrDefault(e.getStatus(), e.getStatus()))
            .rejectReason(e.getRejectReason())
            .receiptUrl(e.getReceiptUrl())
            .createdAt(e.getCreatedAt())
            .createdBy(e.getCreatedBy())
            .build();
    }

    private void fillExpense(Expense e, ExpenseDTO dto) {
        if (dto.getCategory()    != null) e.setCategory(dto.getCategory());
        if (dto.getTitle()       != null) e.setTitle(dto.getTitle());
        if (dto.getDescription() != null) e.setDescription(dto.getDescription());
        if (dto.getAmount()      != null) e.setAmount(dto.getAmount());
        if (dto.getExpenseDate() != null) {
            e.setExpenseDate(dto.getExpenseDate());
            e.setPeriodMonth((short) dto.getExpenseDate().getMonthValue());
            e.setPeriodYear((short)  dto.getExpenseDate().getYear());
        }
        if (dto.getPeriodMonth() != null) e.setPeriodMonth(dto.getPeriodMonth());
        if (dto.getPeriodYear()  != null) e.setPeriodYear(dto.getPeriodYear());
        if (dto.getIsRecurring() != null) e.setIsRecurring(dto.getIsRecurring());
        if (dto.getReceiptUrl()  != null) e.setReceiptUrl(dto.getReceiptUrl());
    }

    /* ──────────────────────────────────────────────────────────────
       CODES & LABELS
    ────────────────────────────────────────────────────────────── */

    private final AtomicInteger expenseSeq = new AtomicInteger(1);

    private String generateExpenseCode() {
        String ym = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long count = expenseRepo.count() + expenseSeq.getAndIncrement();
        return String.format("EXP-%s-%04d", ym, count % 10000);
    }

    private static final Map<String, String> CATEGORY_LABELS = Map.of(
        "RENT",        "Mặt bằng",
        "UTILITIES",   "Điện / Nước / Internet",
        "MATERIALS",   "Nguyên vật liệu",
        "SALARY",      "Lương nhân viên",
        "MARKETING",   "Marketing",
        "EQUIPMENT",   "Thiết bị",
        "MAINTENANCE", "Bảo trì",
        "OTHER",       "Khác"
    );

    private static final Map<String, String> STATUS_LABELS = Map.of(
        "PENDING",  "Chờ duyệt",
        "APPROVED", "Đã duyệt",
        "REJECTED", "Bị từ chối"
    );
}
