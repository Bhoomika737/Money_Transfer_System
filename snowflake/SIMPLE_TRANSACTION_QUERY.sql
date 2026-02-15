-- ============================================
-- Simple Query to Show All Transactions
-- ============================================

-- Query 1: Basic Transaction List (Simplest)
SELECT 
    ft.ACCOUNT_FROM_KEY,
    ft.ACCOUNT_TO_KEY,
    ft.AMOUNT,
    ft.STATUS,
    ft.LOAD_DATE,
    ft.CURRENCY
FROM FACT_TRANSACTIONS ft
ORDER BY ft.LOAD_DATE DESC;


-- Query 2: Transaction List with Account Names (Recommended)
SELECT 
    da_from.ACCOUNT_ID as FROM_ACCOUNT,
    da_from.HOLDER_NAME as FROM_HOLDER,
    da_to.ACCOUNT_ID as TO_ACCOUNT,
    da_to.HOLDER_NAME as TO_HOLDER,
    ft.AMOUNT,
    ft.STATUS,
    ft.TRANSACTION_TYPE,
    ft.CURRENCY,
    ft.LOAD_DATE
FROM FACT_TRANSACTIONS ft
JOIN DIM_ACCOUNT da_from ON ft.ACCOUNT_FROM_KEY = da_from.ACCOUNT_KEY
JOIN DIM_ACCOUNT da_to ON ft.ACCOUNT_TO_KEY = da_to.ACCOUNT_KEY
ORDER BY ft.LOAD_DATE DESC;


-- Query 3: Count All Transactions
SELECT 
    COUNT(*) as TOTAL_TRANSACTIONS,
    SUM(AMOUNT) as TOTAL_AMOUNT,
    COUNT(DISTINCT ACCOUNT_FROM_KEY) as UNIQUE_SENDERS,
    COUNT(DISTINCT ACCOUNT_TO_KEY) as UNIQUE_RECEIVERS
FROM FACT_TRANSACTIONS;


-- Query 4: Transactions by Status
SELECT 
    STATUS,
    COUNT(*) as TRANSACTION_COUNT,
    SUM(AMOUNT) as TOTAL_AMOUNT,
    AVG(AMOUNT) as AVERAGE_AMOUNT
FROM FACT_TRANSACTIONS
GROUP BY STATUS
ORDER BY TRANSACTION_COUNT DESC;


-- Query 5: Recent 10 Transactions (Last 10)
SELECT 
    da_from.ACCOUNT_ID as FROM_ACCOUNT,
    da_from.HOLDER_NAME as FROM_HOLDER,
    da_to.ACCOUNT_ID as TO_ACCOUNT,
    da_to.HOLDER_NAME as TO_HOLDER,
    ft.AMOUNT,
    ft.STATUS,
    ft.LOAD_DATE
FROM FACT_TRANSACTIONS ft
JOIN DIM_ACCOUNT da_from ON ft.ACCOUNT_FROM_KEY = da_from.ACCOUNT_KEY
JOIN DIM_ACCOUNT da_to ON ft.ACCOUNT_TO_KEY = da_to.ACCOUNT_KEY
ORDER BY ft.LOAD_DATE DESC
LIMIT 10;
