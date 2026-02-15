package com.moneytransfersystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Snowflake Data Warehouse Configuration
 * Binds properties from application.yml with prefix "snowflake"
 */
@Configuration
@ConfigurationProperties(prefix = "snowflake")
@Data
public class SnowflakeProperties {
    private boolean enabled;
    private String account;
    private String user;
    private String password;
    private String database;
    private String schema;
    private String warehouse;
    private String role;
}
