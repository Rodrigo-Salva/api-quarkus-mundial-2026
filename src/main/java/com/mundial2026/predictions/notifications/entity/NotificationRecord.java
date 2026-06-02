package com.mundial2026.predictions.notifications.entity;

/**
 * Notificación persistida en DynamoDB.
 * PK: userId  SK: timestamp#notifId
 */
public class NotificationRecord {

    public String userId;
    public String timestamp;   // ISO-8601 + "#" + UUID como sort key
    public String message;
    public boolean read = false;

    public static NotificationRecord of(String userId, String message) {
        NotificationRecord n = new NotificationRecord();
        n.userId    = userId;
        n.timestamp = java.time.Instant.now() + "#" + java.util.UUID.randomUUID();
        n.message   = message;
        n.read      = false;
        return n;
    }
}
