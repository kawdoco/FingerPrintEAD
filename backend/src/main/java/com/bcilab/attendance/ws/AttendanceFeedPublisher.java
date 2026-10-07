package com.bcilab.attendance.ws;

import com.bcilab.attendance.dto.AttendanceRecordDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.CopyOnWriteArraySet;

/** Broadcasts each new attendance record to every connected dashboard. */
@Component
public class AttendanceFeedPublisher extends TextWebSocketHandler {

    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;

    public AttendanceFeedPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        sessions.remove(session);
    }

    public void publish(AttendanceRecordDto record) {
        final TextMessage message;
        try {
            message = new TextMessage(objectMapper.writeValueAsString(record));
        } catch (Exception e) {
            return;
        }
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                }
            } catch (Exception e) {
                sessions.remove(session);
            }
        }
    }
}
