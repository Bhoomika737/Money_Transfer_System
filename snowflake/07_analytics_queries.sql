-- ============================================================================
-- SNOWFLAKE ANALYTICS QUERIES
-- Money Transfer System - Business Intelligence
-- ============================================================================
-- Run these queries in Snowflake Web Console or connect BI tool
-- Located in: MONEY_TRANSFER_DW.ANALYTICS schema
-- ============================================================================

-- ============================================================================
-- QUERY 1: DAILY TRANSACTION VOLUME ANALYSIS
-- ============================================================================
-- Shows daily transaction metrics for trend analysis
SELECT 
    dd.FULL_DATE AS transaction_date,
    dd.DAY_NAME,
    COUNT(ft.TRANSACTION_KEY) AS transaction_count,
    SUM(ft.AMOUNT) AS total_amount,
    AVG(ft.AMOUNT) AS avg_amount,
    MIN(ft.AMOUNT) AS min_amount,
    MAX(ft.AMOUNT) AS max_amount,
    COUNT(DISTINCT ft.ACCOUNT_FROM_KEY) AS unique_senders,
    COUNT(DISTINCT ft.ACCOUNT_TO_KEY) AS unique_receivers
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_DATE dd ON ft.DATE_KEY = dd.DATE_KEY
GROUP BY dd.FULL_DATE, dd.DAY_NAME, ft.DATE_KEY
ORDER BY dd.FULL_DATE DESC
LIMIT 90;


-- ============================================================================
-- QUERY 2: TOP 20 MOST ACTIVE ACCOUNTS
-- ============================================================================
-- Identifies VIP accounts by transaction activity
SELECT 
    da.ACCOUNT_ID,
    da.HOLDER_NAME,
    da.STATUS,
    COUNT(ft.TRANSACTION_KEY) AS total_transactions,
    SUM(ft.AMOUNT) AS total_volume,
    AVG(ft.AMOUNT) AS avg_transaction,
    MIN(ft.AMOUNT) AS min_transaction,
    MAX(ft.AMOUNT) AS max_transaction,
    MAX(ft.CREATED_ON) AS last_transaction_date,
    da.BALANCE AS current_balance
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_ACCOUNT da ON ft.ACCOUNT_FROM_KEY = da.ACCOUNT_KEY
GROUP BY da.ACCOUNT_ID, da.HOLDER_NAME, da.STATUS, da.BALANCE
ORDER BY total_transactions DESC
LIMIT 20;


-- ============================================================================
-- QUERY 3: TRANSACTION SUCCESS RATE ANALYSIS
-- ============================================================================
-- Analyzes success/failure rates and volumes
SELECT 
    ft.STATUS,
    COUNT(ft.TRANSACTION_KEY) AS transaction_count,
    SUM(ft.AMOUNT) AS total_amount,
    AVG(ft.AMOUNT) AS avg_amount,
    ROUND(
        100.0 * COUNT(ft.TRANSACTION_KEY) / SUM(COUNT(ft.TRANSACTION_KEY)) OVER(), 
        2
    ) AS success_percentage
FROM FACT_TRANSACTIONS ft
GROUP BY ft.STATUS
ORDER BY transaction_count DESC;


-- ============================================================================
-- QUERY 4: PEAK HOURS / HIGH ACTIVITY PERIODS
-- ============================================================================
-- Identifies busiest periods for capacity planning
SELECT 
    dd.FULL_DATE AS period_date,
    dd.DAY_NAME,
    COUNT(ft.TRANSACTION_KEY) AS transaction_count,
    SUM(ft.AMOUNT) AS total_amount,
    AVG(ft.AMOUNT) AS avg_amount,
    COUNT(DISTINCT ft.ACCOUNT_FROM_KEY) AS total_senders,
    CASE 
        WHEN COUNT(ft.TRANSACTION_KEY) > 100 THEN 'PEAK HOUR'
        WHEN COUNT(ft.TRANSACTION_KEY) > 50 THEN 'HIGH ACTIVITY'
        ELSE 'NORMAL'
    END AS activity_level
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_DATE dd ON ft.DATE_KEY = dd.DATE_KEY
GROUP BY dd.FULL_DATE, dd.DAY_NAME
ORDER BY transaction_count DESC
LIMIT 50;


-- ============================================================================
-- QUERY 5: AVERAGE TRANSFER AMOUNT STATISTICS
-- ============================================================================
-- Statistical analysis of transaction amounts
SELECT 
    ft.TRANSACTION_TYPE,
    ft.CURRENCY,
    COUNT(ft.TRANSACTION_KEY) AS transaction_count,
    ROUND(AVG(ft.AMOUNT), 2) AS avg_amount,
    ROUND(MEDIAN(ft.AMOUNT), 2) AS median_amount,
    ROUND(MIN(ft.AMOUNT), 2) AS min_amount,
    ROUND(MAX(ft.AMOUNT), 2) AS max_amount,
    ROUND(STDDEV(ft.AMOUNT), 2) AS std_dev_amount,
    SUM(ft.AMOUNT) AS total_amount
