package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.PaymentDTO;
import com.tailorshop.metric.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Payment Controller
 * REST API endpoints for payment management
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create a new payment
     * @param paymentDTO Payment data
     * @return Created payment
     */
    @PostMapping
    @Operation(summary = "Create a new payment")
    public ResponseEntity<ApiResponse<PaymentDTO>> createPayment(@Valid @RequestBody PaymentDTO paymentDTO) {
        log.info("Creating new payment for order: {}", paymentDTO.getTailoringOrderId());
        PaymentDTO created = paymentService.createPayment(paymentDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Payment created successfully", created));
    }

    /**
     * Get payment by ID
     * @param id Payment ID
     * @return Payment data
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPayment(@PathVariable Long id) {
        log.info("Getting payment with ID: {}", id);
        PaymentDTO payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", payment));
    }

    /**
     * Get all payments with pagination
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Page of payments
     */
    @GetMapping
    @Operation(summary = "Get all payments")
    public ResponseEntity<ApiResponse<Page<PaymentDTO>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting all payments - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentDTO> payments = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", payments));
    }

    /**
     * Get payments by order ID
     * @param orderId Order ID
     * @return List of payments for the order
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payments by order ID")
    public ResponseEntity<ApiResponse<List<PaymentDTO>>> getPaymentsByOrder(@PathVariable Long orderId) {
        log.info("Getting payments for order: {}", orderId);
        List<PaymentDTO> payments = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", payments));
    }

    /**
     * Update payment
     * @param id Payment ID
     * @param paymentDTO Updated payment data
     * @return Updated payment
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update payment")
    public ResponseEntity<ApiResponse<PaymentDTO>> updatePayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentDTO paymentDTO) {
        log.info("Updating payment with ID: {}", id);
        PaymentDTO updated = paymentService.updatePayment(id, paymentDTO);
        return ResponseEntity.ok(ApiResponse.success("Payment updated successfully", updated));
    }

    /**
     * Delete payment
     * @param id Payment ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete payment")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id) {
        log.info("Deleting payment with ID: {}", id);
        paymentService.deletePayment(id);
        return ResponseEntity.ok(ApiResponse.success("Payment deleted successfully", null));
    }
}
