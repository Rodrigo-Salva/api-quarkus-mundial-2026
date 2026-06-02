package com.mundial2026.predictions.notifications.consumer;

import com.mundial2026.predictions.notifications.entity.NotificationRecord;
import com.mundial2026.predictions.notifications.repository.NotificationRepository;
import com.mundial2026.predictions.notifications.service.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class NotificationConsumer {

    @Inject
    NotificationService notificationService;

    @Inject
    NotificationRepository notificationRepository;

    @Incoming("notifications-in")
    public void consume(String message) {
        // WebSocket: enviar en tiempo real
        notificationService.broadcast(message);
        // DynamoDB: persistir para consulta posterior (GET /api/notifications)
        // Formato esperado del mensaje: { "userId": "123", "message": "..." }
        String userId = extractUserId(message);
        if (userId != null) {
            notificationRepository.save(NotificationRecord.of(userId, message));
        }
    }

    private String extractUserId(String json) {
        String key = "\"userId\":\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }
}
