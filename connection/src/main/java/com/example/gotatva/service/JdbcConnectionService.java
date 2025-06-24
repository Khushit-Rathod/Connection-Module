package com.example.gotatva.service;

import com.example.gotatva.model.AutoTester;
import com.example.gotatva.model.JdbcConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class JdbcConnectionService {

    private final Map<String, Connection> activeConnections = new ConcurrentHashMap<>();
    private final Map<String, JdbcConnection> connectionConfigs = new ConcurrentHashMap<>();
    private final Map<String, AutoTester> lastTestResults = new ConcurrentHashMap<>();
    private final Map<String, Timer> scheduledTests = new ConcurrentHashMap<>();

    public String startConnection(JdbcConnection request) {
        try {
            String driverClass = getDriverClass(request.getUrl());
            Class.forName(driverClass);

            Connection connection = DriverManager.getConnection(
                    request.getUrl(),
                    request.getUsername(),
                    request.getPassword());

            activeConnections.put(request.getConnectionName(), connection);
            connectionConfigs.put(request.getConnectionName(), request);

            if (request.getAutoTestInterval() > 0) {
                scheduleAutoTest(request);
            }

            // Perform initial test
            String initialTestResult = testConnection(request);
            lastTestResults.put(request.getConnectionName(),
                    new AutoTester(LocalDateTime.now(), initialTestResult));

            return "Connection started successfully: " + request.getConnectionName() +
                    "\nInitial test result: " + initialTestResult;
        } catch (Exception e) {
            log.error("Failed to start connection", e);
            return "Failed to start connection: " + e.getMessage();
        }
    }

    private void scheduleAutoTest(JdbcConnection config) {
        Timer timer = new Timer();
        long intervalMillis = (long) config.getAutoTestInterval() * 60 * 1000; // Convert minutes to milliseconds

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    String result = testConnection(config);
                    lastTestResults.put(config.getConnectionName(),
                            new AutoTester(LocalDateTime.now(), result));
                    log.info("Auto-test completed for {}: {}", config.getConnectionName(), result);
                } catch (Exception e) {
                    log.error("Auto-test failed for " + config.getConnectionName(), e);
                }
            }
        }, intervalMillis, intervalMillis);

        // Store the timer so we can cancel it later if needed
        scheduledTests.put(config.getConnectionName(), timer);
    }

    public String stopConnection(String connectionName) {
        try {
            Connection connection = activeConnections.get(connectionName);
            if (connection != null && !connection.isClosed()) {
                connection.close();
                activeConnections.remove(connectionName);
                connectionConfigs.remove(connectionName);
                return "Connection stopped successfully: " + connectionName;
            }
            return "Connection not found or already closed: " + connectionName;
        } catch (SQLException e) {
            log.error("Failed to stop connection", e);
            return "Failed to stop connection: " + e.getMessage();
        }
    }

    public List<JdbcConnection> getAllConnections() {
        return new ArrayList<>(connectionConfigs.values());
    }

    public JdbcConnection updateConnection(JdbcConnection request) {
        connectionConfigs.put(request.getConnectionName(), request);
        return request;
    }

    public String testConnection(JdbcConnection jdbcConnection) {
        log.info("Testing connection to: {}", jdbcConnection.getUrl());
        StringBuilder result = new StringBuilder();

        try {
            String driverClass = getDriverClass(jdbcConnection.getUrl());
            Class.forName(driverClass);

            try (Connection connection = DriverManager.getConnection(
                    jdbcConnection.getUrl(),
                    jdbcConnection.getUsername(),
                    jdbcConnection.getPassword())) {

                if (connection != null && !connection.isClosed()) {
                    result.append("Connection successful to ").append(jdbcConnection.getUrl()).append("\n");

                    // Use custom query if provided, otherwise use default test
                    String query = jdbcConnection.getCustomQuery() != null ?
                            jdbcConnection.getCustomQuery() :
                            "SELECT 1 as test_value";

                    result.append(executeCustomQuery(connection, query));
                }
            }
        } catch (ClassNotFoundException e) {
            log.error("JDBC Driver not found", e);
            return "JDBC Driver not found: " + e.getMessage();
        } catch (SQLException e) {
            log.error("Connection failed", e);
            return "Connection failed: " + e.getMessage();
        }
        return result.toString();
    }

    private String executeCustomQuery(Connection connection, String query) throws SQLException {
        StringBuilder queryResult = new StringBuilder();
        queryResult.append("\nQuery Results:\n");

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            queryResult.append(formatResultSet(rs));
        }

        return queryResult.toString();
    }

    private String formatResultSet(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Print headers
        for (int i = 1; i <= columnCount; i++) {
            sb.append(String.format("%-20s", metaData.getColumnName(i)));
        }
        sb.append("\n");

        // Print rows
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                sb.append(String.format("%-20s", rs.getString(i)));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String getDriverClass(String url) {
        if (url.startsWith("jdbc:h2:")) {
            return "org.h2.Driver";
        } else if (url.startsWith("jdbc:oracle:")) {
            return "oracle.jdbc.OracleDriver";
        } else if (url.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        } else if (url.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        }
        throw new IllegalArgumentException("Unsupported database URL: " + url);
    }

    public String getLastTestResult(String connectionName) {
        AutoTester result = lastTestResults.get(connectionName);
        if (result != null) {
            return "Last test at " + result.getTimestamp() + ":\n" + result.getResult();
        }
        return "No test results available for " + connectionName;
    }

    @Scheduled(fixedRate = 60000) // Fallback scheduler runs every minute
    private void autoTestConnections() {
        for (JdbcConnection config : connectionConfigs.values()) {
            if (config.getAutoTestInterval() > 0 && !scheduledTests.containsKey(config.getConnectionName())) {
                scheduleAutoTest(config);
            }
        }
    }
}