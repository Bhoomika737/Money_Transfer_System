package com.moneytransfersystem.service.snowflake;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Snowflake Analytics Service
 * Executes analytics queries on the dimensional model
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SnowflakeAnalyticsService {

    private final SnowflakeConnectionManager connectionManager;

    /**
     * Query 1: Daily Transaction Volume
     * Count and sum of transactions by date
     */
    public List<Map<String, Object>> getDailyTransactionVolume() {
        String query = """
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
                GROUP BY dd.full_date, dd.day_name, ft.date_key
                ORDER BY dd.full_date DESC
                LIMIT 30
                """;
        return executeQuery(query);
    }

    /**
     * Query 2: Account Activity (Most Active Accounts)
     * Identifies most active accounts by transaction volume
     */
    public List<Map<String, Object>> getAccountActivity() {
        String query = """
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
                GROUP BY da_from.account_id, da_from.holder_name, da_from.account_type
                ORDER BY total_transactions DESC
                LIMIT 20
                """;
        return executeQuery(query);
    }

    /**
     * Query 3: Success Rate
     * Percentage of successful transfers
     */
    public List<Map<String, Object>> getSuccessRate() {
        String query = """
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
                ORDER BY transaction_count DESC
                """;
        return executeQuery(query);
    }

    /**
     * Query 4: Peak Hours (Busiest Transaction Times)
     * Identifies peak transaction periods
     */
    public List<Map<String, Object>> getPeakHours() {
        String query = """
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
                GROUP BY dd.full_date
                ORDER BY transaction_count DESC
                LIMIT 20
                """;
        return executeQuery(query);
    }

    /**
     * Query 5: Average Transfer Amount
     * Analyzes transfer amounts and statistics
     */
    public List<Map<String, Object>> getAverageTransferAmount() {
        String query = """
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
                GROUP BY ft.transaction_type, ft.currency
                ORDER BY total_amount DESC
                """;
        return executeQuery(query);
    }

    /**
     * Get table record counts
     */
    public Map<String, Long> getDataWarehouseStats() {
        Map<String, Long> stats = new HashMap<>();
        Connection connection = null;

        try {
            connection = connectionManager.getConnection();
            String query = """
                    SELECT 'DIM_ACCOUNT' AS table_name, COUNT(*) AS record_count FROM DIM_ACCOUNT
                    UNION ALL
                    SELECT 'DIM_DATE', COUNT(*) FROM DIM_DATE
                    UNION ALL
                    SELECT 'FACT_TRANSACTIONS', COUNT(*) FROM FACT_TRANSACTIONS
                    """;

            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                stats.put(rs.getString("table_name"), rs.getLong("record_count"));
            }

            log.info("Retrieved data warehouse statistics");

        } catch (SQLException e) {
            log.error("Error retrieving warehouse stats", e);
        } finally {
            connectionManager.closeConnection(connection);
        }

        return stats;
    }

    /**
     * Execute a generic query and return results as list of maps
     */
    private List<Map<String, Object>> executeQuery(String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        Connection connection = null;

        try {
            connection = connectionManager.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metadata.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }

            log.info("Query executed successfully, returned {} rows", results.size());

        } catch (SQLException e) {
            log.error("Error executing analytics query", e);
        } finally {
            connectionManager.closeConnection(connection);
        }

        return results;
    }
}
