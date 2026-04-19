package com.tailorshop.metric.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Root API Controller
 */
@RestController
@RequestMapping("")
@Tag(name = "Root", description = "Root API endpoints")
public class RootController {

    @GetMapping
    @Operation(summary = "API root endpoint")
    public ResponseEntity<?> getRoot() {
        return ResponseEntity.ok("Metric System - Tailoring Management API v1.0.0");
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body(new Object() {
            public final String status = "UP";
            public final String message = "Application is running successfully";
            public final long timestamp = System.currentTimeMillis();
        });
    }

}
