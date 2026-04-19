package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.MeasurementDTO;
import com.tailorshop.metric.entity.Measurement;
import com.tailorshop.metric.entity.TailoringOrder;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.MeasurementRepository;
import com.tailorshop.metric.repository.TailoringOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Measurement Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final TailoringOrderRepository tailoringOrderRepository;

    /**
     * Create a new measurement
     */
    @Transactional
    public MeasurementDTO createMeasurement(MeasurementDTO dto) {
        TailoringOrder order = tailoringOrderRepository.findById(dto.getTailoringOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Measurement measurement = new Measurement();
        measurement.setTailoringOrder(order);
        measurement.setFieldName(dto.getFieldName());
        measurement.setValue(dto.getValue());
        measurement.setUnit(dto.getUnit());
        measurement.setNotes(dto.getNotes());
        measurement.setMeasuredAt(LocalDateTime.now());

        Measurement savedMeasurement = measurementRepository.save(measurement);
        log.info("Measurement created for order: {}", order.getOrderCode());

        return convertToDTO(savedMeasurement);
    }

    /**
     * Update measurement
     */
    @Transactional
    public MeasurementDTO updateMeasurement(Long id, MeasurementDTO dto) {
        Measurement measurement = measurementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Measurement not found"));

        measurement.setFieldName(dto.getFieldName());
        measurement.setValue(dto.getValue());
        measurement.setUnit(dto.getUnit());
        measurement.setNotes(dto.getNotes());

        Measurement updated = measurementRepository.save(measurement);
        log.info("Measurement updated: {}", id);

        return convertToDTO(updated);
    }

    /**
     * Get measurement by ID
     */
    public MeasurementDTO getMeasurementById(Long id) {
        Measurement measurement = measurementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Measurement not found"));
        return convertToDTO(measurement);
    }

    /**
     * Get measurements by order ID
     */
    public Page<MeasurementDTO> getMeasurementsByOrderId(Long orderId, Pageable pageable) {
        Page<Measurement> measurements = measurementRepository.findByTailoringOrderId(orderId, pageable);
        List<MeasurementDTO> dtos = measurements.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, measurements.getTotalElements());
    }

    /**
     * Delete measurement
     */
    @Transactional
    public void deleteMeasurement(Long id) {
        Measurement measurement = measurementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Measurement not found"));
        measurementRepository.delete(measurement);
        log.info("Measurement deleted: {}", id);
    }

    /**
     * Convert to DTO
     */
    private MeasurementDTO convertToDTO(Measurement measurement) {
        return MeasurementDTO.builder()
            .id(measurement.getId())
            .tailoringOrderId(measurement.getTailoringOrder().getId())
            .orderCode(measurement.getTailoringOrder().getOrderCode())
            .fieldName(measurement.getFieldName())
            .value(measurement.getValue())
            .unit(measurement.getUnit())
            .notes(measurement.getNotes())
            .measuredAt(measurement.getMeasuredAt())
            .createdAt(measurement.getCreatedAt())
            .updatedAt(measurement.getUpdatedAt())
            .build();
    }
}
