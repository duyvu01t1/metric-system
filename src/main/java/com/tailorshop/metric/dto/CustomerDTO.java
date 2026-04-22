package com.tailorshop.metric.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer Data Transfer Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {

    private Long id;
    private String customerCode;
    
    @NotBlank(message = "Họ là bắt buộc")
    private String firstName;
    
    @NotBlank(message = "Tên là bắt buộc")
    private String lastName;
    
    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không đúng định dạng")
    private String email;
    
    @NotBlank(message = "Số điện thoại là bắt buộc")
    private String phone;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private String identificationNumber;
    private String identificationType;
    private LocalDate dateOfBirth;
    private String gender;
    private String notes;
    private Boolean isActive;

    // CRM fields
    private Long assignedStaffId;
    private String assignedStaffName;   // populated on read
    private BigDecimal cac;
    private Integer interactionCount;
    private Long sourceChannelId;
    private String sourceChannelName;   // populated on read
    private LocalDateTime lastInteractionAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
