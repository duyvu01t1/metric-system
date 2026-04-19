package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * API Access Log Entity for monitoring API usage
 */
@Entity
@Table(name = "api_access_logs", indexes = {
    @Index(name = "idx_api_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_api_logs_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long userId;

    @Column(length = 255)
    private String endpoint;

    @Column(length = 10)
    private String method; // GET, POST, PUT, DELETE

    @Column
    private Integer statusCode;

    @Column
    private Integer requestDurationMs;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
