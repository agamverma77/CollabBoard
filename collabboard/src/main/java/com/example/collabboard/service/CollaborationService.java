package com.example.collabboard.service;

import com.example.collabboard.network.Client;
import com.example.collabboard.network.Host;
import com.example.collabboard.network.StompClient; // You will create this class next
import javafx.application.Platform;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class CollaborationService {

    // Enum to track which mode we are in
    private enum CommunicationMode {
        NONE,
        LAN,
        CLOUD
    }

    // --- State Variables ---
    private CommunicationMode currentMode = CommunicationMode.NONE;
    private Host lanHost;
    private Client lanClient;
    private StompClient cloudClient;
    private Consumer<String> onDataReceived;
    private String currentRoomIdentifier;

    // --- Public API for Controllers ---

    public boolean isHost() {
        return currentMode == CommunicationMode.LAN && lanHost != null;
    }

    // --- LAN Methods ---

    public void startHost(int port) throws IOException {
        stop(); // Ensure any previous session is closed
        currentMode = CommunicationMode.LAN;
        lanHost = new Host(port, this::receiveData);
        new Thread(lanHost).start();
        System.out.println("LAN Host started on port " + port);
    }

    public void connectToHost(String ipAddress, int port, Runnable onSuccess, Consumer<Exception> onFailure) {
        stop();
        currentMode = CommunicationMode.LAN;
        lanClient = new Client(ipAddress, port, this::receiveData, onSuccess, onFailure);
        new Thread(lanClient).start();
        System.out.println("Attempting to connect to LAN host at " + ipAddress + ":" + port);
    }

    // --- Cloud Methods ---

    public void createCloudRoom(Consumer<String> onSuccess, Consumer<Exception> onFailure) {
        // Generate a simple, unique room code
        String roomCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        joinCloudRoom(roomCode, () -> onSuccess.accept(roomCode), onFailure);
    }

    public void joinCloudRoom(String roomCode, Runnable onSuccess, Consumer<Exception> onFailure) {
        stop();
        currentMode = CommunicationMode.CLOUD;
        // For local testing, the server is at localhost:8080. When deployed, this URL will change.
        String serverUrl = "wss://collabboard-backend2.onrender.com/ws";
        cloudClient = new StompClient(serverUrl, roomCode, this::receiveData, onSuccess, onFailure);
        cloudClient.connect();
    }

    // --- Common Methods ---

    public void send(String data) {
        if (currentMode == CommunicationMode.LAN) {
            if (lanHost != null) {
                lanHost.broadcast(data);
            } else if (lanClient != null) {
                lanClient.sendMessage(data);
            }
        } else if (currentMode == CommunicationMode.CLOUD) {
            if (cloudClient != null) {
                cloudClient.sendMessage(data);
            }
        }
    }

    public void stop() {
        if (lanHost != null) {
            lanHost.shutdown();
            lanHost = null;
        }
        if (lanClient != null) {
            lanClient.shutdown();
            lanClient = null;
        }
        if (cloudClient != null) {
            cloudClient.disconnect();
            cloudClient = null;
        }
        currentMode = CommunicationMode.NONE;
        currentRoomIdentifier = null;
        System.out.println("Collaboration service stopped.");
    }

    private void receiveData(String data) {
        if (onDataReceived != null) {
            Platform.runLater(() -> onDataReceived.accept(data));
        }
    }

    public void setOnDataReceived(Consumer<String> listener) {
        this.onDataReceived = listener;
    }

    public String getCurrentRoomIdentifier() {
        return this.currentRoomIdentifier;
    }

    public void setCurrentRoomIdentifier(String identifier) {
        this.currentRoomIdentifier = identifier;
    }
}

