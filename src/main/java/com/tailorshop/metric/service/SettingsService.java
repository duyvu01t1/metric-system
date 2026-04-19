package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.SettingsDTO;
import com.tailorshop.metric.entity.Settings;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.SettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Settings Service
 * Handles business logic for settings management
 */
@Service
@Slf4j
public class SettingsService {

    @Autowired
    private SettingsRepository settingsRepository;

    /**
     * Get all settings
     * @return List of settings
     */
    @Transactional(readOnly = true)
    public List<SettingsDTO> getAllSettings() {
        log.info("Getting all settings");
        return settingsRepository.findAll()
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get settings by category
     * @param category Settings category
     * @return List of settings in category
     */
    @Transactional(readOnly = true)
    public List<SettingsDTO> getSettingsByCategory(String category) {
        log.info("Getting settings by category: {}", category);
        return settingsRepository.findByCategory(category)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get setting by key
     * @param settingKey Setting key
     * @return Setting value
     */
    @Transactional(readOnly = true)
    public SettingsDTO getSettingByKey(String settingKey) {
        log.info("Getting setting by key: {}", settingKey);
        Settings setting = settingsRepository.findBySettingKey(settingKey)
            .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + settingKey));
        return convertToDTO(setting);
    }

    /**
     * Update setting
     * @param id Setting ID
     * @param settingsDTO Updated setting data
     * @return Updated setting
     */
    @Transactional
    public SettingsDTO updateSetting(Long id, SettingsDTO settingsDTO) {
        log.info("Updating setting with ID: {}", id);
        
        Settings setting = settingsRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Setting not found with id: " + id));
        
        if (!setting.getIsEditable()) {
            throw new RuntimeException("This setting is not editable");
        }
        
        setting.setSettingValue(settingsDTO.getSettingValue());
        setting.setDescription(settingsDTO.getDescription());
        
        Settings updated = settingsRepository.save(setting);
        log.info("Setting updated with ID: {}", id);
        
        return convertToDTO(updated);
    }

    /**
     * Update setting by key
     * @param settingKey Setting key
     * @param settingValue New setting value
     * @return Updated setting
     */
    @Transactional
    public SettingsDTO updateSettingByKey(String settingKey, String settingValue) {
        log.info("Updating setting by key: {}", settingKey);
        
        Settings setting = settingsRepository.findBySettingKey(settingKey)
            .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + settingKey));
        
        if (!setting.getIsEditable()) {
            throw new RuntimeException("This setting is not editable");
        }
        
        setting.setSettingValue(settingValue);
        
        Settings updated = settingsRepository.save(setting);
        log.info("Setting updated with key: {}", settingKey);
        
        return convertToDTO(updated);
    }

    /**
     * Create new setting
     * @param settingsDTO Setting data
     * @return Created setting
     */
    @Transactional
    public SettingsDTO createSetting(SettingsDTO settingsDTO) {
        log.info("Creating new setting: {}", settingsDTO.getSettingKey());
        
        if (settingsRepository.existsBySettingKey(settingsDTO.getSettingKey())) {
            throw new RuntimeException("Setting with key '" + settingsDTO.getSettingKey() + "' already exists");
        }
        
        Settings setting = convertToEntity(settingsDTO);
        Settings saved = settingsRepository.save(setting);
        
        log.info("Setting created with key: {}", settingsDTO.getSettingKey());
        return convertToDTO(saved);
    }

    /**
     * Convert Entity to DTO
     */
    private SettingsDTO convertToDTO(Settings setting) {
        return SettingsDTO.builder()
            .id(setting.getId())
            .settingKey(setting.getSettingKey())
            .settingValue(setting.getSettingValue())
            .description(setting.getDescription())
            .category(setting.getCategory())
            .dataType(setting.getDataType())
            .isEditable(setting.getIsEditable())
            .build();
    }

    /**
     * Convert DTO to Entity
     */
    private Settings convertToEntity(SettingsDTO settingsDTO) {
        Settings setting = new Settings();
        setting.setSettingKey(settingsDTO.getSettingKey());
        setting.setSettingValue(settingsDTO.getSettingValue());
        setting.setDescription(settingsDTO.getDescription());
        setting.setCategory(settingsDTO.getCategory());
        setting.setDataType(settingsDTO.getDataType());
        setting.setIsEditable(settingsDTO.getIsEditable() != null ? settingsDTO.getIsEditable() : true);
        return setting;
    }
}
