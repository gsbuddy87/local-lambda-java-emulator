package com.emulator.lambda;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

public class DatabaseService {

    private final String connectionString;

    static {
        try {
            // Explicitly load drivers for better compatibility in shaded JARs
            Class.forName("org.sqlite.JDBC");
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: JDBC driver not found: " + e.getMessage());
        }
    }

    public DatabaseService(String connectionString) {
        this.connectionString = connectionString;
    }

    public void initDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(connectionString);
                Statement stmt = conn.createStatement()) {

            String sql = """
                        CREATE TABLE IF NOT EXISTS comparison_results (
                            id VARCHAR(50) PRIMARY KEY,
                            timestamp TIMESTAMP,
                            bucket_name VARCHAR(255),
                            key1 VARCHAR(1024),
                            key2 VARCHAR(1024),
                            are_equal BOOLEAN,
                            bytes_processed BIGINT,
                            message TEXT
                        )
                    """;
            stmt.execute(sql);
        }
    }

    public void saveResult(String id, String bucket, String key1, String key2, StreamComparator.ComparisonResult result)
            throws SQLException {
        String sql = "INSERT INTO comparison_results (id, timestamp, bucket_name, key1, key2, are_equal, bytes_processed, message) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(connectionString);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setObject(2, java.sql.Timestamp.from(Instant.now()));
            pstmt.setString(3, bucket);
            pstmt.setString(4, key1);
            pstmt.setString(5, key2);
            pstmt.setBoolean(6, result.areEqual());
            pstmt.setLong(7, result.bytesProcessed());
            pstmt.setString(8, result.message());

            pstmt.executeUpdate();
        }
    }
}