FROM FACT_TRANSACTIONS ft
GROUP BY ft.TRANSACTION_TYPE, ft.CURRENCY
ORDER BY total_amount DESC;


-- ============================================================================
-- QUERY 6: HOURLY TRANSACTION VOLUME
-- ============================================================================
-- Shows transaction distribution by hour of day (using transaction time)
SELECT 
    HOUR(ft.CREATED_ON) AS hour_of_day,
    COUNT(ft.TRANSACTION_KEY) AS transaction_count,
    SUM(ft.AMOUNT) AS total_amount,
    AVG(ft.AMOUNT) AS avg_amount,
    COUNT(DISTINCT ft.ACCOUNT_FROM_KEY) AS unique_senders
FROM FACT_TRANSACTIONS ft
GROUP BY HOUR(ft.CREATED_ON)
ORDER BY hour_of_day ASC;


-- ============================================================================
-- QUERY 7: SENDER vs RECEIVER ANALYSIS
-- ============================================================================
-- Analyzes who sends money to whom
SELECT 
    da_from.ACCOUNT_ID AS sender_account,
    da_from.HOLDER_NAME AS sender_name,
    da_to.ACCOUNT_ID AS receiver_account,
    da_to.HOLDER_NAME AS receiver_name,
    COUNT(ft.TRANSACTION_KEY) AS transaction_count,
    SUM(ft.AMOUNT) AS total_volume,
    AVG(ft.AMOUNT) AS avg_amount
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_ACCOUNT da_from ON ft.ACCOUNT_FROM_KEY = da_from.ACCOUNT_KEY
LEFT JOIN DIM_ACCOUNT da_to ON ft.ACCOUNT_TO_KEY = da_to.ACCOUNT_KEY
GROUP BY da_from.ACCOUNT_ID, da_from.HOLDER_NAME, da_to.ACCOUNT_ID, da_to.HOLDER_NAME
ORDER BY total_volume DESC
LIMIT 50;


-- ============================================================================
-- QUERY 8: WEEKLY TRANSACTION SUMMARY
-- ============================================================================
-- Aggregates transactions by week
SELECT 
    YEAR(dd.FULL_DATE) AS year,
    WEEK(dd.FULL_DATE) AS week_number,
    MIN(dd.FULL_DATE) AS week_start,
    MAX(dd.FULL_DATE) AS week_end,
    COUNT(ft.TRANSACTION_KEY) AS transaction_count,
    SUM(ft.AMOUNT) AS weekly_volume,
    AVG(ft.AMOUNT) AS avg_amount,
    COUNT(DISTINCT ft.ACCOUNT_FROM_KEY) AS unique_senders
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_DATE dd ON ft.DATE_KEY = dd.DATE_KEY
GROUP BY YEAR(dd.FULL_DATE), WEEK(dd.FULL_DATE)
ORDER BY year DESC, week_number DESC;


-- ============================================================================
-- QUERY 9: ACCOUNT BALANCE ANALYSIS
-- ============================================================================
-- Shows account balances and activity
SELECT 
    da.ACCOUNT_ID,
    da.HOLDER_NAME,
    da.STATUS,
    da.BALANCE,
    COUNT(ft.TRANSACTION_KEY) AS total_transactions,
    SUM(ft.AMOUNT) AS lifetime_volume,
    MAX(ft.CREATED_ON) AS last_transaction
FROM DIM_ACCOUNT da
LEFT JOIN FACT_TRANSACTIONS ft ON da.ACCOUNT_KEY = ft.ACCOUNT_FROM_KEY OR da.ACCOUNT_KEY = ft.ACCOUNT_TO_KEY
GROUP BY da.ACCOUNT_ID, da.HOLDER_NAME, da.STATUS, da.BALANCE
ORDER BY da.BALANCE DESC;


-- ============================================================================
-- QUERY 10: ANOMALY DETECTION - LARGE TRANSACTIONS
-- ============================================================================
-- Identifies unusually large transactions (potential fraud)
WITH transaction_stats AS (
    SELECT 
        AVG(AMOUNT) AS avg_amount,
        STDDEV(AMOUNT) AS std_dev
    FROM FACT_TRANSACTIONS
)
SELECT 
    ft.CREATED_ON,
    da_from.ACCOUNT_ID AS sender,
    da_to.ACCOUNT_ID AS receiver,
    ft.AMOUNT,
    ROUND((ft.AMOUNT - ts.avg_amount) / ts.std_dev, 2) AS std_deviations_from_mean,
    'ANOMALY' AS alert
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_ACCOUNT da_from ON ft.ACCOUNT_FROM_KEY = da_from.ACCOUNT_KEY
LEFT JOIN DIM_ACCOUNT da_to ON ft.ACCOUNT_TO_KEY = da_to.ACCOUNT_KEY
CROSS JOIN transaction_stats ts
  WHERE ft.AMOUNT > (ts.avg_amount + (3 * ts.std_dev))
ORDER BY ft.AMOUNT DESC;


