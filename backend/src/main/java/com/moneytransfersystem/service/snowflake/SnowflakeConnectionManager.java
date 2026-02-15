package com.moneytransfersystem.service.snowflake;

import com.moneytransfersystem.config.SnowflakeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Manages Snowflake Database Connections
 * Handles connection establishment and resource management
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnowflakeConnectionManager {

    private final SnowflakeProperties properties;

    /**
     * Gets a connection to Snowflake Data Warehouse
     */
    public Connection getConnection() throws SQLException {

        if (!properties.isEnabled()) {
            throw new RuntimeException("Snowflake is not enabled. Check application.yml");
        }

        String url = buildConnectionUrl();
        log.info("Connecting to Snowflake warehouse: {}", properties.getWarehouse());

        // ✅ Correct way: Put Properties inside method
        Properties props = new Properties();
        props.put("user", properties.getUser());
        props.put("password", properties.getPassword());
        props.put("db", properties.getDatabase());
        props.put("schema", properties.getSchema());
        props.put("warehouse", properties.getWarehouse());
        props.put("role", properties.getRole());

        // ✅ Fix Arrow error by disabling Arrow format
        props.put("JDBC_QUERY_RESULT_FORMAT", "JSON");

        return DriverManager.getConnection(url, props);
    }

    /**
     * Builds Snowflake JDBC URL
     */
    private String buildConnectionUrl() {
        return String.format(
                "jdbc:snowflake://%s.snowflakecomputing.com",
                properties.getAccount()
        );
    }

    /**
     * Tests connection to Snowflake
     */
    public boolean testConnection() {
        try (Connection connection = getConnection()) {

            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");

            boolean connected = rs.next();
            log.info("Snowflake connection test: {}", connected ? "SUCCESS" : "FAILED");

            return connected;

        } catch (SQLException e) {
            log.error("Snowflake connection failed", e);
            return false;
        }
    }

    /**
     * Closes connection safely
     */
    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                log.debug("Connection closed");
            } catch (SQLException e) {
                log.error("Error closing connection", e);
            }
        }
    }
}
