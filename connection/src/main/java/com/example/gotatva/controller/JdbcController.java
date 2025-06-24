package com.example.gotatva.controller;

import com.example.gotatva.model.JdbcConnection;
import com.example.gotatva.service.JdbcConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/jdbc")
@RequiredArgsConstructor
public class JdbcController {

    private final JdbcConnectionService jdbcConnectionService;

    @PostMapping("/start")
    public ResponseEntity<String> startConnection(@RequestBody JdbcConnection request) {
        String msg = jdbcConnectionService.startConnection(request);
        return ResponseEntity.ok(msg);
    }

    @PostMapping("/stop/{connectionName}")
    public ResponseEntity<String> stopConnection(@PathVariable String connectionName) {
        String message = jdbcConnectionService.stopConnection(connectionName);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/connections")
    public ResponseEntity<List<JdbcConnection>> getAllConnections() {
        List<JdbcConnection> connections = jdbcConnectionService.getAllConnections();
        return ResponseEntity.ok(connections);
    }

    @PatchMapping("/update")
    public ResponseEntity<JdbcConnection> updateDetails(@RequestBody JdbcConnection request) {
        return ResponseEntity.ok(jdbcConnectionService.updateConnection(request));
    }

    @PostMapping("/test-connection")
    public ResponseEntity<String> testJdbcConnection(@RequestBody JdbcConnection jdbcConnection) {
        log.info("Testing JDBC connection: {}", jdbcConnection.getUrl());
        String result = jdbcConnectionService.testConnection(jdbcConnection);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/test-results/{connectionName}")
    public ResponseEntity<String> getTestResults(@PathVariable String connectionName) {
        String result = jdbcConnectionService.getLastTestResult(connectionName);
        return ResponseEntity.ok(result);
    }
}