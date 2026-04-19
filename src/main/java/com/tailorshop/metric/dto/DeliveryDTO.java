package com.tailorshop.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Delivery DTO — phiếu giao hàng & tất toán
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryDTO {

    private Long id;
    private Long orderId;
    private String orderCode;           // populated on read
    private String customerName;        // populated on read
    private BigDecimal totalPrice;      // populated on read
    private String paymentStatus;       // populated on read (từ order)

    private Long qcCheckId;
    private String qcNumber;            // populated on read

    private String deliveryCode;
    private String status;              // SCHEDULED | OUT_FOR_DELIVERY | DELIVERED | RETURNED | CANCELLED
    private String statusLabel;         // populated on read

    private LocalDate scheduledDate;
    private LocalDate actualDeliveryDate;

    private String deliveryMethod;      // PICKUP | SHIP | STAFF_DELIVERY
    private String deliveryMethodLabel; // populated on read

    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;

    // Tất toán
    private BigDecimal remainingAmount;
    private BigDecimal amountCollected;
    private String paymentMethod;
    private Boolean isFullyPaid;

    // Xác nhận
    private Long confirmedBy;
    private String confirmedByName;     // populated on read
    private LocalDateTime confirmedAt;
    private String receiptSignature;

    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
