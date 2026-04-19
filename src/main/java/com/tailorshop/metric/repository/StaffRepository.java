package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByStaffCode(String staffCode);

    Optional<Staff> findByUserId(Long userId);

    Page<Staff> findByIsActiveTrue(Pageable pageable);

    List<Staff> findByStaffRoleAndIsActiveTrue(String staffRole);

    /** Nhân viên hoạt động, sắp xếp theo số lead hiện tại tăng dần (round-robin cân bằng) */
    @Query("SELECT s FROM Staff s WHERE s.isActive = true AND s.staffRole IN ('SALE', 'MANAGER') " +
           "ORDER BY s.totalLeads ASC")
    List<Staff> findActiveForDistributionByLeadCount();

    /** Nhân viên hiệu suất cao nhất — để phân khách VIP */
    @Query("SELECT s FROM Staff s WHERE s.isActive = true AND s.staffRole IN ('SALE', 'MANAGER') " +
           "ORDER BY s.performanceScore DESC")
    List<Staff> findActiveOrderByPerformanceDesc();

    /** Nhân viên có điểm < ngưỡng — cần approval */
    @Query("SELECT s FROM Staff s WHERE s.isActive = true AND s.performanceScore < :threshold")
    List<Staff> findBelowPerformanceThreshold(@Param("threshold") BigDecimal threshold);

    /** Tìm nhân viên từng phụ trách khách này qua lead đã convert */
    @Query(value = """
            SELECT s.* FROM staff s
            INNER JOIN lead_assignments la ON la.staff_id = s.id AND la.is_current = false
            INNER JOIN leads l ON l.id = la.lead_id AND l.converted_customer_id = :customerId
            ORDER BY la.created_at DESC LIMIT 1
            """, nativeQuery = true)
    Optional<Staff> findPreviousStaffForCustomer(@Param("customerId") Long customerId);

    boolean existsByStaffCode(String staffCode);
}
