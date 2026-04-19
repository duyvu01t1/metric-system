package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Settings Repository
 */
@Repository
public interface SettingsRepository extends JpaRepository<Settings, Long> {

    Optional<Settings> findBySettingKey(String settingKey);

    List<Settings> findByCategory(String category);

    Boolean existsBySettingKey(String settingKey);

}
