package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.FollowUpReminder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * FollowUpReminderRepository — Phân hệ 6 (Chăm sóc Sau Bán Hàng)
 */
@Repository
public interface FollowUpReminderRepository extends JpaRepository<FollowUpReminder, Long> {

    /** Tất cả reminders của 1 đơn hàng */
    List<FollowUpReminder> findByOrderIdOrderByReminderDateAsc(Long orderId);

    /** Tất cả reminders của 1 khách hàng */
    List<FollowUpReminder> findByCustomerIdOrderByReminderDateDesc(Long customerId);

    /** Reminders hôm nay chưa thực hiện (status = PENDING) */
    @Query("SELECT r FROM FollowUpReminder r WHERE r.reminderDate = :today AND r.status = 'PENDING' ORDER BY r.priority DESC, r.reminderDate ASC")
    List<FollowUpReminder> findTodayPending(@Param("today") LocalDate today);

    /** Reminders quá hạn (reminderDate < today AND status = PENDING) */
    @Query("SELECT r FROM FollowUpReminder r WHERE r.reminderDate < :today AND r.status = 'PENDING' ORDER BY r.reminderDate ASC")
    List<FollowUpReminder> findOverdue(@Param("today") LocalDate today);

    /** Reminders sắp đến hạn (trong vòng N ngày tới) */
    @Query("SELECT r FROM FollowUpReminder r WHERE r.reminderDate BETWEEN :from AND :to AND r.status = 'PENDING' ORDER BY r.reminderDate ASC")
    List<FollowUpReminder> findUpcoming(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Danh sách phân trang theo status */
    Page<FollowUpReminder> findByStatus(String status, Pageable pageable);

    /** Danh sách theo nhân viên phụ trách + status */
    Page<FollowUpReminder> findByAssignedStaffIdAndStatus(Long staffId, String status, Pageable pageable);

    /** Danh sách theo nhân viên phụ trách */
    Page<FollowUpReminder> findByAssignedStaffId(Long staffId, Pageable pageable);

    /** Kiểm tra đã có reminder loại này cho đơn hàng chưa */
    boolean existsByOrderIdAndReminderType(Long orderId, String reminderType);

    /** Kiểm tra đơn hàng đã có bất kỳ reminder nào chưa */
    boolean existsByOrderId(Long orderId);

    /** Lấy 1 reminder cụ thể theo đơn hàng + loại */
    Optional<FollowUpReminder> findByOrderIdAndReminderType(Long orderId, String reminderType);

    /** Đếm theo status */
    @Query("SELECT COUNT(r) FROM FollowUpReminder r WHERE r.status = :status")
    long countByStatus(@Param("status") String status);

    /** Đếm cần làm hôm nay */
    @Query("SELECT COUNT(r) FROM FollowUpReminder r WHERE r.reminderDate = :today AND r.status = 'PENDING'")
    long countTodayPending(@Param("today") LocalDate today);

    /** Đếm quá hạn */
    @Query("SELECT COUNT(r) FROM FollowUpReminder r WHERE r.reminderDate < :today AND r.status = 'PENDING'")
    long countOverdue(@Param("today") LocalDate today);

    /** Reminders có outcome REPEAT_ORDER gần đây (khách muốn đặt lại) */
    @Query("SELECT DISTINCT r FROM FollowUpReminder r " +
           "WHERE r.status = 'DONE' AND r.orderId IN " +
           "(SELECT l.orderId FROM FollowUpLog l WHERE l.outcome = 'REPEAT_ORDER') " +
           "ORDER BY r.completedAt DESC")
    List<FollowUpReminder> findRepeatOrderOpportunities(Pageable pageable);

    /** Danh sách tất cả (phân trang) */
    Page<FollowUpReminder> findAllByOrderByReminderDateDesc(Pageable pageable);
}
