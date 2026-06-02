package com.mundial2026.predictions.history.entity;

/**
 * Registro de actividad de usuario almacenado en DynamoDB.
 * PK: userId  SK: timestamp#actionId
 */
public class ActivityRecord {

    public String userId;
    public String timestamp;   // ISO-8601 + "#" + UUID como sort key
    public String action;      // REGISTER, LOGIN, PREDICTION_SUBMITTED, LEAGUE_CREATED, LEAGUE_JOINED
    public String details;     // JSON con contexto adicional

    public static ActivityRecord of(Long userId, String action, String details) {
        ActivityRecord r = new ActivityRecord();
        r.userId    = String.valueOf(userId);
        r.timestamp = java.time.Instant.now() + "#" + java.util.UUID.randomUUID();
        r.action    = action;
        r.details   = details;
        return r;
    }
}
