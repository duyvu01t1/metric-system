package com.tailorshop.metric;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Metric System application.
 * Tailoring Shop Management System
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class MetricSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetricSystemApplication.class, args);
    }

}
