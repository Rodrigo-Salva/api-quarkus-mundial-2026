package com.mundial2026.predictions.history.repository;

import com.mundial2026.predictions.history.entity.ActivityRecord;
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
public class ActivityRepository {

    @Inject
    DynamoDbClient dynamoDb;

    public void save(ActivityRecord record) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId",    s(record.userId));
        item.put("timestamp", s(record.timestamp));
        item.put("action",    s(record.action));
        item.put("details",   s(record.details != null ? record.details : "{}"));

        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(DynamoDbTableInitializer.TABLE_ACTIVITY_LOG)
                .item(item)
                .build());
    }

    public List<ActivityRecord> findByUserId(Long userId, int limit) {
        QueryResponse response = dynamoDb.query(QueryRequest.builder()
                .tableName(DynamoDbTableInitializer.TABLE_ACTIVITY_LOG)
                .keyConditionExpression("userId = :pk")
                .expressionAttributeValues(Map.of(":pk", s(String.valueOf(userId))))
                .scanIndexForward(false)
                .limit(limit)
                .build());

        return response.items().stream()
                .map(this::toRecord)
                .collect(Collectors.toList());
    }

    private ActivityRecord toRecord(Map<String, AttributeValue> item) {
        ActivityRecord r = new ActivityRecord();
        r.userId    = str(item, "userId");
        r.timestamp = str(item, "timestamp");
        r.action    = str(item, "action");
        r.details   = str(item, "details");
        return r;
    }

    private AttributeValue s(String v) { return AttributeValue.fromS(v != null ? v : ""); }
    private String str(Map<String, AttributeValue> m, String k) {
        return m.containsKey(k) ? m.get(k).s() : null;
    }
}
