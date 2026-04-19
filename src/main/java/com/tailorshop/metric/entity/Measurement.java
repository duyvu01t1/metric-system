package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Measurement Entity
 */
@Entity
@Table(name = "measurements", indexes = {
    @Index(name = "idx_measurements_order_id", columnList = "tailoring_order_id"),
    @Index(name = "idx_measurements_template_id", columnList = "measurement_template_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tailoring_order_id", nullable = false)
    private TailoringOrder tailoringOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measurement_template_id")
    private MeasurementTemplate measurementTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id")
    private MeasurementField field;

    @Column(length = 100)
    private String fieldName;

    @Column(precision = 10, scale = 2)
    private BigDecimal value;

    @Column(length = 20)
    private String unit;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column
    private LocalDateTime measuredAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private Long createdBy;

}