-- ============================================================================
-- QUERY 11: MONTHLY REVENUE REPORT
-- ============================================================================
-- Monthly business metrics
SELECT 
    YEAR(dd.FULL_DATE) AS year,
    MONTH(dd.FULL_DATE) AS month,
    TO_CHAR(dd.FULL_DATE, 'YYYY-MM') AS month_year,
    COUNT(ft.TRANSACTION_KEY) AS transaction_count,
    SUM(ft.AMOUNT) AS monthly_volume,
    AVG(ft.AMOUNT) AS avg_transaction,
    COUNT(DISTINCT ft.ACCOUNT_FROM_KEY) AS active_senders,
    COUNT(DISTINCT ft.ACCOUNT_TO_KEY) AS active_receivers
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_DATE dd ON ft.DATE_KEY = dd.DATE_KEY
GROUP BY YEAR(dd.FULL_DATE), MONTH(dd.FULL_DATE), TO_CHAR(dd.FULL_DATE, 'YYYY-MM')
ORDER BY year DESC, month DESC;


-- ============================================================================
-- QUERY 12: SEGMENT ANALYSIS - ACCOUNT TIERS
-- ============================================================================
-- Categorizes accounts by transaction activity
WITH account_metrics AS (
    SELECT 
        da.ACCOUNT_ID,
        da.HOLDER_NAME,
        da.STATUS,
        COUNT(ft.TRANSACTION_KEY) AS tx_count,
        SUM(ft.AMOUNT) AS total_volume
    FROM DIM_ACCOUNT da
    LEFT JOIN FACT_TRANSACTIONS ft ON da.ACCOUNT_KEY = ft.ACCOUNT_FROM_KEY
    GROUP BY da.ACCOUNT_ID, da.HOLDER_NAME, da.STATUS
)
SELECT 
    CASE 
        WHEN tx_count > 100 THEN 'PLATINUM'
        WHEN tx_count > 50 THEN 'GOLD'
        WHEN tx_count > 10 THEN 'SILVER'
        ELSE 'BRONZE'
    END AS tier,
    COUNT(*) AS account_count,
    SUM(tx_count) AS total_transactions,
    SUM(total_volume) AS total_volume,
    AVG(total_volume) AS avg_account_volume
FROM account_metrics
GROUP BY tier
ORDER BY account_count DESC;


-- ============================================================================
-- QUERY 13: DATA WAREHOUSE STATISTICS
-- ============================================================================
-- Shows data warehouse size and record counts
SELECT 
    'DIM_ACCOUNT' AS table_name,
    COUNT(*) AS record_count,
    CURRENT_TIMESTAMP AS last_updated
FROM DIM_ACCOUNT
UNION ALL
SELECT 
    'DIM_DATE' AS table_name,
    COUNT(*) AS record_count,
    CURRENT_TIMESTAMP AS last_updated
FROM DIM_DATE
UNION ALL
SELECT 
    'FACT_TRANSACTIONS' AS table_name,
    COUNT(*) AS record_count,
    CURRENT_TIMESTAMP AS last_updated
FROM FACT_TRANSACTIONS;


-- ============================================================================
-- QUERY 14: GROWTH METRICS - DAY OVER DAY
-- ============================================================================
-- Tracks growth metrics
SELECT 
    dd.FULL_DATE,
    COUNT(ft.TRANSACTION_KEY) AS daily_transactions,
    SUM(ft.AMOUNT) AS daily_volume,
    LAG(COUNT(ft.TRANSACTION_KEY)) OVER (ORDER BY dd.FULL_DATE) AS prev_day_transactions,
    LAG(SUM(ft.AMOUNT)) OVER (ORDER BY dd.FULL_DATE) AS prev_day_volume,
    ROUND(
        (COUNT(ft.TRANSACTION_KEY) - LAG(COUNT(ft.TRANSACTION_KEY)) OVER (ORDER BY dd.FULL_DATE)) * 100.0 /
        LAG(COUNT(ft.TRANSACTION_KEY)) OVER (ORDER BY dd.FULL_DATE),
        2
    ) AS transaction_growth_percent
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_DATE dd ON ft.DATE_KEY = dd.DATE_KEY
GROUP BY dd.FULL_DATE
ORDER BY dd.FULL_DATE DESC
LIMIT 60;


-- ============================================================================
-- QUERY 15: CUMULATIVE TRANSACTION VOLUME
-- ============================================================================
-- Shows cumulative transaction trends
SELECT 
    dd.FULL_DATE,
    COUNT(ft.TRANSACTION_KEY) AS daily_count,
    SUM(COUNT(ft.TRANSACTION_KEY)) OVER (ORDER BY dd.FULL_DATE) AS cumulative_transactions,
    SUM(ft.AMOUNT) AS daily_amount,
    SUM(SUM(ft.AMOUNT)) OVER (ORDER BY dd.FULL_DATE) AS cumulative_volume
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_DATE dd ON ft.DATE_KEY = dd.DATE_KEY
GROUP BY dd.FULL_DATE
ORDER BY dd.FULL_DATE ASC;
