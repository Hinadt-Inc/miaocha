package com.hinadt.miaocha.ai;

import com.hinadt.miaocha.ai.push.PushWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class AIWebSocketEndpoint implements WebSocketConfigurer {

    public static final String WEBSOCKET_ENDPOINT = "/api/ai/ws";
    @Autowired private PushWebSocketHandler webSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, WEBSOCKET_ENDPOINT).setAllowedOriginPatterns("*");
    }
}
