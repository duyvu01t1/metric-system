package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.OrderItemDTO;
import com.tailorshop.metric.dto.TailoringOrderDTO;
import com.tailorshop.metric.service.TailoringOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Tailoring Orders", description = "Order management endpoints")
public class TailoringOrderController {

    private final TailoringOrderService tailoringOrderService;

    // ── Danh sách + tìm kiếm ─────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Lấy danh sách / tìm kiếm đơn hàng")
    public ResponseEntity<ApiResponse<?>> getOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String sort,
            @RequestParam(required = false)    String orderNumber,
            @RequestParam(required = false)    String customerName,
            @RequestParam(required = false)    String phone) {
        try {
            Pageable pageable = buildPageable(page, size, sort, "orderNumber", Sort.Direction.DESC);
            Page<TailoringOrderDTO> result;

            boolean hasSearch = (orderNumber != null && !orderNumber.isBlank())
                             || (customerName != null && !customerName.isBlank())
                             || (phone != null && !phone.isBlank());

            result = hasSearch
                ? tailoringOrderService.searchOrders(orderNumber, customerName, phone, pageable)
                : tailoringOrderService.getAllOrders(pageable);

            return ResponseEntity.ok(ApiResponse.success("OK", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy đơn hàng theo ID")
    public ResponseEntity<ApiResponse<?>> getOrderById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("OK", tailoringOrderService.getOrderById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("NOT_FOUND", e.getMessage()));
        }
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Lấy đơn hàng theo khách hàng")
    public ResponseEntity<ApiResponse<?>> getByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(ApiResponse.success("OK",
                tailoringOrderService.getOrdersByCustomerId(customerId, pageable)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Lấy đơn hàng theo trạng thái")
    public ResponseEntity<ApiResponse<?>> getByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(ApiResponse.success("OK",
                tailoringOrderService.getOrdersByStatus(status, pageable)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Tạo đơn hàng mới")
    public ResponseEntity<ApiResponse<?>> createOrder(@RequestBody TailoringOrderDTO dto) {
        try {
            TailoringOrderDTO created = tailoringOrderService.createOrder(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Created", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("CREATE_FAILED", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật đơn hàng")
    public ResponseEntity<ApiResponse<?>> updateOrder(
            @PathVariable Long id,
            @RequestBody TailoringOrderDTO dto) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Updated", tailoringOrderService.updateOrder(id, dto)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("UPDATE_FAILED", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Lưu trữ đơn hàng")
    public ResponseEntity<ApiResponse<?>> deleteOrder(@PathVariable Long id) {
        try {
            tailoringOrderService.deleteOrder(id);
            return ResponseEntity.ok(ApiResponse.success("Archived"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("DELETE_FAILED", e.getMessage()));
        }
    }

    // ── Items (CHI TIẾT SẢN PHẨM) ────────────────────────────────────────────

    @GetMapping("/{id}/items")
    @Operation(summary = "Lấy danh sách sản phẩm của đơn hàng")
    public ResponseEntity<ApiResponse<?>> getItems(@PathVariable Long id) {
        try {
            List<OrderItemDTO> items = tailoringOrderService.getOrderById(id).getItems();
            return ResponseEntity.ok(ApiResponse.success("OK", items));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }

    @PutMapping("/{id}/items")
    @Operation(summary = "Thay thế toàn bộ sản phẩm của đơn hàng")
    public ResponseEntity<ApiResponse<?>> updateItems(
            @PathVariable Long id,
            @RequestBody List<OrderItemDTO> items) {
        try {
            TailoringOrderDTO dto = tailoringOrderService.getOrderById(id);
            dto.setItems(items);
            return ResponseEntity.ok(ApiResponse.success("Updated", tailoringOrderService.updateOrder(id, dto)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("UPDATE_FAILED", e.getMessage()));
        }
    }

    // ── utility ───────────────────────────────────────────────────────────────

    private Pageable buildPageable(int page, int size, String sort,
                                   String defaultField, Sort.Direction defaultDir) {
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
            return PageRequest.of(page, size, Sort.by(dir, parts[0].trim()));
        }
        return PageRequest.of(page, size, Sort.by(defaultDir, defaultField));
    }
}
