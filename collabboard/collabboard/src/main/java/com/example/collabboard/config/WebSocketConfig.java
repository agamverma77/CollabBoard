package com.example.collabboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * This class configures the WebSocket message broker for the application.
 * It sets up the endpoints for clients to connect to and the prefixes for message routing.
 */
@Configuration
@EnableWebSocketMessageBroker // Enables WebSocket message handling, backed by a message broker.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

    public WebSocketConfig(WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor) {
        this.webSocketAuthChannelInterceptor = webSocketAuthChannelInterceptor;
    }

    /**
     * Registers the STOMP endpoints, mapping each endpoint to a specific URL and
     * enabling SockJS fallback options.
     * @param registry The STOMP endpoint registry.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The `/ws` endpoint is the URL that clients will connect to for WebSocket communication.
        // `setAllowedOriginPatterns("*")` allows connections from any origin, which is useful for development.
        // `withSockJS()` enables fallback options for browsers that don't support WebSocket.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    /**
     * Configures the message broker, which is responsible for routing messages
     * from one client to another.
     * @param registry The message broker registry.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Defines that messages sent to destinations with the "/app" prefix should be routed
        // to @MessageMapping annotated methods in @Controller classes.
        registry.setApplicationDestinationPrefixes("/app");

        // Defines that messages should be routed to the simple in-memory message broker
        // for destinations with the "/topic" prefix. The broker then broadcasts these
        // messages to all connected clients who are subscribed to that topic.
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthChannelInterceptor);
    }
}

