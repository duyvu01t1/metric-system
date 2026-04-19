package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Delivery> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    Page<Delivery> findByStatus(String status, Pageable pageable);

    List<Delivery> findByScheduledDateBetween(LocalDate start, LocalDate end);

    List<Delivery> findByScheduledDate(LocalDate date);

    boolean existsByOrderIdAndStatus(Long orderId, String status);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.status = :status")
    long countByStatus(String status);

    /** Đơn hàng hẹn giao hôm nay chưa được giao */
    @Query("SELECT d FROM Delivery d WHERE d.scheduledDate = :today AND d.status = 'SCHEDULED' ORDER BY d.scheduledDate ASC")
    List<Delivery> findTodayPending(LocalDate today);
}
