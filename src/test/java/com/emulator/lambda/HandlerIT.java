package com.emulator.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.sync.RequestBody;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class HandlerIT extends AbstractIT {

    private Handler handler;
    private Context context;

    @BeforeEach
    void setUp() throws Exception {
        context = Mockito.mock(Context.class);
        com.amazonaws.services.lambda.runtime.LambdaLogger logger = Mockito
                .mock(com.amazonaws.services.lambda.runtime.LambdaLogger.class);
        Mockito.when(context.getLogger()).thenReturn(logger);

        // Re-initialize handler to pick up updated system properties from AbstractIT
        handler = new Handler();

        createBucket("test-bucket");
        clearDatabase(); // Ensure clean state between tests
    }

    @Test
    void testEndToEndComparison() throws Exception {
        // 1. Upload files to S3
        s3Client.putObject(b -> b.bucket("test-bucket").key("file1.txt"),
                RequestBody.fromString("This is a test file."));
        s3Client.putObject(b -> b.bucket("test-bucket").key("file2.txt"),
                RequestBody.fromString("This is a test file."));

        // 2. Invoke Handler
        ComparisonRequest request = new ComparisonRequest();
        request.setBucketName("test-bucket");
        request.setKey1("file1.txt");
        request.setKey2("file2.txt");

        String response = handler.handleRequest(request, context);

        // 3. Verify Response
        assertNotNull(response);
        assertTrue(response.contains("Comparison complete"));
        assertTrue(response.contains("Files are identical"));

        // 4. Verify Database Record
        try (Connection conn = DriverManager.getConnection(dbConnectionString);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM comparison_results")) {

            assertTrue(rs.next(), "Should have one record in database");
            assertEquals("test-bucket", rs.getString("bucket_name"));
            assertEquals("file1.txt", rs.getString("key1"));
            assertEquals("file2.txt", rs.getString("key2"));
            assertTrue(rs.getBoolean("are_equal"));
        }
    }

    @Test
    void testMismatchComparison() throws Exception {
        // 1. Upload files to S3
        s3Client.putObject(b -> b.bucket("test-bucket").key("fileA.txt"),
                RequestBody.fromString("Content A"));
        s3Client.putObject(b -> b.bucket("test-bucket").key("fileB.txt"),
                RequestBody.fromString("Content B"));

        // 2. Invoke Handler
        ComparisonRequest request = new ComparisonRequest();
        request.setBucketName("test-bucket");
        request.setKey1("fileA.txt");
        request.setKey2("fileB.txt");

        String response = handler.handleRequest(request, context);

        // 3. Verify Response
        assertNotNull(response);
        assertTrue(response.contains("Content mismatch found"));

        // 4. Verify Database Record
        try (Connection conn = DriverManager.getConnection(dbConnectionString);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM comparison_results WHERE key1 = 'fileA.txt'")) {

            assertTrue(rs.next(), "Should have the record in database");
            assertFalse(rs.getBoolean("are_equal"));
        }
    }
}
