package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.OrderItemDTO;
import com.tailorshop.metric.dto.TailoringOrderDTO;
import com.tailorshop.metric.service.TailoringOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Tailoring Orders", description = "Tailoring order management endpoints")
public class TailoringOrderController {

    private final TailoringOrderService tailoringOrderService;

    // ── CRUD đơn hàng ────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Tạo đơn hàng mới")
    public ResponseEntity<ApiResponse<?>> createOrder(@Valid @RequestBody TailoringOrderDTO dto) {
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
    @Operation(summary = "Lấy danh sách đơn hàng (phân trang)")
    public ResponseEntity<ApiResponse<?>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort) {
        try {
            int actualSize = limit != null ? limit : size;
            Pageable pageable;
            if (sort != null && !sort.trim().isEmpty()) {
                String[] parts = sort.split(",");
                Sort.Direction dir = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
                pageable = PageRequest.of(page, actualSize, Sort.by(dir, parts[0]));
            } else {
                pageable = PageRequest.of(page, actualSize, Sort.by(Sort.Direction.DESC, "orderNumber"));
            }
            Page<TailoringOrderDTO> orders = tailoringOrderService.getAllOrders(pageable);
            return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy đơn hàng theo ID")
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
    @Operation(summary = "Lấy đơn hàng theo khách hàng")
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
    @Operation(summary = "Lấy đơn hàng theo trạng thái")
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
    @Operation(summary = "Cập nhật đơn hàng")
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
    @Operation(summary = "Lưu trữ đơn hàng (soft delete)")
    public ResponseEntity<ApiResponse<?>> deleteOrder(@PathVariable Long id) {
        try {
            tailoringOrderService.deleteOrder(id);
            return ResponseEntity.ok(ApiResponse.success("Order archived successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("DELETE_FAILED", e.getMessage()));
        }
    }

    // ── Quản lý items trong đơn hàng ─────────────────────────────────────────

    @GetMapping("/{id}/items")
    @Operation(summary = "Lấy danh sách sản phẩm của đơn hàng")
    public ResponseEntity<ApiResponse<?>> getOrderItems(@PathVariable Long id) {
        try {
            TailoringOrderDTO order = tailoringOrderService.getOrderById(id);
            return ResponseEntity.ok(ApiResponse.success("Items retrieved successfully", order.getItems()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }

    @PutMapping("/{id}/items")
    @Operation(summary = "Cập nhật toàn bộ sản phẩm của đơn hàng")
    public ResponseEntity<ApiResponse<?>> updateOrderItems(
            @PathVariable Long id,
            @Valid @RequestBody List<OrderItemDTO> items) {
        try {
            TailoringOrderDTO dto = tailoringOrderService.getOrderById(id);
            dto.setItems(items);
            TailoringOrderDTO updated = tailoringOrderService.updateOrder(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Items updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("UPDATE_FAILED", e.getMessage()));
        }
    }
}
