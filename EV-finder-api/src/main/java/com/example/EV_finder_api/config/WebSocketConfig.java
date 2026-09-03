package com.example.EV_finder_api.config;

import com.example.EV_finder_api.websocket.AvailabilityWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AvailabilityWebSocketHandler availabilityWebSocketHandler;

    public WebSocketConfig(AvailabilityWebSocketHandler availabilityWebSocketHandler) {
        this.availabilityWebSocketHandler = availabilityWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(availabilityWebSocketHandler, "/ws").setAllowedOrigins("*");
    }
}
