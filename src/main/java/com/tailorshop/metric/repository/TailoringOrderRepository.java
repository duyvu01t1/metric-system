package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.TailoringOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Tailoring Order Repository
 */
@Repository
public interface TailoringOrderRepository extends JpaRepository<TailoringOrder, Long> {

    Optional<TailoringOrder> findByOrderCode(String orderCode);

    Page<TailoringOrder> findByCustomerId(Long customerId, Pageable pageable);

    Page<TailoringOrder> findByStatus(String status, Pageable pageable);

    Page<TailoringOrder> findByOrderDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    Long countByStatus(String status);

    Long countByOrderType(String orderType);

    Boolean existsByOrderCode(String orderCode);

    /** Đơn hàng COMPLETED trong khoảng ngày (dùng cho Scheduler phân hệ 6) */
    List<TailoringOrder> findByStatusAndCompletedDateBetween(String status, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(t.sourceChannelId, 0), COUNT(t), COALESCE(SUM(t.totalPrice), 0) " +
           "FROM TailoringOrder t GROUP BY t.sourceChannelId ORDER BY COALESCE(SUM(t.totalPrice), 0) DESC")
    List<Object[]> summarizeRevenueByChannel();

    @Query("SELECT COALESCE(t.primaryStaffId, 0), COUNT(t), COALESCE(SUM(t.totalPrice), 0) " +
           "FROM TailoringOrder t GROUP BY t.primaryStaffId ORDER BY COALESCE(SUM(t.totalPrice), 0) DESC")
    List<Object[]> summarizeRevenueByPrimaryStaff();

    @Query("SELECT t.status, COUNT(t) FROM TailoringOrder t GROUP BY t.status")
    List<Object[]> summarizeByStatus();

    @Query("SELECT t.orderType, COUNT(t), COALESCE(SUM(t.totalPrice), 0) FROM TailoringOrder t " +
           "GROUP BY t.orderType ORDER BY COUNT(t) DESC")
    List<Object[]> summarizeByOrderType();

}
