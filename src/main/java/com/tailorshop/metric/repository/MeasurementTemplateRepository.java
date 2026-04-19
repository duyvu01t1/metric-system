package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.MeasurementTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Measurement Template Repository
 */
@Repository
public interface MeasurementTemplateRepository extends JpaRepository<MeasurementTemplate, Long> {

    Optional<MeasurementTemplate> findByNameAndOrderType(String name, String orderType);

    List<MeasurementTemplate> findByOrderTypeAndIsActiveTrue(String orderType);

    List<MeasurementTemplate> findByIsActiveTrue();

}
