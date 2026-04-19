package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.QCItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QCItemRepository extends JpaRepository<QCItem, Long> {

    List<QCItem> findByQcCheckIdOrderBySortOrderAsc(Long qcCheckId);

    List<QCItem> findByOrderId(Long orderId);

    /** Kiểm tra có item nào FAIL trong phiếu QC không */
    boolean existsByQcCheckIdAndResult(Long qcCheckId, String result);

    /** Đếm số items theo kết quả trong 1 phiếu QC */
    long countByQcCheckIdAndResult(Long qcCheckId, String result);

    /** Đếm tổng items chưa được đánh giá (result = NA) */
    long countByQcCheckIdAndResultNot(Long qcCheckId, String result);

    @Query("SELECT q FROM QCItem q WHERE q.qcCheckId = :qcCheckId AND q.result = 'FAIL' ORDER BY q.sortOrder ASC")
    List<QCItem> findFailedItemsByQcCheckId(Long qcCheckId);
}
