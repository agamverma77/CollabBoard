package com.example.collabboard.controller;

import com.example.collabboard.service.CollaborationService;
import com.example.collabboard.service.ScreenCaptureService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for managing screen sharing functionality.
 * Handles UI interactions for starting/stopping screen sharing,
 * displaying shared screens, and managing participants.
 */
@Component
public class ScreenSharingController {
    
    @Autowired
    private ScreenCaptureService screenCaptureService;
    
    @Autowired
    private CollaborationService collaborationService;
    
    // UI Components
    @FXML private Button startSharingButton;
    @FXML private Button stopSharingButton;
    @FXML private Button selectAreaButton;
    @FXML private ComboBox<String> qualityComboBox;
    @FXML private ComboBox<String> fpsComboBox;
    @FXML private VBox sharedScreensContainer;
    @FXML private Label statusLabel;
    @FXML private ScrollPane screensScrollPane;
    
    // State management
    private boolean isSharing = false;
    private String currentUserId;
    private Map<String, ImageView> participantScreens = new ConcurrentHashMap<>();
    private Map<String, String> participantNames = new ConcurrentHashMap<>();
    
    @FXML
    public void initialize() {
        setupUI();
        setupEventHandlers();
        setupCollaborationService();
    }
    
    private void setupUI() {
        // Initialize combo boxes
        qualityComboBox.getItems().addAll("Low (480p)", "Medium (720p)", "High (1080p)", "Ultra (4K)");
        qualityComboBox.setValue("Medium (720p)");
        
        fpsComboBox.getItems().addAll("5 FPS", "10 FPS", "15 FPS", "30 FPS");
        fpsComboBox.setValue("10 FPS");
        
        // Initial button states
        updateButtonStates();
        
        // Setup status label
        statusLabel.setText("Ready to share screen");
    }
    
    private void setupEventHandlers() {
        startSharingButton.setOnAction(e -> startScreenSharing());
        stopSharingButton.setOnAction(e -> stopScreenSharing());
        selectAreaButton.setOnAction(e -> selectScreenArea());
        
        qualityComboBox.setOnAction(e -> updateCaptureSettings());
        fpsComboBox.setOnAction(e -> updateCaptureSettings());
    }
    
    private void setupCollaborationService() {
        collaborationService.setOnDataReceived(this::handleIncomingData);
    }
    
    /**
     * Start sharing the user's screen.
     */
    @FXML
    private void startScreenSharing() {
        if (isSharing) {
            return;
        }
        
        try {
            // Get capture settings
            int intervalMs = getFpsInterval();
            
            // Start capturing with callback
            screenCaptureService.startCapturing(intervalMs, this::sendScreenshot);
            
            isSharing = true;
            updateButtonStates();
            statusLabel.setText("Sharing screen...");
            
            // Notify other participants
            sendScreenSharingStatus(true);
            
            System.out.println("Screen sharing started");
            
        } catch (Exception e) {
            showError("Failed to start screen sharing", e.getMessage());
        }
    }
    
    /**
     * Stop sharing the user's screen.
     */
    @FXML
    private void stopScreenSharing() {
        if (!isSharing) {
            return;
        }
        
        screenCaptureService.stopCapturing();
        isSharing = false;
        updateButtonStates();
        statusLabel.setText("Screen sharing stopped");
        
        // Notify other participants
        sendScreenSharingStatus(false);
        
        System.out.println("Screen sharing stopped");
    }
    
    /**
     * Select a specific area of the screen to share.
     */
    @FXML
    private void selectScreenArea() {
        // For now, reset to full screen
        // In a full implementation, you would show a screen selection dialog
        screenCaptureService.resetCaptureAreaToFullScreen();
        showInfo("Screen Area", "Screen area reset to full screen. Custom area selection coming soon!");
    }
    
    /**
     * Send screenshot data to other participants.
     */
    private void sendScreenshot(String base64Image) {
        if (!isSharing) {
            return;
        }
        
        String message = String.format("SCREEN_SHARE:%s:%s", currentUserId, base64Image);
        collaborationService.send(message);
    }
    
    /**
     * Send screen sharing status to other participants.
     */
    private void sendScreenSharingStatus(boolean sharing) {
        String message = String.format("SCREEN_SHARE_STATUS:%s:%s", currentUserId, sharing);
        collaborationService.send(message);
    }
    
