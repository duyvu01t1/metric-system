package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.SettingsDTO;
import com.tailorshop.metric.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Settings Controller
 * REST API endpoints for settings management
 */
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Settings", description = "Settings management endpoints")
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * Get all settings
     * @return List of all settings
     */
    @GetMapping
    @Operation(summary = "Get all settings")
    public ResponseEntity<ApiResponse<List<SettingsDTO>>> getAllSettings() {
        log.info("Getting all settings");
        List<SettingsDTO> settings = settingsService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success("Settings retrieved successfully", settings));
    }

    /**
     * Get settings by category
     * @param category Settings category
     * @return List of settings in category
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "Get settings by category")
    public ResponseEntity<ApiResponse<List<SettingsDTO>>> getSettingsByCategory(
            @PathVariable String category) {
        log.info("Getting settings by category: {}", category);
        List<SettingsDTO> settings = settingsService.getSettingsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success("Settings retrieved successfully", settings));
    }

    /**
     * Get setting by key
     * @param key Setting key
     * @return Setting data
     */
    @GetMapping("/key/{key}")
    @Operation(summary = "Get setting by key")
    public ResponseEntity<ApiResponse<SettingsDTO>> getSettingByKey(@PathVariable String key) {
        log.info("Getting setting by key: {}", key);
        SettingsDTO setting = settingsService.getSettingByKey(key);
        return ResponseEntity.ok(ApiResponse.success("Setting retrieved successfully", setting));
    }

    /**
     * Get setting by ID
     * @param id Setting ID
     * @return Setting data
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get setting by ID")
    public ResponseEntity<ApiResponse<SettingsDTO>> getSetting(@PathVariable Long id) {
        log.info("Getting setting with ID: {}", id);
        SettingsDTO setting = settingsService.getSettingByKey(String.valueOf(id));
        return ResponseEntity.ok(ApiResponse.success("Setting retrieved successfully", setting));
    }

    /**
     * Update setting
     * @param id Setting ID
     * @param settingsDTO Updated setting data
     * @return Updated setting
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update setting")
    public ResponseEntity<ApiResponse<SettingsDTO>> updateSetting(
            @PathVariable Long id,
            @Valid @RequestBody SettingsDTO settingsDTO) {
        log.info("Updating setting with ID: {}", id);
        SettingsDTO updated = settingsService.updateSetting(id, settingsDTO);
        return ResponseEntity.ok(ApiResponse.success("Setting updated successfully", updated));
    }

    /**
     * Update setting by key
     * @param key Setting key
     * @param value New setting value
     * @return Updated setting
     */
    @PutMapping("/key/{key}")
    @Operation(summary = "Update setting by key")
    public ResponseEntity<ApiResponse<SettingsDTO>> updateSettingByKey(
            @PathVariable String key,
            @RequestParam String value) {
        log.info("Updating setting by key: {}", key);
        SettingsDTO updated = settingsService.updateSettingByKey(key, value);
        return ResponseEntity.ok(ApiResponse.success("Setting updated successfully", updated));
    }

    /**
     * Create new setting
     * @param settingsDTO Setting data
     * @return Created setting
     */
    @PostMapping
    @Operation(summary = "Create new setting")
    public ResponseEntity<ApiResponse<SettingsDTO>> createSetting(
            @Valid @RequestBody SettingsDTO settingsDTO) {
        log.info("Creating new setting: {}", settingsDTO.getSettingKey());
        SettingsDTO created = settingsService.createSetting(settingsDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Setting created successfully", created));
    }
}
