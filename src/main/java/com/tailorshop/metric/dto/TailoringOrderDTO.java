package com.tailorshop.metric.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tailoring Order Data Transfer Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TailoringOrderDTO {

    private Long id;

    /** SỐ BIÊN LẠI */
    private Long orderNumber;

    private String orderCode;

    @NotNull(message = "Khách hàng là bắt buộc")
    private Long customerId;
    
    private String customerName;
    private LocalDate orderDate;
    private LocalDate promisedDate;
    private LocalDate completedDate;
    
    private String orderType;

    private String description;

    private Integer quantity;

    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String status;
    private String paymentStatus;
    private String notes;
    private Boolean isArchived;

    // Phân hệ 3 — Order & Quotation fields
    private String fabricMaterial;
    private String fabricColor;
    private String accessories;
    private Long sourceChannelId;
    private String sourceChannelName;   // populated on read
    private Long quotationId;
    private Long discountCodeId;
    private String discountCode;        // populated on read
    private BigDecimal discountAmount;

    // Đặt cọc (3.6)
    private BigDecimal depositAmount;
    private String depositStatus;       // NONE | PENDING | CONFIRMED
    private LocalDate depositDate;
    private Long depositConfirmedBy;
    private LocalDateTime depositConfirmedAt;

    // 2 nhân viên (3.9)
    private Long primaryStaffId;
    private String primaryStaffName;    // populated on read
    private Long secondaryStaffId;
    private String secondaryStaffName;  // populated on read

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Danh sách sản phẩm (SẢN PHẨM / MÃ SP / MET VẢI / SỐ LƯỢNG / ĐƠN GIÁ / THÀNH TIỀN) */
    private List<OrderItemDTO> items;

}
