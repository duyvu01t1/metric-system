package com.tailorshop.metric.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settings Data Transfer Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettingsDTO {

    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;
    private String category; // GENERAL, EMAIL, PAYMENT, BUSINESS, SYSTEM
    private String dataType; // STRING, INT, DECIMAL, BOOLEAN, JSON
    private Boolean isEditable;

}
