package com.example.collabboard.network;

import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Consumer;

public class StompClient {

    private final String serverUrl;
    private final String roomCode;
    private final String jwtToken;
    private final Consumer<String> onDataReceived;
    private final Runnable onSuccess;
    private final Consumer<Exception> onFailure;

    private StompSession stompSession;

    public StompClient(String serverUrl, String roomCode, String jwtToken, Consumer<String> onDataReceived, Runnable onSuccess, Consumer<Exception> onFailure) {
        this.serverUrl = Objects.requireNonNull(serverUrl, "serverUrl");
        this.roomCode = Objects.requireNonNull(roomCode, "roomCode");
        this.jwtToken = Objects.requireNonNull(jwtToken, "jwtToken");
        this.onDataReceived = Objects.requireNonNull(onDataReceived, "onDataReceived");
        this.onSuccess = Objects.requireNonNull(onSuccess, "onSuccess");
        this.onFailure = Objects.requireNonNull(onFailure, "onFailure");
    }

    public void connect() {
        try {
            WebSocketClient client = new StandardWebSocketClient();
            WebSocketStompClient stompClient = new WebSocketStompClient(client);
            stompClient.setMessageConverter(new StringMessageConverter());

            WebSocketHttpHeaders webSocketHeaders = new WebSocketHttpHeaders();
            StompHeaders connectHeaders = new StompHeaders();
            connectHeaders.add("Authorization", "Bearer " + jwtToken);
            String targetUrl = Objects.requireNonNull(serverUrl, "serverUrl");

            stompClient.connectAsync(targetUrl, webSocketHeaders, connectHeaders, new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(@NonNull StompSession session, @NonNull StompHeaders connectedHeaders) {
                    System.out.println("STOMP client connected to " + serverUrl);
                    stompSession = session;

                    // Subscribe to the room
                    session.subscribe("/topic/board/" + roomCode, this);
                    System.out.println("Subscribed to /topic/board/" + roomCode);
                    session.send("/app/board/" + roomCode, "JOIN_ROOM");

                    onSuccess.run();
                }

                // THE FIX: Explicitly tell Spring to convert the incoming bytes into a String
                @Override
                public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
                    // Because we added getPayloadType(), this payload is now safely a String
                    if (payload instanceof String) {
                        onDataReceived.accept((String) payload);
                    }
                }

                @Override
                public void handleException(@NonNull StompSession session, @Nullable StompCommand command, @NonNull StompHeaders headers, @NonNull byte[] payload, @NonNull Throwable exception) {
                    System.err.println("STOMP client error: " + exception.getMessage());
                    exception.printStackTrace();
                    onFailure.accept(new Exception("Error during STOMP communication", exception));
                }

                @Override
                public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
                    System.err.println("STOMP transport error: " + exception.getMessage());
                    onFailure.accept(new Exception("Connection to server failed", exception));
                }
            });
        } catch (Exception e) {
            System.err.println("STOMP connection failed: " + e.getMessage());
            onFailure.accept(e);
        }
    }

    public void sendMessage(String data) {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.send("/app/board/" + roomCode, Objects.requireNonNull(data, "data"));
        }
    }

    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        System.out.println("STOMP client disconnected.");
    }
}