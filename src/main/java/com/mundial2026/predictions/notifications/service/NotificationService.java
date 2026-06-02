package com.mundial2026.predictions.notifications.service;

import com.mundial2026.predictions.notifications.websocket.NotificationSocket;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NotificationService {

    public void sendToUser(Long userId, String message) {
        NotificationSocket.sendToUser(userId.toString(), message);
    }

    public void broadcast(String message) {
        NotificationSocket.broadcast(message);
    }
}
