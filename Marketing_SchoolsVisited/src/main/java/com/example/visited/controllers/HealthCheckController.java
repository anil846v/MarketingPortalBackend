package com.example.visited.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthCheckController {

    private final DataSource dataSource;

    public HealthCheckController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<?> healthCheck() {
        try {
            // Check database connection
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(2)) {
                    return ResponseEntity.ok(Map.of(
                        "status", "UP",
                        "database", "UP",
                        "timestamp", System.currentTimeMillis()
                    ));
                }
            }
            return ResponseEntity.status(503).body(Map.of(
                "status", "DOWN",
                "database", "DOWN",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                "status", "DOWN",
                "database", "DOWN",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @GetMapping("/ready")
    public ResponseEntity<?> readinessCheck() {
        return ResponseEntity.ok(Map.of("status", "READY"));
    }

    @GetMapping("/live")
    public ResponseEntity<?> livenessCheck() {
        return ResponseEntity.ok(Map.of("status", "ALIVE"));
    }
}
