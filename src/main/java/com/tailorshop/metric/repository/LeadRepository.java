package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lead Repository
 */
@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByLeadCode(String leadCode);

    Page<Lead> findByStatus(String status, Pageable pageable);

    Page<Lead> findByChannelId(Long channelId, Pageable pageable);

    Page<Lead> findByAssignedStaffId(Long staffId, Pageable pageable);

    Page<Lead> findByChannelIdAndStatus(Long channelId, String status, Pageable pageable);

    /**
     * Tìm kiếm lead theo tên, SĐT, email
     */
    @Query("SELECT l FROM Lead l WHERE " +
           "LOWER(l.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "l.phone LIKE CONCAT('%', :q, '%') OR " +
           "LOWER(l.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Lead> search(@Param("q") String q, Pageable pageable);

    /**
     * Lấy leads cần follow-up trong hôm nay
     */
    @Query("SELECT l FROM Lead l WHERE l.followupAt BETWEEN :start AND :end AND l.status NOT IN ('CONVERTED','LOST')")
    List<Lead> findFollowUpToday(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Thống kê số lượng lead theo trạng thái
     */
    @Query("SELECT l.status, COUNT(l) FROM Lead l GROUP BY l.status")
    List<Object[]> countByStatus();

    /**
     * Thống kê số lượng lead theo kênh
     */
    @Query("SELECT l.channel.channelCode, COUNT(l) FROM Lead l GROUP BY l.channel.channelCode")
    List<Object[]> countByChannel();

    /**
     * Tìm lead đã có SĐT trùng — xác định khách cũ
     */
    @Query("SELECT l FROM Lead l WHERE l.phone = :phone AND l.status = 'CONVERTED' ORDER BY l.convertedAt DESC")
    List<Lead> findConvertedByPhone(@Param("phone") String phone);

    /**
     * Đếm lead được tạo theo khoảng thời gian
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
