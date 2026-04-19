package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Measurement Field Entity
 */
@Entity
@Table(name = "measurement_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private MeasurementTemplate template;

    @Column(nullable = false, length = 100)
    private String fieldName;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(length = 20)
    private String unit; // CM, INCH, etc.

    @Column
    private Integer fieldOrder;

    @Column(nullable = false)
    private Boolean isRequired = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
