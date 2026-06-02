package com.mundial2026.predictions.shared.dynamodb;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@ApplicationScoped
public class DynamoDbClientProducer {

    @ConfigProperty(name = "quarkus.dynamodb.endpoint-override", defaultValue = "")
    String endpointOverride;

    @ConfigProperty(name = "quarkus.dynamodb.aws.region", defaultValue = "us-east-1")
    String region;

    @ConfigProperty(name = "quarkus.dynamodb.aws.credentials.static-provider.access-key-id", defaultValue = "local")
    String accessKey;

    @ConfigProperty(name = "quarkus.dynamodb.aws.credentials.static-provider.secret-access-key", defaultValue = "local")
    String secretKey;

    @Produces
    @ApplicationScoped
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));

        if (!endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride));
        }

        return builder.build();
    }
}
