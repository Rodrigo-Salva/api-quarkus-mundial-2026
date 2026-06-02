package com.mundial2026.predictions.notifications.repository;

import com.mundial2026.predictions.notifications.entity.NotificationRecord;
import com.mundial2026.predictions.shared.dynamodb.DynamoDbTableInitializer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationRepository {

    @Inject
    DynamoDbClient dynamoDb;

    public void save(NotificationRecord record) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId",    s(record.userId));
        item.put("timestamp", s(record.timestamp));
        item.put("message",   s(record.message));
        item.put("read",      AttributeValue.fromBool(record.read));

        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(DynamoDbTableInitializer.TABLE_NOTIFICATIONS)
                .item(item)
                .build());
    }

    public List<NotificationRecord> findByUserId(String userId, int limit) {
        QueryResponse response = dynamoDb.query(QueryRequest.builder()
                .tableName(DynamoDbTableInitializer.TABLE_NOTIFICATIONS)
                .keyConditionExpression("userId = :pk")
                .expressionAttributeValues(Map.of(":pk", s(userId)))
                .scanIndexForward(false)
                .limit(limit)
                .build());

        return response.items().stream()
                .map(this::toRecord)
                .collect(Collectors.toList());
    }

    private NotificationRecord toRecord(Map<String, AttributeValue> item) {
        NotificationRecord r = new NotificationRecord();
        r.userId    = str(item, "userId");
        r.timestamp = str(item, "timestamp");
        r.message   = str(item, "message");
        r.read      = item.containsKey("read") && Boolean.TRUE.equals(item.get("read").bool());
        return r;
    }

    private AttributeValue s(String v) { return AttributeValue.fromS(v != null ? v : ""); }
    private String str(Map<String, AttributeValue> m, String k) {
        return m.containsKey(k) ? m.get(k).s() : null;
    }
}
