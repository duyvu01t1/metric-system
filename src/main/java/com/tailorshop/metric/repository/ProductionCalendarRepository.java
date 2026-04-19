package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.ProductionCalendar;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductionCalendarRepository extends JpaRepository<ProductionCalendar, Long> {

    List<ProductionCalendar> findByOrderId(Long orderId);

    List<ProductionCalendar> findByStaffIdAndCalendarType(Long staffId, String calendarType);

    List<ProductionCalendar> findByCalendarTypeAndEventStartBetween(
            String calendarType, LocalDateTime start, LocalDateTime end);

    List<ProductionCalendar> findByStaffIdAndEventStartBetween(
            Long staffId, LocalDateTime start, LocalDateTime end);

    List<ProductionCalendar> findByEventStartBetween(
            LocalDateTime start, LocalDateTime end);

    @Modifying
    @Transactional
    void deleteByStageId(Long stageId);

    @Modifying
    @Transactional
    void deleteByOrderId(Long orderId);
}
