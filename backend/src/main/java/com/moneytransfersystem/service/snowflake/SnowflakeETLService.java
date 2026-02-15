package com.moneytransfersystem.service.snowflake;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnowflakeETLService {

    private final SnowflakeConnectionManager connectionManager;
    private final JdbcTemplate jdbcTemplate;

    public void initializeSchema() {

        log.info("🚀 Starting Snowflake Schema Initialization...");

        try (Connection conn = connectionManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // ℹ️ Database and Schema must be pre-created by ACCOUNTADMIN
            // Using existing MONEY_TRANSFER_DW.ANALYTICS schema
            
            // ✅ Step 1: Set context to existing database and schema
            log.info("Setting context to MONEY_TRANSFER_DW.ANALYTICS...");

            stmt.execute("USE DATABASE MONEY_TRANSFER_DW");
            stmt.execute("USE SCHEMA ANALYTICS");

            log.info("✅ Using MONEY_TRANSFER_DW.ANALYTICS");

            // ✅ Step 2: Create DIM_ACCOUNT table
            log.info("Creating DIM_ACCOUNT...");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS DIM_ACCOUNT (
                    ACCOUNT_KEY NUMBER AUTOINCREMENT PRIMARY KEY,
                    ACCOUNT_ID STRING NOT NULL UNIQUE,
                    HOLDER_NAME VARCHAR(255) NOT NULL,
                    ACCOUNT_NUMBER VARCHAR(50),
                    ACCOUNT_TYPE VARCHAR(50),
                    STATUS VARCHAR(50) NOT NULL,
                    BALANCE NUMBER(19,4),
                    CREATED_DATE DATE,
                    EFFECTIVE_DATE DATE,
                    END_DATE DATE,
                    IS_CURRENT BOOLEAN DEFAULT TRUE,
                    LOAD_DATE TIMESTAMP_NTZ DEFAULT CURRENT_TIMESTAMP()
                )
            """);

            // ✅ Step 3: Create DIM_DATE table
            log.info("Creating DIM_DATE...");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS DIM_DATE (
                    DATE_KEY NUMBER PRIMARY KEY,
                    FULL_DATE DATE NOT NULL UNIQUE,
                    DAY NUMBER NOT NULL,
                    MONTH NUMBER NOT NULL,
                    YEAR NUMBER NOT NULL,
                    QUARTER NUMBER NOT NULL,
                    DAY_NAME VARCHAR(20) NOT NULL,
                    MONTH_NAME VARCHAR(20) NOT NULL,
                    IS_WEEKEND BOOLEAN NOT NULL,
                    IS_HOLIDAY BOOLEAN DEFAULT FALSE,
                    WEEK_NUMBER NUMBER NOT NULL,
                    FISCAL_YEAR NUMBER
                )
            """);

            // ✅ Step 4: Create FACT_TRANSACTIONS table
            log.info("Creating FACT_TRANSACTIONS...");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS FACT_TRANSACTIONS (
                    TRANSACTION_KEY NUMBER AUTOINCREMENT PRIMARY KEY,
                    TRANSACTION_ID VARCHAR(64) NOT NULL UNIQUE,
                    ACCOUNT_FROM_KEY NUMBER,
                    ACCOUNT_TO_KEY NUMBER,
                    DATE_KEY NUMBER NOT NULL,
                    AMOUNT NUMBER(19,4),
                    CURRENCY VARCHAR(3) NOT NULL,
                    STATUS VARCHAR(50),
                    TRANSACTION_TYPE VARCHAR(50) NOT NULL,
                    DESCRIPTION VARCHAR(500),
                    REFERENCE_NUMBER VARCHAR(100),
                    CREATED_ON TIMESTAMP_NTZ(6),
                    FAILURE_REASON VARCHAR(255),
                    IDEMPOTENCY_KEY VARCHAR(255) NOT NULL UNIQUE,
                    REMARKS VARCHAR(255),
                    LOAD_DATE TIMESTAMP_NTZ(9) DEFAULT CURRENT_TIMESTAMP()
                )
            """);

            log.info("🎉 Snowflake Schema Initialized Successfully!");

        } catch (Exception e) {

            log.error("❌ Snowflake Init Failed!", e);

            // Throw real error back to controller
            throw new RuntimeException("Snowflake Schema Init Error: " + e.getMessage());
        }
    }

    /**
     * Load all data from MySQL to Snowflake
     * Populates dimension tables and fact table with actual application data
     */
    public void loadDataToSnowflake() {
        log.info("📊 Starting ETL Pipeline - Loading Data to Snowflake...");

        try {
            // Step 1: Load date dimension (730 days)
            loadDateDimension();

            // Step 2: Load account dimension from MySQL
            loadAccountDimension();

            // Step 3: Load transaction facts from MySQL
            loadTransactionFacts();

            log.info("✅ ETL Pipeline Completed Successfully!");

        } catch (Exception e) {
            log.error("❌ ETL Pipeline Failed!", e);
            throw new RuntimeException("ETL Pipeline Error: " + e.getMessage());
        }
    }

    /**
     * Populate DIM_DATE table with 730 days of data
     */
    private void loadDateDimension() {
        log.info("📅 Loading Date Dimension (730 days)...");

        try (Connection conn = connectionManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Insert 730 days of date dimension data (populate all NOT NULL fields)
            String dateInsertSql = """
                INSERT INTO DIM_DATE (DATE_KEY, FULL_DATE, DAY, MONTH, YEAR, QUARTER, DAY_NAME, MONTH_NAME, IS_WEEKEND, WEEK_NUMBER, FISCAL_YEAR)
                WITH RECURSIVE date_range AS (
                    SELECT CURRENT_DATE - INTERVAL '730 days' as date_val
                    UNION ALL
                    SELECT date_val + INTERVAL '1 day' as date_val
                    FROM date_range
                    WHERE date_val < CURRENT_DATE
                )
                SELECT
                    TO_NUMBER(TO_CHAR(date_val, 'YYYYMMDD')) as DATE_KEY,
                    date_val as FULL_DATE,
                    DAY(date_val) as DAY,
                    MONTH(date_val) as MONTH,
                    YEAR(date_val) as YEAR,
                    QUARTER(date_val) as QUARTER,
                    TO_CHAR(date_val, 'DDDD') as DAY_NAME,
                    TO_CHAR(date_val, 'MMMM') as MONTH_NAME,
                    CASE WHEN DAYOFWEEKISO(date_val) IN (6,7) THEN TRUE ELSE FALSE END as IS_WEEKEND,
                    WEEK(date_val) as WEEK_NUMBER,
                    CASE WHEN MONTH(date_val) >= 4 THEN YEAR(date_val) ELSE YEAR(date_val) - 1 END as FISCAL_YEAR
                FROM date_range
                WHERE NOT EXISTS (
                    SELECT 1 FROM DIM_DATE 
                    WHERE FULL_DATE = date_val
                )
                """;

            stmt.execute(dateInsertSql);

            // Verify there are no NULLs in required columns that could cause future DML failures
            try (ResultSet verify = stmt.executeQuery(
                    "SELECT COUNT(*) FROM DIM_DATE WHERE DAY IS NULL OR MONTH IS NULL OR DAY_NAME IS NULL OR WEEK_NUMBER IS NULL")) {
                if (verify.next() && verify.getInt(1) > 0) {
                    log.error("❌ DIM_DATE contains NULLs in required columns (day/month/day_name/week_number) — count={}", verify.getInt(1));
                    throw new RuntimeException("DIM_DATE verification failed: required columns contain NULLs");
                }
            }

            log.info("✅ Date Dimension Loaded and Verified");

        } catch (Exception e) {
            log.error("⚠️ Error loading date dimension (if empty, that's okay on first run)", e);
        }
    }

    /**
     * Load accounts from MySQL accounts table to Snowflake DIM_ACCOUNT
     */
    private void loadAccountDimension() {
        log.info("👥 Loading Account Dimension from MySQL...");

        try {
            // Fetch from MySQL
            String mysqlQuery = "SELECT account_id, holder_name, status, balance FROM accounts";
            
            var accounts = jdbcTemplate.query(mysqlQuery, (rs, rowNum) -> {
                Map<String, Object> map = new HashMap<>();
                map.put("account_id", rs.getString("account_id"));
                map.put("holder_name", rs.getString("holder_name"));
                map.put("status", rs.getString("status"));
                map.put("balance", rs.getDouble("balance"));
                return map;
            });

            if (accounts.isEmpty()) {
                log.warn("ℹ️ No accounts found in MySQL");
                return;
            }

            log.info("Found {} accounts in MySQL", accounts.size());

            // Upsert into Snowflake using MERGE (update if exists, insert if not)
            try (Connection conn = connectionManager.getConnection()) {
                String mergeSql = """
                    MERGE INTO DIM_ACCOUNT d
                    USING (SELECT ? AS ACCOUNT_ID, ? AS HOLDER_NAME, ? AS STATUS, ? AS BALANCE) s
                    ON d.ACCOUNT_ID = s.ACCOUNT_ID
                    WHEN MATCHED THEN
                      UPDATE SET HOLDER_NAME = s.HOLDER_NAME, STATUS = s.STATUS, BALANCE = s.BALANCE, LAST_UPDATED = CURRENT_TIMESTAMP()
                    WHEN NOT MATCHED THEN
                      INSERT (ACCOUNT_ID, HOLDER_NAME, STATUS, BALANCE, LAST_UPDATED)
                      VALUES (s.ACCOUNT_ID, s.HOLDER_NAME, s.STATUS, s.BALANCE, CURRENT_TIMESTAMP())
                    """;

                try (PreparedStatement pstmt = conn.prepareStatement(mergeSql)) {
                    int count = 0;
                    for (Map<String, Object> account : accounts) {
                        pstmt.setString(1, (String) account.get("account_id"));
                        pstmt.setString(2, (String) account.get("holder_name"));
                        pstmt.setString(3, (String) account.get("status"));
                        pstmt.setDouble(4, ((Number) account.get("balance")).doubleValue());
                        pstmt.addBatch();
                        count++;
                        log.debug("✓ Queued account: {} holder={}", (String) account.get("account_id"), (String) account.get("holder_name"));
                        if (count % 100 == 0) {
                            pstmt.executeBatch();
                        }
                    }
                    pstmt.executeBatch();
                    log.info("✅ Upserted {} accounts to Snowflake", accounts.size());
                }
            }

        } catch (Exception e) {
            log.error("⚠️ Error loading account dimension: {}", e.getMessage());
        }
    }

    /**
     * Load transactions from MySQL to Snowflake FACT_TRANSACTIONS
     */
    private void loadTransactionFacts() {
        log.info("💳 Loading Transaction Facts from MySQL...");

        try {
            // Fetch transactions from MySQL
            String mysqlQuery = """
                SELECT 
                    t.transaction_id,
                    t.from_account_id,
                    t.to_account_id,
                    DATE(t.created_on) as transaction_date,
                    t.amount,
                    t.status,
                    'TRANSFER' as transaction_type,
                    'USD' as currency,
                    t.created_on as created_on,
                    t.idempotency_key,
                    t.failure_reason,
                    t.remarks
                FROM transaction_logs t
                """;
            
            var transactions = jdbcTemplate.query(mysqlQuery, (rs, rowNum) -> {
                Map<String, Object> map = new HashMap<>();
                map.put("transaction_id", rs.getString("transaction_id"));
                map.put("from_account_id", rs.getString("from_account_id"));
                map.put("to_account_id", rs.getString("to_account_id"));
                map.put("transaction_date", rs.getDate("transaction_date"));
                map.put("amount", rs.getDouble("amount"));
                map.put("status", rs.getString("status"));
                map.put("transaction_type", rs.getString("transaction_type"));
                map.put("currency", rs.getString("currency"));
                map.put("created_on", rs.getTimestamp("created_on"));
                map.put("idempotency_key", rs.getString("idempotency_key"));
                map.put("failure_reason", rs.getString("failure_reason"));
                map.put("remarks", rs.getString("remarks"));
                return map;
            });

            if (transactions.isEmpty()) {
                log.warn("ℹ️ No transactions found in MySQL");
                return;
            }

            // Convert to List<TransactionData>
            List<TransactionData> txnDataList = new java.util.ArrayList<>();
            for (Map<String, Object> txn : transactions) {
                txnDataList.add(new TransactionData(
                        (String) txn.get("transaction_id"),
                        (String) txn.get("from_account_id"),
                        (String) txn.get("to_account_id"),
                        (java.sql.Date) txn.get("transaction_date"),
                        ((Number) txn.get("amount")).doubleValue(),
                        (String) txn.get("status"),
                        (String) txn.get("transaction_type"),
                        (String) txn.get("currency"),
                        (java.sql.Timestamp) txn.get("created_on"),
                        (String) txn.get("idempotency_key"),
                        (String) txn.get("failure_reason"),
                        (String) txn.get("remarks")
                ));
            }
            // Load to Snowflake
            try (Connection conn = connectionManager.getConnection()) {
                insertTransactionsToSnowflake(conn, txnDataList);
                log.info("✅ Loaded {} transactions to Snowflake", txnDataList.size());
            }

        } catch (Exception e) {
            log.error("⚠️ Error loading transaction facts", e);
        }
    }

    /**
     * Insert transactions to Snowflake with account key and date key lookups
     */
    private void insertTransactionsToSnowflake(Connection conn, java.util.List<TransactionData> transactions) 
            throws Exception {
        // Use MERGE to insert new transactions and update existing ones by transaction_id
        String mergeSql = """
            MERGE INTO FACT_TRANSACTIONS t
            USING (SELECT ? AS TRANSACTION_ID, ? AS ACCOUNT_FROM_KEY, ? AS ACCOUNT_TO_KEY, ? AS DATE_KEY,
                          ? AS AMOUNT, ? AS STATUS, ? AS TRANSACTION_TYPE, ? AS CURRENCY, ? AS CREATED_ON, ? AS IDEMPOTENCY_KEY, ? AS FAILURE_REASON, ? AS REMARKS) s
            ON t.TRANSACTION_ID = s.TRANSACTION_ID
            WHEN MATCHED THEN
              UPDATE SET ACCOUNT_FROM_KEY = s.ACCOUNT_FROM_KEY, ACCOUNT_TO_KEY = s.ACCOUNT_TO_KEY, DATE_KEY = s.DATE_KEY,
                         AMOUNT = s.AMOUNT, STATUS = s.STATUS, TRANSACTION_TYPE = s.TRANSACTION_TYPE, CURRENCY = s.CURRENCY,
                         CREATED_ON = s.CREATED_ON, FAILURE_REASON = s.FAILURE_REASON, REMARKS = s.REMARKS
            WHEN NOT MATCHED THEN
              INSERT (TRANSACTION_ID, ACCOUNT_FROM_KEY, ACCOUNT_TO_KEY, DATE_KEY, AMOUNT, STATUS, TRANSACTION_TYPE, CURRENCY, CREATED_ON, IDEMPOTENCY_KEY, FAILURE_REASON, REMARKS)
              VALUES (s.TRANSACTION_ID, s.ACCOUNT_FROM_KEY, s.ACCOUNT_TO_KEY, s.DATE_KEY, s.AMOUNT, s.STATUS, s.TRANSACTION_TYPE, s.CURRENCY, s.CREATED_ON, s.IDEMPOTENCY_KEY, s.FAILURE_REASON, s.REMARKS)
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(mergeSql)) {
            int batch = 0;
            int skipped = 0;
            int inserted = 0;
            for (TransactionData txn : transactions) {
                int fromKey = getAccountKey(conn, txn.accountFrom);
                int toKey = getAccountKey(conn, txn.accountTo);
                int dateKey = getDateKey(conn, txn.transactionDate);

                // Only require dateKey and idempotencyKey
                if (dateKey > 0 && txn.idempotencyKey != null) {
                    pstmt.setString(1, txn.transactionId);
                    if (fromKey > 0) pstmt.setInt(2, fromKey); else pstmt.setNull(2, java.sql.Types.INTEGER);
                    if (toKey > 0) pstmt.setInt(3, toKey); else pstmt.setNull(3, java.sql.Types.INTEGER);
                    pstmt.setInt(4, dateKey);
                    pstmt.setDouble(5, txn.amount);
                    pstmt.setString(6, txn.status);
                    pstmt.setString(7, txn.transactionType);
                    pstmt.setString(8, txn.currency);
                    if (txn.transactionTime != null) pstmt.setTimestamp(9, txn.transactionTime);
                    else pstmt.setNull(9, java.sql.Types.TIMESTAMP);
                    pstmt.setString(10, txn.idempotencyKey);
                    pstmt.setString(11, txn.failureReason);
                    pstmt.setString(12, txn.remarks);
                    pstmt.addBatch();
                    batch++;
                    inserted++;
                    if (batch % 100 == 0) pstmt.executeBatch();
                    log.debug("✓ Queued txn: {} from_account={} (key={}) to_account={} (key={}) date_key={}", 
                             txn.transactionId, txn.accountFrom, fromKey, txn.accountTo, toKey, dateKey);
                } else {
                    skipped++;
                    log.warn("⚠️ Skipping txn: {} (missing keys) from={} to={} dateKey={} idempotency={}", 
                             txn.transactionId, txn.accountFrom, txn.accountTo, dateKey, txn.idempotencyKey);
                }
            }
            pstmt.executeBatch();
            log.info("✅ Merged {} transaction records (inserted={}, skipped={})", transactions.size(), inserted, skipped);
        }
    }

    /**
     * Get account key from account ID
     */
    private int getAccountKey(Connection conn, String accountId) throws Exception {
        String sql = "SELECT ACCOUNT_KEY FROM DIM_ACCOUNT WHERE ACCOUNT_ID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("ACCOUNT_KEY");
            }
        }
        return 0;
    }

    /**
     * Get date key from date
     */
    private int getDateKey(Connection conn, java.sql.Date sqlDate) throws Exception {
        String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(sqlDate);
        int dateKey = Integer.parseInt(dateStr);
        
        String sql = "SELECT DATE_KEY FROM DIM_DATE WHERE DATE_KEY = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, dateKey);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("DATE_KEY");
            }
        }
        return 0;
    }

    /**
     * Data class for transaction transfers
     */
    private static class TransactionData {
        String transactionId;
        String accountFrom;
        String accountTo;
        java.sql.Date transactionDate;
        double amount;
        String status;
        String transactionType;
        String currency;
        java.sql.Timestamp transactionTime;
        String idempotencyKey;
        String failureReason;
        String remarks;

        TransactionData(String transactionId, String accountFrom, String accountTo, 
                       java.sql.Date transactionDate, double amount, String status,
                       String transactionType, String currency, java.sql.Timestamp transactionTime,
                       String idempotencyKey, String failureReason, String remarks) {
            this.transactionId = transactionId;
            this.accountFrom = accountFrom;
            this.accountTo = accountTo;
            this.transactionDate = transactionDate;
            this.amount = amount;
            this.status = status;
            this.transactionType = transactionType;
            this.currency = currency;
            this.transactionTime = transactionTime;
            this.idempotencyKey = idempotencyKey;
            this.failureReason = failureReason;
            this.remarks = remarks;
        }
    }
}
