package com.example.collabboard.service;

import com.example.collabboard.network.Client;
import com.example.collabboard.network.Host;
import com.example.collabboard.network.StompClient;
import javafx.application.Platform;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class CollaborationService {

    private enum CommunicationMode {
        NONE, LAN, CLOUD
    }

    private CommunicationMode currentMode = CommunicationMode.NONE;
    private Host lanHost;
    private Client lanClient;
    private StompClient cloudClient;
    private Consumer<String> onDataReceived;
    private String currentRoomIdentifier;
    
    // NEW: Add the SessionManager dependency
    private final SessionManager sessionManager;

    // NEW: Inject SessionManager via constructor
    public CollaborationService(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public boolean isHost() {
        return currentMode == CommunicationMode.LAN && lanHost != null;
    }

    public void startHost(int port) throws IOException {
        stop(); 
        currentMode = CommunicationMode.LAN;
        lanHost = new Host(port, this::receiveData);
        new Thread(lanHost).start();
        System.out.println("LAN Host started on port " + port);
    }

    // UPDATED: Now fetches the username and passes it to the Client
    public void connectToHost(String ipAddress, int port, Runnable onSuccess, Consumer<Exception> onFailure) {
        stop();
        currentMode = CommunicationMode.LAN;
        
        // Grab the username of the person currently logged into this app
        String username = sessionManager.getCurrentUser().getUsername();
        
        // Pass the username to the new Client
        lanClient = new Client(ipAddress, port, username, this::receiveData, onSuccess, onFailure);
        
        new Thread(lanClient).start();
        System.out.println("Attempting to connect to LAN host at " + ipAddress + ":" + port);
    }

    public void createCloudRoom(Consumer<String> onSuccess, Consumer<Exception> onFailure) {
        String roomCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        joinCloudRoom(roomCode, () -> onSuccess.accept(roomCode), onFailure);
    }

    public void joinCloudRoom(String roomCode, Runnable onSuccess, Consumer<Exception> onFailure) {
        stop();
        currentMode = CommunicationMode.CLOUD;
                // Comment out the Render link for a minute
        String serverUrl = "wss://collabboard-backend2.onrender.com/ws"; 

        // Use the local WS (WebSocket) link. Notice it is 'ws://' not 'wss://' because there is no SSL locally.
        //String serverUrl = "ws://localhost:8080/ws";
        
        // THE FIX: Grab the username
        String username = sessionManager.getCurrentUser().getUsername();
        
        // Pass the username into the updated StompClient constructor
        cloudClient = new StompClient(serverUrl, roomCode, username, this::receiveData, onSuccess, onFailure);
        cloudClient.connect();
    }

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
        System.out.println("[SERVICE RECEIVER] Data has arrived: " + data); 
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