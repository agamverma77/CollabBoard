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

/**
 * A client that connects to a STOMP-based WebSocket server (like the one in Spring).
 * It handles connecting, subscribing to a topic, and sending messages.
 */
public class StompClient {

    private final String serverUrl;
    private final String roomCode;
    private final Consumer<String> onDataReceived;
    private final Runnable onSuccess;
    private final Consumer<Exception> onFailure;

    private StompSession stompSession;

    public StompClient(String serverUrl, String roomCode, Consumer<String> onDataReceived, Runnable onSuccess, Consumer<Exception> onFailure) {
        this.serverUrl = serverUrl;
        this.roomCode = roomCode;
        this.onDataReceived = onDataReceived;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    /**
     * Initiates the connection to the WebSocket server.
     */
    public void connect() {
        try {
            WebSocketClient client = new StandardWebSocketClient();
            WebSocketStompClient stompClient = new WebSocketStompClient(client);
            stompClient.setMessageConverter(new StringMessageConverter());

            // The StompSessionHandler handles all connection events.
            stompClient.connect(serverUrl, new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    System.out.println("STOMP client connected to " + serverUrl);
                    stompSession = session;

                    // Once connected, subscribe to the room's topic to receive messages.
                    session.subscribe("/topic/board/" + roomCode, this);
                    System.out.println("Subscribed to /topic/board/" + roomCode);

                    // Trigger the success callback to notify the UI.
                    onSuccess.run();
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    // This method is called whenever a message is received on the subscribed topic.
                    onDataReceived.accept((String) payload);
                }

                @Override
                public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                    System.err.println("STOMP client error: " + exception.getMessage());
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

    /**
     * Sends a message to the server for the current room.
     * @param data The message payload (e.g., drawing or chat data).
     */
    public void sendMessage(String data) {
        if (stompSession != null && stompSession.isConnected()) {
            // Sends the message to the destination that the @MessageMapping in the server controller is listening to.
            stompSession.send("/app/board/" + roomCode, data);
        }
    }

    /**
     * Disconnects from the WebSocket server.
     */
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        System.out.println("STOMP client disconnected.");
    }
}
