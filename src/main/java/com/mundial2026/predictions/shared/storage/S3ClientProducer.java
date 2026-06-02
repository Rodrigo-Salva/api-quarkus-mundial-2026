package com.mundial2026.predictions.shared.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@ApplicationScoped
public class S3ClientProducer {

    @ConfigProperty(name = "storage.endpoint", defaultValue = "http://localhost:9000")
    String endpoint;

    @ConfigProperty(name = "storage.region", defaultValue = "us-east-1")
    String region;

    @ConfigProperty(name = "storage.access-key", defaultValue = "dev_access_key")
    String accessKey;

    @ConfigProperty(name = "storage.secret-key", defaultValue = "dev_secret_key")
    String secretKey;

    @ConfigProperty(name = "storage.path-style", defaultValue = "true")
    boolean pathStyle;

    @Produces
    @ApplicationScoped
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));

        // MinIO y S3 compatible requieren path-style en local
        if (pathStyle) {
            builder.serviceConfiguration(s -> s.pathStyleAccessEnabled(true));
        }

        // Endpoint personalizado (MinIO en dev, vacío en prod usa AWS directo)
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
