package com.tailorshop.metric.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {

    private Long id;

    /** SẢN PHẨM */
    @NotBlank(message = "Tên sản phẩm là bắt buộc")
    private String productName;

    /** MÃ SP */
    private String productCode;

    /** MET VẢI */
    @DecimalMin(value = "0", message = "Met vải phải >= 0")
    private BigDecimal fabricMeters;

    /** SỐ LƯỢNG */
    @NotNull(message = "Số lượng là bắt buộc")
    @Min(value = 1, message = "Số lượng phải >= 1")
    private Integer quantity;

    /** ĐƠN GIÁ */
    @NotNull(message = "Đơn giá là bắt buộc")
    @DecimalMin(value = "0", message = "Đơn giá phải >= 0")
    private BigDecimal unitPrice;

    /** THÀNH TIỀN — tính tự động: quantity * unitPrice */
    private BigDecimal totalPrice;

    private Integer sortOrder;
}
