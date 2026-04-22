package com.tailorshop.metric.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Data Transfer Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentDTO {

    private Long id;
    
    @NotNull(message = "Mã đơn hàng là bắt buộc")
    private Long tailoringOrderId;
    
    private String orderCode;
    private String customerName;
    
    @NotNull(message = "Số tiền là bắt buộc")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Phương thức thanh toán là bắt buộc")
    private String paymentMethod;
    
    private String transactionReference;
    
    @NotNull(message = "Ngày thanh toán là bắt buộc")
    private LocalDateTime paymentDate;
    private String notes;
    private LocalDateTime createdAt;
    private Long createdBy;

    // Additional fields for display
    private String status;

}
