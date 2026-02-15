-- ============================================================================
-- DIMENSION TABLES - MONEY TRANSFER SYSTEM
-- ============================================================================

USE DATABASE MONEY_TRANSFER_DW;
USE SCHEMA MONEY_TRANSFER_DW.ANALYTICS;

-- Dimension 1: Account Dimension Table
-- Contains account attributes for dimensional analysis
CREATE TABLE IF NOT EXISTS DIM_ACCOUNT (
    account_key NUMBER AUTOINCREMENT PRIMARY KEY,
    account_id STRING NOT NULL UNIQUE,
    holder_name VARCHAR(255) NOT NULL,
    account_number VARCHAR(50),
    account_type VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    balance NUMBER(19,4),
    created_date DATE,
    effective_date DATE,
    end_date DATE,
    is_current BOOLEAN DEFAULT TRUE,
    load_date TIMESTAMP_NTZ DEFAULT CURRENT_TIMESTAMP()
);

-- Dimension 2: Date Dimension Table
-- Contains date attributes for time-based analysis
CREATE TABLE IF NOT EXISTS DIM_DATE (
    date_key INT PRIMARY KEY,
    full_date DATE NOT NULL UNIQUE,
    day INT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    quarter INT NOT NULL,
    day_name VARCHAR(20) NOT NULL,
    month_name VARCHAR(20) NOT NULL,
    is_weekend BOOLEAN NOT NULL,
    is_holiday BOOLEAN DEFAULT FALSE,
    week_number INT NOT NULL,
    fiscal_year INT
);

-- Indexes on dimension tables for performance
-- NOTE: Snowflake does not support traditional B-tree indexes.
-- Removed CREATE INDEX statements. To improve performance consider
-- using clustering keys or relying on Snowflake micro-partitioning.
-- Example (optional):
-- ALTER TABLE DIM_DATE CLUSTER BY (year, month);
-- ALTER TABLE DIM_ACCOUNT CLUSTER BY (account_id);

-- Verify dimension tables (run interactively in Snowflake console if needed)
-- DESCRIBE TABLE DIM_ACCOUNT;
-- DESCRIBE TABLE DIM_DATE;