    /**
     * Handle incoming data from other participants.
     */
    private void handleIncomingData(String data) {
        Platform.runLater(() -> {
            if (data.startsWith("SCREEN_SHARE:")) {
                handleScreenshotData(data);
            } else if (data.startsWith("SCREEN_SHARE_STATUS:")) {
                handleScreenSharingStatus(data);
            } else if (data.startsWith("USER_LIST:")) {
                handleUserList(data);
            }
        });
    }
    
    /**
     * Handle screenshot data from other participants.
     */
    private void handleScreenshotData(String data) {
        try {
            String[] parts = data.split(":", 3);
            if (parts.length >= 3) {
                String userId = parts[1];
                String base64Image = parts[2];
                
                Image image = ScreenCaptureService.base64ToImage(base64Image);
                if (image != null) {
                    displayParticipantScreen(userId, image);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling screenshot data: " + e.getMessage());
        }
    }
    
    /**
     * Handle screen sharing status from other participants.
     */
    private void handleScreenSharingStatus(String data) {
        try {
            String[] parts = data.split(":", 3);
            if (parts.length >= 3) {
                String userId = parts[1];
                boolean sharing = Boolean.parseBoolean(parts[2]);
                
                if (!sharing) {
                    removeParticipantScreen(userId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling screen sharing status: " + e.getMessage());
        }
    }
    
    /**
     * Handle user list updates.
     */
    private void handleUserList(String data) {
        try {
            String userList = data.substring("USER_LIST:".length());
            String[] users = userList.split(",");
            
            // Update participant names
            participantNames.clear();
            for (String user : users) {
                if (!user.isEmpty()) {
                    participantNames.put(user, user);
                }
            }
            
            // Remove screens for users no longer in the room
            participantScreens.keySet().removeIf(userId -> !participantNames.containsKey(userId));
            
        } catch (Exception e) {
            System.err.println("Error handling user list: " + e.getMessage());
        }
    }
    
    /**
     * Display a participant's shared screen.
     */
    private void displayParticipantScreen(String userId, Image image) {
        ImageView imageView = participantScreens.computeIfAbsent(userId, k -> {
            ImageView newImageView = new ImageView();
            newImageView.setFitWidth(300);
            newImageView.setFitHeight(200);
            newImageView.setPreserveRatio(true);
            newImageView.setSmooth(true);
            
            // Add to container
            sharedScreensContainer.getChildren().add(newImageView);
            
            return newImageView;
        });
        
        imageView.setImage(image);
    }
    
    /**
     * Remove a participant's shared screen.
     */
    private void removeParticipantScreen(String userId) {
        ImageView imageView = participantScreens.remove(userId);
        if (imageView != null) {
            sharedScreensContainer.getChildren().remove(imageView);
        }
    }
    
    /**
     * Update button states based on current sharing status.
     */
    private void updateButtonStates() {
        startSharingButton.setDisable(isSharing);
        stopSharingButton.setDisable(!isSharing);
        selectAreaButton.setDisable(isSharing);
        qualityComboBox.setDisable(isSharing);
        fpsComboBox.setDisable(isSharing);
    }
    
    /**
     * Update capture settings based on UI selections.
     */
    private void updateCaptureSettings() {
        if (isSharing) {
            int intervalMs = getFpsInterval();
            // Restart capture with new settings
            screenCaptureService.stopCapturing();
            screenCaptureService.startCapturing(intervalMs, this::sendScreenshot);
        }
    }
    
    /**
     * Get FPS interval in milliseconds from combo box selection.
     */
    private int getFpsInterval() {
        String fpsText = fpsComboBox.getValue();
        if (fpsText.contains("5")) return 200;
        if (fpsText.contains("10")) return 100;
        if (fpsText.contains("15")) return 67;
        if (fpsText.contains("30")) return 33;
        return 100; // Default to 10 FPS
    }
    
    /**
     * Set the current user ID for screen sharing identification.
     */
    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }
    
    /**
     * Check if currently sharing screen.
     */
    public boolean isSharing() {
        return isSharing;
    }
    
    /**
     * Clean up resources when controller is destroyed.
     */
    public void cleanup() {
        stopScreenSharing();
        screenCaptureService.cleanup();
    }
    
    /**
     * Show error dialog.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show info dialog.
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
