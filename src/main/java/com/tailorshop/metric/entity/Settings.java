package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settings Entity
 */
@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(columnDefinition = "TEXT")
    private String settingValue;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String category; // GENERAL, EMAIL, PAYMENT, BUSINESS, SYSTEM

    @Column(length = 50)
    private String dataType; // STRING, INT, DECIMAL, BOOLEAN, JSON

    @Column(nullable = false)
    private Boolean isEditable = true;

}
