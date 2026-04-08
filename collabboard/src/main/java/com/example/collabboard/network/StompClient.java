package com.example.collabboard.network;

import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.function.Consumer;

public class StompClient {

    private final String serverUrl;
    private final String roomCode;
    private final String username; // NEW: Store the username
    private final Consumer<String> onDataReceived;
    private final Runnable onSuccess;
    private final Consumer<Exception> onFailure;

    private StompSession stompSession;

    // UPDATED: Constructor now requires username
    public StompClient(String serverUrl, String roomCode, String username, Consumer<String> onDataReceived, Runnable onSuccess, Consumer<Exception> onFailure) {
        this.serverUrl = serverUrl;
        this.roomCode = roomCode;
        this.username = username;
        this.onDataReceived = onDataReceived;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    public void connect() {
        try {
            WebSocketClient client = new StandardWebSocketClient();
            WebSocketStompClient stompClient = new WebSocketStompClient(client);
            stompClient.setMessageConverter(new StringMessageConverter());

            stompClient.connectAsync(serverUrl, new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    System.out.println("STOMP client connected to " + serverUrl);
                    stompSession = session;

                    // Subscribe to the room
                    session.subscribe("/topic/board/" + roomCode, this);
                    System.out.println("Subscribed to /topic/board/" + roomCode);

                    // THE FIX: Send the IDENTIFY handshake immediately to the cloud server
                    session.send("/app/board/" + roomCode, "IDENTIFY:" + username);

                    onSuccess.run();
                }

                // THE FIX: Explicitly tell Spring to convert the incoming bytes into a String
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    // Because we added getPayloadType(), this payload is now safely a String
                    if (payload instanceof String) {
                        onDataReceived.accept((String) payload);
                    }
                }

                @Override
                public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                    System.err.println("STOMP client error: " + exception.getMessage());
                    exception.printStackTrace();
                    onFailure.accept(new Exception("Error during STOMP communication", exception));
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
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
            stompSession.send("/app/board/" + roomCode, data);
        }
    }

    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        System.out.println("STOMP client disconnected.");
    }
}