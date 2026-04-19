package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.QCCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QCCheckRepository extends JpaRepository<QCCheck, Long> {

    List<QCCheck> findByOrderIdOrderByCheckRoundDesc(Long orderId);

    /** Lấy phiếu QC mới nhất của đơn hàng */
    Optional<QCCheck> findTopByOrderIdOrderByCheckRoundDesc(Long orderId);

    /** Kiểm tra xem đơn hàng có phiếu QC nào đang PASSED không */
    boolean existsByOrderIdAndStatus(Long orderId, String status);

    Page<QCCheck> findByStatus(String status, Pageable pageable);

    Page<QCCheck> findByCheckedBy(Long checkedBy, Pageable pageable);

    @Query("SELECT COUNT(q) FROM QCCheck q WHERE q.status = :status")
    long countByStatus(String status);

    /** Lấy phiếu QC PASSED mới nhất của đơn hàng */
    Optional<QCCheck> findTopByOrderIdAndStatusOrderByCheckRoundDesc(Long orderId, String status);
}
