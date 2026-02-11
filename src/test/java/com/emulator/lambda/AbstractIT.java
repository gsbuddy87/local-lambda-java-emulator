package com.emulator.lambda;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Testcontainers
public abstract class AbstractIT {

    @Container
    protected static final LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:2.0"))
            .withServices(LocalStackContainer.Service.S3);

    protected static S3Client s3Client;
    protected static String dbConnectionString;

    @BeforeAll
    static void setup() {
        // Use H2 in-memory database for integration tests
        dbConnectionString = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .region(Region.of(localstack.getRegion()))
                .build();

        // Set system properties for the Handler to use
        System.setProperty("S3_ENDPOINT", localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        System.setProperty("AWS_REGION", localstack.getRegion());
        System.setProperty("DB_CONNECTION_STRING", dbConnectionString);
        System.setProperty("aws.accessKeyId", localstack.getAccessKey());
        System.setProperty("aws.secretAccessKey", localstack.getSecretKey());
    }

    protected void createBucket(String bucketName) {
        s3Client.createBucket(b -> b.bucket(bucketName));
    }

    protected void clearDatabase() throws Exception {
        try (Connection conn = DriverManager.getConnection(dbConnectionString);
                Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM comparison_results");
        }
    }
}
