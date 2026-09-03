package com.example.EV_finder_api.websocket;

import com.example.EV_finder_api.entity.StationService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Raw WebSocket endpoint at /ws. Broadcasts lightweight availability events
 * (e.g. "slot booked" / "slot released") to every connected client; clients
 * react by refreshing the affected data. Non-sensitive info, so no auth.
 */
@Component
public class AvailabilityWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        sessions.remove(session);
    }

    public void broadcastAvailability(StationService service) {
        String payload = "{\"type\":\"availability_changed\",\"serviceId\":\"" + service.getId()
                + "\",\"stationId\":\"" + service.getStation().getId() + "\"}";
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(payload));
                    }
                } catch (IOException ignored) {
                    // client dropped mid-send; cleanup happens in afterConnectionClosed
                }
            }
        }
    }
}
