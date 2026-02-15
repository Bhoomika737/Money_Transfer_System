-- ============================================================================
-- SNOWFLAKE DATA WAREHOUSE SETUP
-- Money Transfer System - Analytics Database
-- ============================================================================
-- NOTE: This assumes database and schema are already created by 00_admin_setup.sql
-- If not, run 00_admin_setup.sql first as ACCOUNTADMIN user
-- ============================================================================

-- Switch to SYSADMIN for object creation
USE ROLE SYSADMIN;

-- Step 1: Use existing Database  
USE DATABASE MONEY_TRANSFER_DW;
COMMENT = 'Data warehouse for Money Transfer System';

-- Step 2: Use existing Schema
USE SCHEMA MONEY_TRANSFER_DW.ANALYTICS;
COMMENT = 'Analytics and reporting schema';

-- Step 3: Create or Replace Warehouse
CREATE WAREHOUSE IF NOT EXISTS COMPUTE_WH
WAREHOUSE_SIZE = 'XSMALL'
AUTO_SUSPEND = 5
AUTO_RESUME = TRUE
COMMENT = 'Compute warehouse for analytics queries';

-- Step 4: Set default warehouse context
ALTER WAREHOUSE COMPUTE_WH SET AUTO_SUSPEND = 5, AUTO_RESUME = TRUE;

-- Verify setup
SELECT CURRENT_DATABASE(), CURRENT_SCHEMA(), CURRENT_WAREHOUSE();

-- ============================================================================
-- Setup Complete! Database and schema are ready for table creation.
-- ============================================================================

