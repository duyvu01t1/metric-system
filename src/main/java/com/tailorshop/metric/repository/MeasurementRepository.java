package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.Measurement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Measurement Repository
 */
@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    List<Measurement> findByTailoringOrderId(Long tailoringOrderId);

    Page<Measurement> findByTailoringOrderId(Long tailoringOrderId, Pageable pageable);

    List<Measurement> findByMeasurementTemplateId(Long templateId);

}
