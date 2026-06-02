package com.mundial2026.predictions.realtime.websocket;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket(path = "/ws/match/{matchId}")
public class MatchSocket {

    private static final Map<String, Set<WebSocketConnection>> rooms = new ConcurrentHashMap<>();

    @Inject
    WebSocketConnection connection;

    @OnOpen
    public String onOpen() {
        String matchId = connection.pathParam("matchId");
        rooms.computeIfAbsent(matchId, k -> ConcurrentHashMap.newKeySet()).add(connection);
        return "{\"event\":\"connected\",\"matchId\":\"" + matchId + "\"}";
    }

    @OnClose
    public void onClose() {
        String matchId = connection.pathParam("matchId");
        Set<WebSocketConnection> room = rooms.get(matchId);
        if (room != null) {
            room.remove(connection);
            if (room.isEmpty()) rooms.remove(matchId);
        }
    }

    @OnTextMessage
    public void onMessage(String message) {
        // Solo lectura — clientes no envían mensajes en este canal
    }

    public static void broadcastToMatch(String matchId, String message) {
        Set<WebSocketConnection> room = rooms.get(matchId);
        if (room != null) {
            room.stream()
                    .filter(WebSocketConnection::isOpen)
                    .forEach(conn -> conn.sendTextAndAwait(message));
        }
    }
}
