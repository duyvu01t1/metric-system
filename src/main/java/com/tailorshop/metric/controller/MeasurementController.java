package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.MeasurementDTO;
import com.tailorshop.metric.service.MeasurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Measurement Controller
 */
@RestController
@RequestMapping("/measurements")
@RequiredArgsConstructor
@Tag(name = "Measurements", description = "Measurement management endpoints")
public class MeasurementController {

    private final MeasurementService measurementService;

    @GetMapping
    @Operation(summary = "Get all measurements")
    public ResponseEntity<ApiResponse<?>> getAllMeasurements(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<MeasurementDTO> measurements = measurementService.getAllMeasurements(pageable);
            return ResponseEntity.ok(ApiResponse.success("Measurements retrieved successfully", measurements));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }

    @PostMapping
    @Operation(summary = "Create a new measurement")
    public ResponseEntity<ApiResponse<?>> createMeasurement(@Valid @RequestBody MeasurementDTO dto) {
        try {
            MeasurementDTO created = measurementService.createMeasurement(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Measurement created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("CREATION_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get measurement by ID")
    public ResponseEntity<ApiResponse<?>> getMeasurementById(@PathVariable Long id) {
        try {
            MeasurementDTO measurement = measurementService.getMeasurementById(id);
            return ResponseEntity.ok(ApiResponse.success("Measurement retrieved successfully", measurement));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("NOT_FOUND", e.getMessage()));
        }
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get measurements by order ID")
    public ResponseEntity<ApiResponse<?>> getMeasurementsByOrderId(
        @PathVariable Long orderId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<MeasurementDTO> measurements = measurementService.getMeasurementsByOrderId(orderId, pageable);
            return ResponseEntity.ok(ApiResponse.success("Measurements retrieved successfully", measurements));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a measurement")
    public ResponseEntity<ApiResponse<?>> updateMeasurement(
        @PathVariable Long id,
        @Valid @RequestBody MeasurementDTO dto) {
        try {
            MeasurementDTO updated = measurementService.updateMeasurement(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Measurement updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("UPDATE_FAILED", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a measurement")
    public ResponseEntity<ApiResponse<?>> deleteMeasurement(@PathVariable Long id) {
        try {
            measurementService.deleteMeasurement(id);
            return ResponseEntity.ok(ApiResponse.success("Measurement deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("DELETE_FAILED", e.getMessage()));
        }
    }

}
