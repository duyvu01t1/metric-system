package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.StaffCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffCommissionRepository extends JpaRepository<StaffCommission, Long> {

    List<StaffCommission> findByOrderId(Long orderId);

    List<StaffCommission> findByStaffId(Long staffId);

    Optional<StaffCommission> findByOrderIdAndStaffIdAndStaffRoleType(
            Long orderId, Long staffId, String staffRoleType);

    List<StaffCommission> findByStaffIdAndIsPaidFalse(Long staffId);

    @Query("SELECT COALESCE(SUM(sc.commissionAmount), 0) FROM StaffCommission sc WHERE sc.staffId = :staffId AND sc.isPaid = false")
    BigDecimal sumUnpaidByStaffId(Long staffId);

    // ─── Phân hệ 7 — Finance queries ────────────────────────────────────────

    List<StaffCommission> findByPeriodYearAndPeriodMonth(Short year, Short month);

    List<StaffCommission> findByStaffIdAndPeriodYearAndPeriodMonth(Long staffId, Short year, Short month);

    org.springframework.data.domain.Page<StaffCommission> findByIsPaid(
            Boolean isPaid, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(sc.commissionAmount), 0) FROM StaffCommission sc " +
        "WHERE sc.periodYear = :year AND sc.periodMonth = :month")
    java.math.BigDecimal sumByPeriod(Short year, Short month);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(sc.commissionAmount), 0) FROM StaffCommission sc " +
        "WHERE sc.staffId = :staffId AND sc.periodYear = :year AND sc.periodMonth = :month")
    java.math.BigDecimal sumByStaffAndPeriod(Long staffId, Short year, Short month);

    @org.springframework.data.jpa.repository.Query(
        "SELECT sc.staffId, sc.staffName, COALESCE(SUM(sc.commissionAmount), 0) " +
        "FROM StaffCommission sc " +
        "WHERE sc.periodYear = :year AND sc.periodMonth = :month " +
        "GROUP BY sc.staffId, sc.staffName ORDER BY SUM(sc.commissionAmount) DESC")
    List<Object[]> summarizeByStaffForPeriod(Short year, Short month);

    @org.springframework.data.jpa.repository.Query(
        "SELECT sc FROM StaffCommission sc WHERE " +
        "(:staffId IS NULL OR sc.staffId = :staffId) AND " +
        "(:isPaid  IS NULL OR sc.isPaid  = :isPaid)  AND " +
        "(:year    IS NULL OR sc.periodYear  = :year)  AND " +
        "(:month   IS NULL OR sc.periodMonth = :month) " +
        "ORDER BY sc.createdAt DESC")
    org.springframework.data.domain.Page<StaffCommission> findFiltered(
        Long staffId, Boolean isPaid, Short year, Short month,
        org.springframework.data.domain.Pageable pageable);

    Long countByIsPaid(Boolean isPaid);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(sc.commissionAmount), 0) FROM StaffCommission sc WHERE sc.isPaid = false")
    java.math.BigDecimal sumAllUnpaid();
}
