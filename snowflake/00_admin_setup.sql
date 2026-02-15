-- ============================================================================
-- SNOWFLAKE ADMIN SETUP - RUN AS ACCOUNTADMIN
-- Money Transfer System - Pre-requisite Setup
-- ============================================================================
-- 
-- IMPORTANT: This script MUST be run by ACCOUNTADMIN before any other scripts
-- It creates the database, schema, warehouse, and grants privileges to the application user
--
-- Usage:
--   1. Login to Snowflake Web Console as ACCOUNTADMIN
--   2. Copy and paste this entire script
--   3. Execute all statements
--   4. Verify objects created
-- ============================================================================

-- Switch to ACCOUNTADMIN role
USE ROLE ACCOUNTADMIN;

-- ============================================================================
-- Step 1: Create Database
-- ============================================================================
CREATE DATABASE IF NOT EXISTS MONEY_TRANSFER_DW
COMMENT = 'Data warehouse for Money Transfer System analytics';

-- ============================================================================
-- Step 2: Create Schema
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS MONEY_TRANSFER_DW.ANALYTICS
COMMENT = 'Analytics schema containing dimensional and fact tables';

-- ============================================================================
-- Step 3: Create Warehouse
-- ============================================================================
CREATE WAREHOUSE IF NOT EXISTS COMPUTE_WH
WAREHOUSE_SIZE = 'XSMALL'
AUTO_SUSPEND = 5
AUTO_RESUME = TRUE
COMMENT = 'Compute warehouse for analytics queries';

-- ============================================================================
-- Step 4: Create or verify app user (ajay006)
-- ============================================================================
-- Note: If user already exists, this will be ignored
CREATE USER IF NOT EXISTS ajay006
PASSWORD = 'Sharmaxbud0604'
COMMENT = 'Application user for Money Transfer System analytics'
DEFAULT_WAREHOUSE = COMPUTE_WH
DEFAULT_ROLE = SYSADMIN;

-- ============================================================================
-- Step 5: Grant Privileges to Application User
-- ============================================================================

-- Grant role
GRANT ROLE SYSADMIN TO USER ajay006;

-- Grant database usage
GRANT USAGE ON DATABASE MONEY_TRANSFER_DW TO ROLE SYSADMIN;

-- Grant schema privileges
GRANT USAGE, CREATE TABLE, CREATE STAGE ON SCHEMA MONEY_TRANSFER_DW.ANALYTICS TO ROLE SYSADMIN;

-- Grant warehouse usage
GRANT USAGE, OPERATE ON WAREHOUSE COMPUTE_WH TO ROLE SYSADMIN;

-- ============================================================================
-- Step 6: Verify Setup
-- ============================================================================
-- Verify objects created
SELECT CURRENT_DATABASE(), CURRENT_SCHEMA(), CURRENT_WAREHOUSE();

-- List all created objects
SHOW DATABASES LIKE 'MONEY_TRANSFER_DW';
SHOW SCHEMAS IN DATABASE MONEY_TRANSFER_DW;
SHOW WAREHOUSES LIKE 'COMPUTE_WH';
SHOW USERS LIKE 'ajay006';

-- ============================================================================
-- SUCCESS: Admin setup complete!
-- ============================================================================
-- Next steps:
-- 1. Run 02_dimensions.sql to create dimension tables
-- 2. Run 03_facts.sql to create fact tables
-- 3. Run 04_stages.sql to create data load stages
-- 4. Run 05_etl_pipeline.sql to populate dimensions and facts
-- 5. Backend can then call POST /api/snowflake/init to initialize tables
-- ============================================================================
