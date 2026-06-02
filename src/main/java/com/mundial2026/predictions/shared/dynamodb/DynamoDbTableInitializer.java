package com.mundial2026.predictions.shared.dynamodb;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;

@ApplicationScoped
public class DynamoDbTableInitializer {

    private static final Logger LOG = Logger.getLogger(DynamoDbTableInitializer.class);

    public static final String TABLE_MATCH_EVENTS   = "match-events";
    public static final String TABLE_ACTIVITY_LOG   = "activity-log";
    public static final String TABLE_NOTIFICATIONS  = "notifications";

    @Inject
    DynamoDbClient dynamoDb;

    void onStart(@Observes StartupEvent event) {
        createIfNotExists(TABLE_MATCH_EVENTS,  "matchId",  "timestamp");
        createIfNotExists(TABLE_ACTIVITY_LOG,  "userId",   "timestamp");
        createIfNotExists(TABLE_NOTIFICATIONS, "userId",   "timestamp");
    }

    private void createIfNotExists(String tableName, String pk, String sk) {
        try {
            dynamoDb.describeTable(r -> r.tableName(tableName));
            LOG.infof("DynamoDB table already exists: %s", tableName);
        } catch (ResourceNotFoundException e) {
            dynamoDb.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName(pk).attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName(sk).attributeType(ScalarAttributeType.S).build()
                    )
                    .keySchema(
                            KeySchemaElement.builder().attributeName(pk).keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName(sk).keyType(KeyType.RANGE).build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            LOG.infof("DynamoDB table created: %s", tableName);
        }
    }
}
