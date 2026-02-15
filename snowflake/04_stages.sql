-- ============================================================================
-- STAGE CONFIGURATION FOR DATA LOADING
-- ============================================================================

USE DATABASE MONEY_TRANSFER_DW;
USE SCHEMA MONEY_TRANSFER_DW.ANALYTICS;

-- Create Internal Stage
-- Used for temporary storage of data files before loading
CREATE OR REPLACE STAGE staging_internal
ENCRYPTION = (TYPE = 'SNOWFLAKE_SSE')
DIRECTORY = (ENABLE = TRUE)
COMMENT = 'Internal stage for transaction data loading';

-- Create External Stage (if using S3)
-- Optional: Uncomment if you have AWS S3 configured
/*
CREATE OR REPLACE STAGE staging_s3
URL = 's3://your-bucket/money-transfer-data/'
CREDENTIALS = (
  AWS_KEY_ID = 'YOUR_KEY'
  AWS_SECRET_KEY = 'YOUR_SECRET'
)
FILE_FORMAT = (
  TYPE = 'CSV'
  FIELD_DELIMITER = ','
  SKIP_HEADER = 1
  DATE_FORMAT = 'YYYY-MM-DD'
  TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS'
)
COMMENT = 'External S3 stage for bulk data loading';
*/

-- Note: To test stage creation, run the following interactively in Snowflake:
-- LIST @staging_internal;
-- SHOW STAGES;
