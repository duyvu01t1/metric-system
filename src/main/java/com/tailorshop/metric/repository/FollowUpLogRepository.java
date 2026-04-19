package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.FollowUpLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FollowUpLogRepository — Nhật ký liên hệ chăm sóc sau bán hàng (Phân hệ 6)
 */
@Repository
public interface FollowUpLogRepository extends JpaRepository<FollowUpLog, Long> {

    /** Tất cả logs của 1 reminder, mới nhất trước */
    List<FollowUpLog> findByReminderIdOrderByContactedAtDesc(Long reminderId);

    /** Tất cả logs của 1 đơn hàng */
    List<FollowUpLog> findByOrderIdOrderByContactedAtDesc(Long orderId);

    /** Tất cả logs của 1 khách hàng */
    Page<FollowUpLog> findByCustomerIdOrderByContactedAtDesc(Long customerId, Pageable pageable);

    /** Logs của 1 nhân viên trong khoảng thời gian */
    List<FollowUpLog> findByStaffIdAndContactedAtBetween(Long staffId, LocalDateTime from, LocalDateTime to);

    /** Đếm số lần liên hệ của 1 reminder */
    long countByReminderId(Long reminderId);

    /** Đếm số lần liên hệ theo outcome */
    @Query("SELECT COUNT(l) FROM FollowUpLog l WHERE l.outcome = :outcome")
    long countByOutcome(@Param("outcome") String outcome);

    /** Đếm số lần liên hệ hôm nay */
    @Query("SELECT COUNT(l) FROM FollowUpLog l WHERE l.contactedAt >= :from AND l.contactedAt < :to")
    long countContactsToday(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Logs toàn hệ thống phân trang mới nhất trước */
    Page<FollowUpLog> findAllByOrderByContactedAtDesc(Pageable pageable);

    /** Tìm logs có đánh giá khách hàng (để tính trung bình) */
    @Query("SELECT AVG(l.customerRating) FROM FollowUpLog l WHERE l.customerRating IS NOT NULL")
    Double findAverageCustomerRating();

    /** Logs có kết quả REPEAT_ORDER — cơ hội bán lại */
    List<FollowUpLog> findByOutcomeOrderByContactedAtDesc(String outcome);
}
