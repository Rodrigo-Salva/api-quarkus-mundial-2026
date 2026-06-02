package com.mundial2026.predictions.matches.repository;

import com.mundial2026.predictions.matches.entity.MatchEvent;
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
public class MatchEventRepository {

    @Inject
    DynamoDbClient dynamoDb;

    public void save(MatchEvent event) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("matchId",    s(event.matchId));
        item.put("timestamp",  s(event.timestamp));
        item.put("type",       s(event.type));
        item.put("status",     s(event.status != null ? event.status : ""));
        item.put("homeScore",  n(event.homeScore));
        item.put("awayScore",  n(event.awayScore));

        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(DynamoDbTableInitializer.TABLE_MATCH_EVENTS)
                .item(item)
                .build());
    }

    public List<MatchEvent> findByMatchId(String matchId) {
        QueryResponse response = dynamoDb.query(QueryRequest.builder()
                .tableName(DynamoDbTableInitializer.TABLE_MATCH_EVENTS)
                .keyConditionExpression("matchId = :pk")
                .expressionAttributeValues(Map.of(":pk", s(matchId)))
                .scanIndexForward(false) // más recientes primero
                .build());

        return response.items().stream()
                .map(this::toMatchEvent)
                .collect(Collectors.toList());
    }

    private MatchEvent toMatchEvent(Map<String, AttributeValue> item) {
        MatchEvent e = new MatchEvent();
        e.matchId   = str(item, "matchId");
        e.timestamp = str(item, "timestamp");
        e.type      = str(item, "type");
        e.status    = str(item, "status");
        e.homeScore = num(item, "homeScore");
        e.awayScore = num(item, "awayScore");
        return e;
    }

    private AttributeValue s(String v)  { return AttributeValue.fromS(v != null ? v : ""); }
    private AttributeValue n(Integer v) { return AttributeValue.fromN(v != null ? String.valueOf(v) : "0"); }
    private String str(Map<String, AttributeValue> m, String k) {
        return m.containsKey(k) ? m.get(k).s() : null;
    }
    private Integer num(Map<String, AttributeValue> m, String k) {
        return m.containsKey(k) ? Integer.parseInt(m.get(k).n()) : null;
    }
}
