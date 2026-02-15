package com.moneytransfersystem.controllers;

import com.moneytransfersystem.service.snowflake.SnowflakeConnectionManager;
import com.moneytransfersystem.service.snowflake.SnowflakeETLService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/snowflake")
@RequiredArgsConstructor
public class SnowflakeController {

    private final SnowflakeConnectionManager connectionManager;
    private final SnowflakeETLService etlService;

    /**
     * ✅ Health Check Endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {

        Map<String, Object> response = new HashMap<>();

        try {
            boolean connected = connectionManager.testConnection();

            response.put("status", connected ? "healthy" : "unhealthy");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * ✅ Init Schema Endpoint
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initSchema() {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("🚀 Snowflake Init Requested...");

            // Step 1: Check connection
            if (!connectionManager.testConnection()) {
                response.put("status", "error");
                response.put("message", "Snowflake connection failed. Cannot initialize schema.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Step 2: Initialize schema
            etlService.initializeSchema();

            response.put("status", "success");
            response.put("message", "✅ Snowflake Schema Initialized Successfully!");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {

            log.error("❌ Snowflake Init Failed!", e);
            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * ✅ Load Data to Snowflake Endpoint
     * Loads accounts from MySQL and all transaction data
     */
    @PostMapping("/load-data")
    public ResponseEntity<Map<String, Object>> loadData() {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("📊 Data Loading Requested...");

            // Step 1: Check connection
            if (!connectionManager.testConnection()) {
                response.put("status", "error");
                response.put("message", "Snowflake connection failed. Cannot load data.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Step 2: Load all data
            etlService.loadDataToSnowflake();

            response.put("status", "success");
            response.put("message", "✅ Data Loaded to Snowflake Successfully!");
            response.put("details", "Accounts and transactions loaded from MySQL to Snowflake");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            log.error("❌ Data Loading Failed!", e);
            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }
}