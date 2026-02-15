-- ============================================================================
-- ETL PIPELINE - COPY INTO COMMANDS
-- ============================================================================

USE DATABASE MONEY_TRANSFER_DW;
USE SCHEMA MONEY_TRANSFER_DW.ANALYTICS;

-- ============================================================================
-- ACCOUNT DIMENSION LOAD
-- ============================================================================

-- ETL Step 1: Load Account Dimension Data
-- This script loads accounts from staging into DIM_ACCOUNT
-- Updated CSV format to include BALANCE: account_id, holder_name, account_number, account_type, status, balance, created_date

COPY INTO DIM_ACCOUNT (
    account_id,
    holder_name,
    account_number,
    account_type,
    status,
    balance,
    created_date,
    effective_date
)
FROM (
    SELECT 
        $1::INT,                    -- account_id
        $2::VARCHAR,                -- holder_name
        $3::VARCHAR,                -- account_number
        $4::VARCHAR,                -- account_type
        $5::VARCHAR,                -- status
        $6::NUMBER(18,2),           -- balance
        $7::DATE,                   -- created_date
        $7::DATE                    -- effective_date (use created_date for now)
    FROM @staging_internal/accounts/
)
FILE_FORMAT = (
    TYPE = 'CSV'
    FIELD_DELIMITER = ','
    SKIP_HEADER = 1
    NULL_IF = ('NULL', 'null', '')
    DATE_FORMAT = 'YYYY-MM-DD'
)
ON_ERROR = 'CONTINUE'
FORCE = FALSE;

-- ============================================================================
-- DATE DIMENSION LOAD
-- ============================================================================

-- ETL Step 2: Load Date Dimension (populate for a range)
-- Run this once to populate dates
INSERT INTO DIM_DATE (
    date_key,
    full_date,
    day,
    month,
    year,
    quarter,
    day_name,
    month_name,
    is_weekend,
    week_number,
    fiscal_year
)
WITH DATERANGE AS (
    SELECT 
        TO_DATE('2024-01-01') + ROW_NUMBER() OVER (ORDER BY NULL) - 1 AS date_val
    FROM TABLE(GENERATOR(ROWCOUNT => 730))  -- 2 years of dates
)
SELECT 
    YEAR(date_val) * 10000 + MONTH(date_val) * 100 + DAY(date_val) AS date_key,
    date_val AS full_date,
    DAY(date_val) AS day,
    MONTH(date_val) AS month,
    YEAR(date_val) AS year,
    QUARTER(date_val) AS quarter,
    TO_CHAR(date_val, 'DDDD') AS day_name,                              -- Day name (Monday, etc.)
    TO_CHAR(date_val, 'MMMM') AS month_name,                            -- Month name (January, etc.)
    CASE WHEN DAYOFWEEKISO(date_val) IN (6, 7) THEN TRUE ELSE FALSE END AS is_weekend,  -- ISO week (6=Sat, 7=Sun)
    WEEK(date_val) AS week_number,                                        -- Week number
    CASE WHEN MONTH(date_val) >= 4 THEN YEAR(date_val) ELSE YEAR(date_val) - 1 END AS fiscal_year
FROM DATERANGE
WHERE NOT EXISTS (SELECT 1 FROM DIM_DATE WHERE full_date = date_val);

-- ============================================================================
-- TRANSACTION FACT LOAD
-- ============================================================================

-- ============================================================================
-- TRANSACTION FACT LOAD
-- ============================================================================

-- ETL Step 3: Create temporary staging table for raw transaction data
CREATE TEMPORARY TABLE FACT_TRANSACTIONS_STAGING (
    transaction_id BIGINT,
    account_from_id INT,
    account_to_id INT,
    transaction_date DATE,
    amount DECIMAL(19,4),
    currency VARCHAR(3),
    status VARCHAR(50),
    transaction_type VARCHAR(50),
    description VARCHAR(500),
    reference_number VARCHAR(100),
    failure_reason VARCHAR(255)
);

-- Load raw transaction data into staging table (simple COPY)
COPY INTO FACT_TRANSACTIONS_STAGING (
    transaction_id,
    account_from_id,
    account_to_id,
    transaction_date,
    amount,
    currency,
    status,
    transaction_type,
    description,
    reference_number,
    failure_reason
)
FROM (
    SELECT 
        $1::BIGINT,                 -- transaction_id
        $2::INT,                    -- account_from_id
        $3::INT,                    -- account_to_id
        $4::DATE,                   -- transaction_date
        $5::DECIMAL(19,4),          -- amount
        $6::VARCHAR,                -- currency
        $7::VARCHAR,                -- status
        $8::VARCHAR,                -- transaction_type
        $9::VARCHAR,                -- description
        $10::VARCHAR,               -- reference_number
        $11::VARCHAR                -- failure_reason
    FROM @staging_internal/transactions/
)
FILE_FORMAT = (
    TYPE = 'CSV'
    FIELD_DELIMITER = ','
    SKIP_HEADER = 1
    NULL_IF = ('NULL', 'null', '')
    DATE_FORMAT = 'YYYY-MM-DD'
    TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS'
)
ON_ERROR = 'CONTINUE'
FORCE = FALSE;

-- ETL Step 3b: Transform and load from staging into FACT_TRANSACTIONS
INSERT INTO FACT_TRANSACTIONS (
    transaction_id,
    account_from_key,
    account_to_key,
    date_key,
    amount,
    currency,
    status,
    transaction_type,
    description,
    reference_number,
    failure_reason
)
SELECT 
    s.transaction_id,
    df.account_key AS account_from_key,
    dt.account_key AS account_to_key,
    (YEAR(s.transaction_date) * 10000 + MONTH(s.transaction_date) * 100 + DAY(s.transaction_date))::INT AS date_key,
    s.amount,
    s.currency,
    s.status,
    s.transaction_type,
    s.description,
    s.reference_number,
    s.failure_reason
FROM FACT_TRANSACTIONS_STAGING s
LEFT JOIN DIM_ACCOUNT df ON df.account_id = s.account_from_id
LEFT JOIN DIM_ACCOUNT dt ON dt.account_id = s.account_to_id;

-- Clean up staging table
DROP TABLE IF EXISTS FACT_TRANSACTIONS_STAGING;

-- ============================================================================
-- VERIFICATION QUERIES (Run these interactively after ETL completes)
-- ============================================================================

-- Check loaded record counts
-- SELECT 'DIM_ACCOUNT' AS table_name, COUNT(*) AS record_count FROM DIM_ACCOUNT
-- UNION ALL
-- SELECT 'DIM_DATE', COUNT(*) FROM DIM_DATE
-- UNION ALL
-- SELECT 'FACT_TRANSACTIONS', COUNT(*) FROM FACT_TRANSACTIONS;

-- Show most recent load dates
-- SELECT 
--     'DIM_ACCOUNT' AS table_name,
--     MAX(load_date) AS last_load_date
-- FROM DIM_ACCOUNT
-- UNION ALL
-- SELECT 
--     'FACT_TRANSACTIONS',
--     MAX(load_date)
-- FROM FACT_TRANSACTIONS;

-- Sample transaction data
-- SELECT * FROM FACT_TRANSACTIONS LIMIT 10;
