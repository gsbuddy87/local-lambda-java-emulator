package com.emulator.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

public class Handler implements RequestHandler<ComparisonRequest, String> {

    private final S3Service s3Service;
    private final StreamComparator streamComparator;
    private final DatabaseService databaseService;

    public Handler() {
        // 1. Configure S3 Client
        String s3Endpoint = System.getenv("S3_ENDPOINT");
        String region = System.getenv("AWS_REGION");
        if (region == null)
            region = "us-east-1";

        S3ClientBuilder s3Builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (s3Endpoint != null && !s3Endpoint.isEmpty()) {
            s3Builder.endpointOverride(URI.create(s3Endpoint));
            // For LocalStack/MinIO, path style access is often needed
            s3Builder.forcePathStyle(true);
        }

        this.s3Service = new S3Service(s3Builder.build());

        // 2. Configure Database Service
        String dbConnectionString = System.getenv("DB_CONNECTION_STRING");
        if (dbConnectionString == null || dbConnectionString.isEmpty()) {
            // Fallback for local testing if not set? Or fail hard.
            // Let's default to a local sqlite file if not set, for ease of use.
            dbConnectionString = "jdbc:sqlite:/tmp/results.db";
        }

        this.databaseService = new DatabaseService(dbConnectionString);
        try {
            this.databaseService.initDatabase();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }

        // 3. Initialize Comparator
        this.streamComparator = new StreamComparator();
    }

    // Constructor for testing with mocks
    public Handler(S3Service s3Service, StreamComparator streamComparator, DatabaseService databaseService) {
        this.s3Service = s3Service;
        this.streamComparator = streamComparator;
        this.databaseService = databaseService;
    }

    @Override
    public String handleRequest(ComparisonRequest input, Context context) {
        String matchId = UUID.randomUUID().toString();
        context.getLogger()
                .log("Starting comparison " + matchId + " for keys: " + input.getKey1() + ", " + input.getKey2());

        try (InputStream stream1 = s3Service.getFileStream(input.getBucketName(), input.getKey1());
                InputStream stream2 = s3Service.getFileStream(input.getBucketName(), input.getKey2())) {

            StreamComparator.ComparisonResult result = streamComparator.compare(stream1, stream2);

            context.getLogger().log("Comparison Result: " + result);

            databaseService.saveResult(matchId, input.getBucketName(), input.getKey1(), input.getKey2(), result);

            return "Comparison complete. Match ID: " + matchId + ". Result: " + result.message();

        } catch (Exception e) {
            context.getLogger().log("Error during comparison: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Comparison failed", e);
        }
    }
}
