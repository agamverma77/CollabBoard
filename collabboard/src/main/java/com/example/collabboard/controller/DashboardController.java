package com.example.collabboard.controller;

import com.example.collabboard.config.FxmlView;
import com.example.collabboard.model.User;
import com.example.collabboard.service.CollaborationService;
import com.example.collabboard.service.StageManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Component
public class DashboardController {

    // --- Dependencies ---
    @Lazy // Lazy loading helps prevent circular dependency issues during startup
    @Autowired
    private StageManager stageManager;

    @Autowired
    private CollaborationService collaborationService;

    // --- State ---
    private User loggedInUser;

    // --- FXML Fields from the new DashboardView.fxml ---
    @FXML private Label welcomeLabel;
    @FXML private TextField ipAddressField;       // For LAN
    @FXML private TextField cloudRoomCodeField;   // For Cloud
    @FXML private Label messageLabel;

    /**
     * Called from the LoginController to pass the current user's data.
     */
    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        if (loggedInUser != null) {
            welcomeLabel.setText("Welcome, " + loggedInUser.getUsername() + "!");
        }
    }

    // --- LAN Methods ---

    @FXML
    void handleCreateLanRoom(ActionEvent event) {
        try {
            collaborationService.startHost(12345);
            String roomCode = InetAddress.getLocalHost().getHostAddress();
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("LAN Room created! Share this IP: " + roomCode);
            navigateToWhiteboard(roomCode);
        } catch (Exception e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Failed to create LAN room: " + e.getMessage());
        }
    }

    @FXML
    void handleJoinLanRoom(ActionEvent event) {
        String ipAddress = ipAddressField.getText();
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            messageLabel.setText("Please enter the Host's IP Address.");
            return;
        }
        messageLabel.setText("Connecting to " + ipAddress + "...");
        // Use the callback-based method to handle connection success or failure
        collaborationService.connectToHost(ipAddress.trim(), 12345,
            () -> Platform.runLater(() -> navigateToWhiteboard(ipAddress.trim())),
            (ex) -> Platform.runLater(() -> {
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText("Failed to connect: " + ex.getMessage());
            })
        );
    }

    // --- Cloud Methods ---

    @FXML
    void handleCreateCloudRoom(ActionEvent event) {
        messageLabel.setText("Creating Cloud Room...");
        collaborationService.createCloudRoom(
            // On Success, we receive the generated room code
            (roomCode) -> Platform.runLater(() -> {
                messageLabel.setStyle("-fx-text-fill: green;");
                messageLabel.setText("Cloud Room created! Share this Code: " + roomCode);
                navigateToWhiteboard(roomCode);
            }),
            // On Failure
            (ex) -> Platform.runLater(() -> {
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText("Failed to create Cloud Room: " + ex.getMessage());
            })
        );
    }

    @FXML
    void handleJoinCloudRoom(ActionEvent event) {
        String roomCode = cloudRoomCodeField.getText();
        if (roomCode == null || roomCode.trim().isEmpty()) {
            messageLabel.setText("Please enter a Cloud Room Code.");
            return;
        }
        messageLabel.setText("Joining Cloud Room " + roomCode + "...");
        collaborationService.joinCloudRoom(roomCode.trim(),
            // On Success
            () -> Platform.runLater(() -> navigateToWhiteboard(roomCode.trim())),
            // On Failure
            (ex) -> Platform.runLater(() -> {
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText("Failed to join: " + ex.getMessage());
            })
        );
    }

    // --- Common and Logout Methods ---

    /**
     * A helper method to avoid duplicating navigation code.
     */
    private void navigateToWhiteboard(String roomCode) {
        // Use the StageManager to switch scenes and get the new controller
        WhiteboardController wc = stageManager.switchScene(FxmlView.WHITEBOARD);
        // Pass the room identifier to the new controller
        wc.initData(roomCode);
    }

    @FXML
    void handleLogoutButtonAction(ActionEvent event) {
        loggedInUser = null;
        collaborationService.stop(); // Stop any active LAN or Cloud connection
        stageManager.switchScene(FxmlView.LOGIN); // Use StageManager to go back to login
    }
}

