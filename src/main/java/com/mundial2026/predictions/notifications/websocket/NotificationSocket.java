package com.mundial2026.predictions.notifications.websocket;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket(path = "/ws/notifications")
public class NotificationSocket {

    private static final Map<String, WebSocketConnection> sessions = new ConcurrentHashMap<>();

    @Inject
    WebSocketConnection connection;

    @OnOpen
    public void onOpen() {
        String userId = connection.handshakeRequest().header("X-User-Id");
        if (userId != null) {
            sessions.put(userId, connection);
        }
    }

    @OnClose
    public void onClose() {
        sessions.values().remove(connection);
    }

    @OnTextMessage
    public String onMessage(String message) {
        return "ack:" + message;
    }

    public static void sendToUser(String userId, String message) {
        WebSocketConnection conn = sessions.get(userId);
        if (conn != null && conn.isOpen()) {
            conn.sendTextAndAwait(message);
        }
    }

    public static void broadcast(String message) {
        sessions.values().stream()
                .filter(WebSocketConnection::isOpen)
                .forEach(conn -> conn.sendTextAndAwait(message));
    }
}
