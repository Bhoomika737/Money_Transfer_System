-- ============================================================================
-- ANALYTICS QUERIES - BUSINESS INTELLIGENCE
-- ============================================================================

USE DATABASE MONEY_TRANSFER_DW;
USE SCHEMA MONEY_TRANSFER_DW.ANALYTICS;

-- ============================================================================
-- QUERY 1: DAILY TRANSACTION VOLUME
-- Purpose: Count and sum of transactions by date
-- ============================================================================

SELECT 
    dd.full_date AS transaction_date,
    dd.day_name,
    COUNT(ft.transaction_key) AS transaction_count,
    SUM(ft.amount) AS total_amount,
    AVG(ft.amount) AS avg_amount,
    COUNT(DISTINCT ft.account_from_key) AS unique_senders,
    COUNT(DISTINCT ft.account_to_key) AS unique_receivers
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_DATE dd ON ft.date_key = dd.date_key
WHERE ft.status = 'SUCCESS'
GROUP BY dd.full_date, dd.day_name, ft.date_key
ORDER BY dd.full_date DESC
LIMIT 30;

-- ============================================================================
-- QUERY 2: ACCOUNT ACTIVITY (MOST ACTIVE ACCOUNTS)
-- Purpose: Identify most active accounts by transaction volume
-- ============================================================================

SELECT 
    da_from.account_id,
    da_from.holder_name,
    da_from.account_type,
    COUNT(ft.transaction_key) AS total_transactions,
    SUM(ft.amount) AS total_amount_sent,
    AVG(ft.amount) AS avg_amount_sent,
    MIN(ft.amount) AS min_amount,
    MAX(ft.amount) AS max_amount,
    MAX(dd.full_date) AS most_recent_transaction
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_ACCOUNT da_from ON ft.account_from_key = da_from.account_key
LEFT JOIN DIM_DATE dd ON ft.date_key = dd.date_key
WHERE ft.status = 'SUCCESS'
GROUP BY da_from.account_id, da_from.holder_name, da_from.account_type
ORDER BY total_transactions DESC
LIMIT 20;

-- ============================================================================
-- QUERY 3: SUCCESS RATE (PERCENTAGE OF SUCCESSFUL TRANSFERS)
-- Purpose: Measure transfer success rate
-- ============================================================================

SELECT 
    ft.status,
    COUNT(ft.transaction_key) AS transaction_count,
    SUM(ft.amount) AS total_amount,
    ROUND(
        100.0 * COUNT(ft.transaction_key) / SUM(COUNT(ft.transaction_key)) OVER(), 
        2
    ) AS success_percentage
FROM FACT_TRANSACTIONS ft
GROUP BY ft.status
ORDER BY transaction_count DESC;

-- Alternative: Success Rate by Date
SELECT 
    dd.full_date,
    SUM(CASE WHEN ft.status = 'SUCCESS' THEN 1 ELSE 0 END) AS successful_txns,
    COUNT(ft.transaction_key) AS total_txns,
    ROUND(
        100.0 * SUM(CASE WHEN ft.status = 'SUCCESS' THEN 1 ELSE 0 END) / COUNT(ft.transaction_key),
        2
    ) AS success_rate_percentage
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_DATE dd ON ft.date_key = dd.date_key
GROUP BY dd.full_date
ORDER BY dd.full_date DESC;

-- ============================================================================
-- QUERY 4: PEAK HOURS (BUSIEST TRANSACTION TIMES)
-- Purpose: Identify peak transaction hours
-- ============================================================================

SELECT 
    dd.full_date AS transaction_date,
    COUNT(ft.transaction_key) AS transaction_count,
    SUM(ft.amount) AS total_amount,
    AVG(ft.amount) AS avg_amount,
    CASE 
        WHEN COUNT(ft.transaction_key) > 100 THEN 'PEAK HOUR'
        WHEN COUNT(ft.transaction_key) > 50 THEN 'HIGH ACTIVITY'
        ELSE 'NORMAL'
    END AS activity_level
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_DATE dd ON ft.date_key = dd.date_key
WHERE ft.status = 'SUCCESS'
GROUP BY dd.full_date
ORDER BY transaction_count DESC
LIMIT 20;

-- ============================================================================
-- QUERY 5: AVERAGE TRANSFER AMOUNT
-- Purpose: Analyze transfer amounts and statistics
-- ============================================================================

SELECT 
    ft.transaction_type,
    ft.currency,
    COUNT(ft.transaction_key) AS transaction_count,
    ROUND(AVG(ft.amount), 2) AS avg_amount,
    ROUND(MEDIAN(ft.amount), 2) AS median_amount,
    MIN(ft.amount) AS min_amount,
    MAX(ft.amount) AS max_amount,
    ROUND(STDDEV(ft.amount), 2) AS std_dev_amount,
    SUM(ft.amount) AS total_amount
FROM FACT_TRANSACTIONS ft
WHERE ft.status = 'SUCCESS'
GROUP BY ft.transaction_type, ft.currency
ORDER BY total_amount DESC;

-- ============================================================================
-- BONUS QUERY 6: TRANSACTION VOLUME BY ACCOUNT TYPE
-- Purpose: Analyze transaction patterns by account type
-- ============================================================================

SELECT 
    da.account_type,
    COUNT(ft.transaction_key) AS total_transactions,
    SUM(ft.amount) AS total_amount,
    ROUND(AVG(ft.amount), 2) AS avg_amount,
    COUNT(DISTINCT da.account_id) AS unique_accounts
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_ACCOUNT da ON ft.account_from_key = da.account_key
WHERE ft.status = 'SUCCESS'
GROUP BY da.account_type
ORDER BY total_amount DESC;

-- ============================================================================
-- BONUS QUERY 7: MONTHLY TRANSACTION TREND
-- Purpose: Track transaction trends over months
-- ============================================================================

SELECT 
    dd.year,
    dd.month,
    dd.month_name,
    COUNT(ft.transaction_key) AS transaction_count,
    SUM(ft.amount) AS total_amount,
    ROUND(AVG(ft.amount), 2) AS avg_amount,
    COUNT(DISTINCT ft.account_from_key) AS unique_senders
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_DATE dd ON ft.date_key = dd.date_key
WHERE ft.status = 'SUCCESS'
GROUP BY dd.year, dd.month, dd.month_name
ORDER BY dd.year DESC, dd.month DESC;

-- ============================================================================
-- BONUS QUERY 8: ACCOUNT PAIR ANALYSIS
-- Purpose: Identify frequently transacting account pairs
-- ============================================================================

SELECT 
    da_from.holder_name AS sender_name,
    da_to.holder_name AS receiver_name,
    COUNT(ft.transaction_key) AS transaction_count,
    SUM(ft.amount) AS total_amount_transferred,
    ROUND(AVG(ft.amount), 2) AS avg_amount,
    MAX(dd.full_date) AS most_recent_transaction
FROM FACT_TRANSACTIONS ft
LEFT JOIN DIM_ACCOUNT da_from ON ft.account_from_key = da_from.account_key
LEFT JOIN DIM_ACCOUNT da_to ON ft.account_to_key = da_to.account_key
LEFT JOIN DIM_DATE dd ON ft.date_key = dd.date_key
WHERE ft.status = 'SUCCESS'
GROUP BY da_from.holder_name, da_to.holder_name
HAVING COUNT(ft.transaction_key) > 5
ORDER BY total_amount_transferred DESC;
