package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.ProductionStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {

    /** Lấy tất cả công đoạn của một đơn hàng, sắp xếp theo thứ tự */
    List<ProductionStage> findByOrderIdOrderByStageOrderAsc(Long orderId);

    /** Tìm công đoạn theo orderId + stageType */
    Optional<ProductionStage> findByOrderIdAndStageType(Long orderId, String stageType);

    /** Kiểm tra xem đơn hàng đã có stages chưa */
    boolean existsByOrderId(Long orderId);

    /** Đếm số stages theo status trong 1 đơn hàng */
    long countByOrderIdAndStatus(Long orderId, String status);

    /** Tìm stages đang hoạt động của thợ may (không COMPLETED/SKIPPED) */
    @Query("SELECT s FROM ProductionStage s WHERE s.assignedWorkerId = :workerId AND s.status NOT IN ('COMPLETED','SKIPPED')")
    List<ProductionStage> findActiveByWorkerId(Long workerId);

    /** Tìm stages đang hoạt động của sale */
    @Query("SELECT s FROM ProductionStage s WHERE s.assignedSaleId = :saleId AND s.status NOT IN ('COMPLETED','SKIPPED')")
    List<ProductionStage> findActiveBySaleId(Long saleId);

    /** Tìm theo alert status (cho dashboard cảnh báo) */
    List<ProductionStage> findByAlertStatus(String alertStatus);

    /** Tìm stages của thợ trong khoảng ngày kế hoạch */
    List<ProductionStage> findByAssignedWorkerIdAndPlannedEndDateBetween(
            Long workerId, LocalDate startDate, LocalDate endDate);

    /** Tìm stages chưa hoàn thành có planned_end_date quá hạn (cho batch job cập nhật alert) */
    @Query("SELECT s FROM ProductionStage s WHERE s.status NOT IN ('COMPLETED','SKIPPED') AND s.plannedEndDate IS NOT NULL")
    List<ProductionStage> findAllPendingOrInProgress();

    /** Lấy công đoạn ngay trước trong cùng đơn hàng */
    Optional<ProductionStage> findByOrderIdAndStageOrder(Long orderId, Integer stageOrder);
}
