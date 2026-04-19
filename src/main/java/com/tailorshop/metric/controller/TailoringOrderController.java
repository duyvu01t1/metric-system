package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.TailoringOrderDTO;
import com.tailorshop.metric.service.TailoringOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Tailoring Order Controller
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Tailoring Orders", description = "Tailoring order management endpoints")
public class TailoringOrderController {

    private final TailoringOrderService tailoringOrderService;

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<ApiResponse<?>> createOrder(@RequestBody TailoringOrderDTO dto) {
        try {
            TailoringOrderDTO created = tailoringOrderService.createOrder(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ORDER_CREATION_FAILED", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all orders")
    public ResponseEntity<ApiResponse<?>> getAllOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TailoringOrderDTO> orders = tailoringOrderService.getAllOrders(pageable);
            return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<?>> getOrderById(@PathVariable Long id) {
        try {
            TailoringOrderDTO order = tailoringOrderService.getOrderById(id);
            return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ORDER_NOT_FOUND", e.getMessage()));
        }
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get orders by customer ID")
    public ResponseEntity<ApiResponse<?>> getOrdersByCustomerId(
        @PathVariable Long customerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TailoringOrderDTO> orders = tailoringOrderService.getOrdersByCustomerId(customerId, pageable);
            return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status")
    public ResponseEntity<ApiResponse<?>> getOrdersByStatus(
        @PathVariable String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TailoringOrderDTO> orders = tailoringOrderService.getOrdersByStatus(status, pageable);
            return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an order")
    public ResponseEntity<ApiResponse<?>> updateOrder(
        @PathVariable Long id,
        @RequestBody TailoringOrderDTO dto) {
        try {
            TailoringOrderDTO updated = tailoringOrderService.updateOrder(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Order updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("UPDATE_FAILED", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive an order")
    public ResponseEntity<ApiResponse<?>> deleteOrder(@PathVariable Long id) {
        try {
            tailoringOrderService.deleteOrder(id);
            return ResponseEntity.ok(ApiResponse.success("Order archived successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("DELETE_FAILED", e.getMessage()));
        }
    }

}
