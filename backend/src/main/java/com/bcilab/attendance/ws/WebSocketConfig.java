package com.bcilab.attendance.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Exposes ws://<host>/ws/attendance so the React live-display dashboard gets
 * pushed updates the instant someone scans in, instead of polling.
 * The frontend falls back to polling GET /api/v1/attendance/live if the
 * socket connection drops (see frontend/src/hooks/useAttendanceFeed.js).
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AttendanceFeedPublisher publisher;

    public WebSocketConfig(AttendanceFeedPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(publisher, "/ws/attendance").setAllowedOriginPatterns("*");
    }
}
