package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.ProductionStageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionStageLogRepository extends JpaRepository<ProductionStageLog, Long> {

    List<ProductionStageLog> findByStageIdOrderByChangedAtDesc(Long stageId);

    List<ProductionStageLog> findByOrderIdOrderByChangedAtDesc(Long orderId);
}
