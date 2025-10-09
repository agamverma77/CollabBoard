package com.example.collabboard.controller;

import com.example.collabboard.service.CollaborationService;
import com.example.collabboard.service.SessionManager;
import com.example.collabboard.util.SceneManager;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import javafx.scene.control.Tooltip;

@Component
public class WhiteboardController {

    private enum Tool { SELECTION, HAND, PEN, TEXT, ERASER, RECTANGLE, OVAL, STICKY_NOTE, SHAPE  }

    // Use Stacks for efficient push/pop operations for undo/redo
    private final Stack<String> drawingHistory = new Stack<>();
    private final Stack<String> redoHistory = new Stack<>();

    private GraphicsContext graphicsContext;
    private Tool currentTool = Tool.PEN;
    private double startX, startY;
    private double lastX, lastY;
    private double currentZoom = 1.0;
    private Scale scaleTransform;
    private boolean isBoardLocked = false;

    // Canvas and core elements
    @FXML private AnchorPane canvasPane;
    @FXML private Canvas canvas;
    @FXML private ColorPicker colorPicker;
    @FXML private Label roomCodeLabel;
    @FXML private ListView<String> chatListView;
    @FXML private TextField chatTextField;
    @FXML private Button sendChatButton;


    // Tool group and individual tools
    @FXML private ToggleGroup toolGroup;
    @FXML private ToggleButton selectTool;
    @FXML private ToggleButton handTool;
    @FXML private ToggleButton penTool;
    @FXML private ToggleButton textTool;
    @FXML private ToggleButton noteTool;
    @FXML private ToggleButton shapeTool;
    @FXML private ToggleButton eraserTool;
    @FXML private ToggleButton lockBoardButton;


    // Action buttons
    @FXML private Button clearBtn;
    @FXML private Button undoBtn;
    @FXML private Button redoBtn;
    @FXML private Button saveBtn;
    @FXML private Button loadBtn;
    @FXML private Button exportBtn;

    // Header elements
    @FXML private HBox collaboratorsBox;
    @FXML private Label collaboratorCountLabel;
    @FXML private Button addCollaboratorBtn;
    @FXML private Button shareButton;
    @FXML private Button settingsButton;

    // Zoom controls
    @FXML private Label zoomLabel;
    @FXML private Slider zoomSlider;

    // Floating buttons
    @FXML private Button fabBtn;
    @FXML private Button imageUploadBtn;
    @FXML private Button layersBtn;

    // Left sidebar - Templates
    @FXML private VBox templatesContainer;
    @FXML private Button closeTemplatesBtn;
    @FXML private Button brainstormingTemplate;
    @FXML private Button mindMapTemplate;
    @FXML private Button kanbanTemplate;
    @FXML private Button flowchartTemplate;
    @FXML private Button userJourneyTemplate;
    @FXML private Button timelineTemplate;

    // Left sidebar - Activity
    @FXML private VBox activityList;
    @FXML private Button closeActivityBtn;

    // Right sidebar - Participants
    @FXML private VBox participantsList;
    @FXML private Button closeParticipantsBtn;
    @FXML private Button inviteOthersBtn;

    // Right sidebar - Version History
    @FXML private VBox versionHistoryList;
    @FXML private Button closeVersionBtn;

    // Bottom
    @FXML private Button exitRoomBtn;

    private final CollaborationService collaborationService;
    private final SessionManager sessionManager;
    private final ApplicationContext applicationContext;

    public WhiteboardController(CollaborationService collaborationService, SessionManager sessionManager, ApplicationContext applicationContext) {
        this.collaborationService = collaborationService;
        this.sessionManager = sessionManager;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        // Bind the canvas size to its parent AnchorPane to make it resizable
        AnchorPane parentPane = (AnchorPane) canvas.getParent();
        canvas.widthProperty().bind(parentPane.widthProperty());
        canvas.heightProperty().bind(parentPane.heightProperty());

        graphicsContext = canvas.getGraphicsContext2D();
        colorPicker.setValue(Color.BLACK);

        // Initialize zoom functionality
        setupZoomControls();

        // Setup tool group selection
        setupToolGroup();

        // Initialize room info for all
        String roomIdentifier = collaborationService.getCurrentRoomIdentifier();
        if (roomIdentifier != null) {
            String labelPrefix = collaborationService.isHost() ? "Room: " : "Room: ";
            roomCodeLabel.setText(labelPrefix + roomIdentifier);
        }

        // Initialize collaboration and UI
        collaborationService.setOnDataReceived(this::parseData);
        setupCanvasEventHandlers();
        initializeUIComponents();

        if (collaborationService.isHost()) {
            lockBoardButton.setVisible(true); // Only the host can see the lock button
            updateParticipantsUI(new String[]{sessionManager.getCurrentUser().getUsername()});
        }

        // --- NEW: Add Tooltips Programmatically ---
        Tooltip.install(penTool, new Tooltip("Pen"));
        Tooltip.install(eraserTool, new Tooltip("Eraser"));
        Tooltip.install(undoBtn, new Tooltip("Undo"));
        Tooltip.install(redoBtn, new Tooltip("Redo"));
    }

    // --- FXML ACTION HANDLERS ---

    // Tool Selection Methods
    @FXML private void selectSelectionTool() { currentTool = Tool.SELECTION; }
    @FXML private void selectHandTool() { currentTool = Tool.HAND; }
    @FXML private void selectPenTool() { currentTool = Tool.PEN; }
    @FXML private void selectTextTool() { currentTool = Tool.TEXT; }
    @FXML private void selectEraserTool() { currentTool = Tool.ERASER; }
    @FXML private void selectRectangleTool() { currentTool = Tool.RECTANGLE; }
    @FXML private void selectOvalTool() { currentTool = Tool.OVAL; }
    @FXML private void selectStickyNoteTool() { currentTool = Tool.STICKY_NOTE; }

    @FXML private void handleClearCanvas(ActionEvent event) { collaborationService.send("CLEAR"); }

    // Template Methods
    @FXML private void handleBrainstormingTemplate(ActionEvent event) {
        // TODO: Load brainstorming template
        System.out.println("Loading brainstorming template...");
    }

    @FXML private void handleMindMapTemplate(ActionEvent event) {
        // TODO: Load mind map template
        System.out.println("Loading mind map template...");
    }

    @FXML private void handleKanbanTemplate(ActionEvent event) {
        // TODO: Load kanban template
        System.out.println("Loading kanban template...");
    }

    @FXML private void handleFlowchartTemplate(ActionEvent event) {
        // TODO: Load flowchart template
        System.out.println("Loading flowchart template...");
    }

    @FXML private void handleUserJourneyTemplate(ActionEvent event) {
        // TODO: Load user journey template
        System.out.println("Loading user journey template...");
    }

    @FXML private void handleTimelineTemplate(ActionEvent event) {
        // TODO: Load timeline template
        System.out.println("Loading timeline template...");
    }

    // Collaboration Methods
    @FXML private void handleAddCollaborator(ActionEvent event) {
        // TODO: Show add collaborator dialog
        System.out.println("Adding collaborator...");
    }

    @FXML private void handleShare(ActionEvent event) {
        // TODO: Show share dialog
        System.out.println("Sharing room...");
    }

    @FXML private void handleSettings(ActionEvent event) {
        // TODO: Show settings dialog
        System.out.println("Opening settings...");
    }

    @FXML private void handleInviteOthers(ActionEvent event) {
        // TODO: Show invite dialog
        System.out.println("Inviting others...");
    }

    // Floating Action Button Methods
    @FXML private void handleFAB(ActionEvent event) {
        // TODO: Show quick action menu
        System.out.println("FAB clicked - showing quick actions...");
    }

    @FXML private void handleImageUpload(ActionEvent event) {
        // TODO: Show image upload dialog
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());
        if (file != null) {
            System.out.println("Selected image: " + file.getName());
            // TODO: Implement image loading and positioning on canvas
        }
    }

    @FXML private void handleLayers(ActionEvent event) {
        // TODO: Show layers panel
        System.out.println("Opening layers panel...");
    }

    // Close Button Methods
    @FXML private void handleCloseTemplates(ActionEvent event) {
        templatesContainer.setVisible(false);
    }

    @FXML private void handleCloseActivity(ActionEvent event) {
        activityList.getParent().setVisible(false);
    }

    @FXML private void handleCloseParticipants(ActionEvent event) {
        participantsList.getParent().setVisible(false);
    }

    @FXML private void handleCloseVersion(ActionEvent event) {
        versionHistoryList.getParent().setVisible(false);
    }

    @FXML
    private void handleExitRoom(ActionEvent event) throws IOException {
        collaborationService.stop();
        SceneManager.switchScene(event, "DashboardView.fxml", "CollabBoard - Dashboard", applicationContext);
    }

    @FXML
    void handleSendChatMessage(ActionEvent event) {
        String message = chatTextField.getText();
        if (message == null || message.trim().isEmpty()) return;
        String username = sessionManager.getCurrentUser().getUsername();
        String data = String.format("CHAT:%s: %s", username, message);
        collaborationService.send(data);
        chatTextField.clear();
    }

    // Zoom Control Methods
    @FXML private void handleZoomChange() {
        if (zoomSlider != null) {
            double zoomValue = zoomSlider.getValue();
            currentZoom = zoomValue / 100.0;
            updateZoomLabel();
            applyZoomToCanvas();
        }
    }

    private void updateZoomLabel() {
        if (zoomLabel != null) {
            zoomLabel.setText(String.format("Zoom: %.0f%%", currentZoom * 100));
        }
    }

    private void applyZoomToCanvas() {
        if (canvas != null) {
            if (scaleTransform == null) {
                scaleTransform = new Scale();
                canvas.getTransforms().add(scaleTransform);
            }
            scaleTransform.setX(currentZoom);
            scaleTransform.setY(currentZoom);
        }
    }

    @FXML
    private void handleLockBoard(ActionEvent event) {
        if (lockBoardButton.isSelected()) {
            collaborationService.send("LOCK_BOARD");
            lockBoardButton.setText("🔒 Unlock");
        } else {
            collaborationService.send("UNLOCK_BOARD");
            lockBoardButton.setText("🔒 Lock");
        }
    }

    @FXML
    void handleExportAsImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                canvas.snapshot(null, writableImage);
                ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", file);
            } catch (IOException e) {
                System.err.println("Error saving image: " + e.getMessage());
            }
        }
    }

    @FXML
    void handleSaveBoard(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Board State");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CollabBoard File", "*.collab"));
        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                for (String action : drawingHistory) {
                    writer.println(action);
                }
            } catch (IOException e) {
                System.err.println("Error saving board state: " + e.getMessage());
            }
        }
    }

    @FXML
    void handleLoadBoard(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Board State");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CollabBoard File", "*.collab"));
        File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                collaborationService.send("CLEAR");
                List<String> actions = Files.readAllLines(file.toPath());
                for (String action : actions) {
                    collaborationService.send(action);
                }
            } catch (IOException e) {
                System.err.println("Error loading board state: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUndo(ActionEvent event) {
        if (!drawingHistory.isEmpty()) {
            collaborationService.send("UNDO");
        }
    }

    @FXML
    private void handleRedo(ActionEvent event) {
        if (!redoHistory.isEmpty()) {
            collaborationService.send("REDO");
        }
    }

    // --- UI SETUP HELPER METHODS ---

    private void setupZoomControls() {
        if (zoomSlider != null) {
            zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                currentZoom = newVal.doubleValue() / 100.0;
                updateZoomLabel();
                applyZoomToCanvas();
            });
            updateZoomLabel();
        }
    }

    private void setupToolGroup() {
        if (toolGroup != null && penTool != null) {
            penTool.setSelected(true);
            currentTool = Tool.PEN;
        }
    }

    private void setupCanvasEventHandlers() {
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
    }

    private void initializeUIComponents() {
        // Initialize collaborator count
        if (collaboratorCountLabel != null) {
            collaboratorCountLabel.setText("1 collaborator");
        }

        // Initialize activity list with sample data
        initializeActivityList();

        // Initialize participants list
        initializeParticipantsList();

        // Initialize version history
        initializeVersionHistory();
    }

    private void initializeActivityList() {
        if (activityList != null) {
            // Add sample activity items - in real implementation, this would come from backend
            addActivityItem("You joined the session", "Just now");
            addActivityItem("Canvas cleared", "2 min ago");
            addActivityItem("New drawing added", "5 min ago");
        }
    }

    private void initializeParticipantsList() {
        if (participantsList != null) {
            // Add current user - in real implementation, this would come from backend
            String currentUsername = sessionManager.getCurrentUser().getUsername();
            addParticipant(currentUsername, true);
        }
    }

    private void initializeVersionHistory() {
        if (versionHistoryList != null) {
            // Add sample version history - in real implementation, this would come from backend
            addVersionHistoryItem("Current Version", "Now", true);
            addVersionHistoryItem("Auto-save #3", "5 min ago", false);
            addVersionHistoryItem("Auto-save #2", "10 min ago", false);
        }
    }

    // Helper methods for dynamic UI updates
    private void addActivityItem(String activity, String time) {
        if (activityList != null) {
            Label activityLabel = new Label(activity);
            activityLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #333;");
            Label timeLabel = new Label(time);
            timeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
            VBox activityItem = new VBox(2, activityLabel, timeLabel);
            activityItem.setStyle("-fx-padding: 8; -fx-background-color: #f9f9f9; -fx-background-radius: 4;");
            activityList.getChildren().add(activityItem);
        }
    }

    private void addParticipant(String username, boolean isCurrentUser) {
        if (participantsList != null) {
            Label nameLabel = new Label(username + (isCurrentUser ? " (You)" : ""));
            nameLabel.setStyle("-fx-font-size: 13; -fx-font-weight: " + (isCurrentUser ? "bold" : "normal") + ";");
            Label statusLabel = new Label("Online");
            statusLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #4caf50;");
            VBox participantItem = new VBox(2, nameLabel, statusLabel);
            participantItem.setStyle("-fx-padding: 8;");
            participantsList.getChildren().add(participantItem);
        }
    }

    private void addVersionHistoryItem(String version, String time, boolean isCurrent) {
        if (versionHistoryList != null) {
            Label versionLabel = new Label(version);
            versionLabel.setStyle("-fx-font-size: 12; -fx-font-weight: " + (isCurrent ? "bold" : "normal") + ";");
            Label timeLabel = new Label(time);
            timeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
            VBox versionItem = new VBox(2, versionLabel, timeLabel);
            String bgColor = isCurrent ? "#e3f2fd" : "#f9f9f9";
            versionItem.setStyle("-fx-padding: 8; -fx-background-color: " + bgColor + "; -fx-background-radius: 4; -fx-cursor: hand;");
            versionHistoryList.getChildren().add(versionItem);
        }
    }

    // --- MOUSE AND DATA PROCESSING ---

    private void handleMousePressed(MouseEvent event) {
        startX = event.getX();
        startY = event.getY();
        lastX = startX; // Initialize lastX/Y here
        lastY = startY;
        if (isBoardLocked && !collaborationService.isHost()) return; // PREVENT ACTION IF LOCKED
        if (currentTool == Tool.PEN) {
            graphicsContext.setStroke(colorPicker.getValue());
            graphicsContext.setLineWidth(2.0);
            graphicsContext.beginPath();
            graphicsContext.moveTo(startX, startY);
            graphicsContext.stroke();
        } else if (currentTool == Tool.STICKY_NOTE) {
            createTemporaryTextArea(startX, startY);
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (currentTool != Tool.PEN && currentTool != Tool.ERASER) return;
        double x = event.getX();
        double y = event.getY();
        String data = null;
        if (isBoardLocked && !collaborationService.isHost()) return; // PREVENT ACTION IF LOCKED
        if (currentTool == Tool.PEN) {
            graphicsContext.lineTo(x, y);
            graphicsContext.stroke();
            data = String.format("DRAW:%.2f,%.2f,%.2f,%.2f,%s", lastX, lastY, x, y, colorPicker.getValue().toString());

        } else if (currentTool == Tool.ERASER) {
            double eraserSize = 15.0;
            eraseData(String.format("%.2f,%.2f,%.2f", x, y, eraserSize));
            data = String.format("ERASE:%.2f,%.2f,%.2f", x, y, eraserSize);
        }
        if (data != null) {
            collaborationService.send(data);
        }
        lastX = x;
        lastY = y;
    }

    private void handleMouseReleased(MouseEvent event) {
        if (currentTool != Tool.RECTANGLE && currentTool != Tool.OVAL) return;
        double endX = event.getX();
        double endY = event.getY();
        Color color = colorPicker.getValue();
        String data = null;
        double x = Math.min(startX, endX);
        double y = Math.min(startY, endY);
        double width = Math.abs(startX - endX);
        double height = Math.abs(startY - endY);
        if (isBoardLocked && !collaborationService.isHost()) return; // PREVENT ACTION IF LOCKED
        if (currentTool == Tool.RECTANGLE) {
            data = String.format("RECTANGLE:%.2f,%.2f,%.2f,%.2f,%s", x, y, width, height, color.toString());
        } else if (currentTool == Tool.OVAL) {
            data = String.format("OVAL:%.2f,%.2f,%.2f,%.2f,%s", x, y, width, height, color.toString());
        }
        if (data != null) {
            collaborationService.send(data);
        }
    }

    private void parseData(String data) {
        // When a new drawing action occurs, clear the redo history
        if (data.startsWith("DRAW:") || data.startsWith("ERASE:") || data.startsWith("RECTANGLE:") || data.startsWith("OVAL:") || data.startsWith("STICKY_NOTE:")) {
            drawingHistory.push(data);
            redoHistory.clear();
        } else if (data.equals("CLEAR")) {
            drawingHistory.clear();
            redoHistory.clear();
        }

        Platform.runLater(() -> {
            try {
                if (data.equals("CLEAR")) {
                    graphicsContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    return;
                }
                if ("UNDO".equals(data)) {
                    if (!drawingHistory.isEmpty()) {
                        redoHistory.push(drawingHistory.pop());
                        redrawCanvas();
                    }
                    return;
                }
                if ("REDO".equals(data)) {
                    if (!redoHistory.isEmpty()) {
                        drawingHistory.push(redoHistory.pop());
                        redrawCanvas();
                    }
                    return;
                }
                // --- NEW: Handle Session Management Commands ---
                if ("LOCK_BOARD".equals(data)) {
                    isBoardLocked = true;
                    if (lockBoardButton.isVisible()) { // If host, update button state
                        lockBoardButton.setSelected(true);
                        lockBoardButton.setText("🔒 Unlock");
                    }
                    return;
                }
                if ("UNLOCK_BOARD".equals(data)) {
                    isBoardLocked = false;
                    if (lockBoardButton.isVisible()) {
                        lockBoardButton.setSelected(false);
                        lockBoardButton.setText("🔒 Lock");
                    }
                    return;
                }
                if (data.startsWith("USER_LIST:")) {
                    String usersData = data.length() > 10 ? data.substring(10) : "";
                    updateParticipantsUI(usersData.split(","));
                    return;
                }
                if ("YOU_WERE_KICKED".equals(data)) {
                    collaborationService.stop();
                    Alert alert = new Alert(Alert.AlertType.WARNING, "You have been removed from the session by the host.");
                    alert.setHeaderText("Session Ended");
                    alert.showAndWait();
                    SceneManager.switchScene(new ActionEvent(canvas, null), "DashboardView.fxml", "CollabBoard", applicationContext);
                    return;
                }

                String[] parts = data.split(":", 2);
                String command = parts[0];
                String content = parts[1];
                switch (command) {
                    case "DRAW": drawData(content); break;
                    case "ERASE": eraseData(content); break;
                    case "RECTANGLE": drawRectangle(content); break;
                    case "OVAL": drawOval(content); break;
                    case "STICKY_NOTE": drawStickyNote(content); break;
                    case "CHAT":
                        chatListView.getItems().add(content);
                        chatListView.scrollTo(chatListView.getItems().size() - 1);
                        break;
                }
            } catch (Exception e) {
                System.err.println("Could not parse incoming data: " + data);
            }
        });
    }

    // --- HELPER METHODS ---

    private void updateParticipantsUI(String[] usernames) {
        participantsList.getChildren().clear();
        String currentUsername = sessionManager.getCurrentUser().getUsername();

        for (String username : usernames) {
            if (username.isEmpty()) continue;

            HBox userEntry = new HBox(10);
            userEntry.setAlignment(Pos.CENTER_LEFT);
            userEntry.setStyle("-fx-padding: 5;");

            Label nameLabel = new Label(username + (username.equals(currentUsername) ? " (You)" : ""));
            nameLabel.setStyle("-fx-font-weight: bold;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            userEntry.getChildren().addAll(nameLabel, spacer);

            // If we are the host, add a "Kick" button for other users
            if (collaborationService.isHost() && !username.equals(currentUsername)) {
                Button kickButton = new Button("Kick");
                kickButton.setStyle("-fx-background-color: #ffcdd2; -fx-text-fill: #c62828; -fx-font-size: 10; -fx-cursor: hand;");
                kickButton.setOnAction(e -> {
                    // Send a command to the host's own service to kick the user
                    collaborationService.kickUser(username);
                });
                userEntry.getChildren().add(kickButton);
            }

            participantsList.getChildren().add(userEntry);
        }
    }


    private void redrawCanvas() {
        graphicsContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        // Iterate through a copy to draw from oldest to newest
        for (String action : new ArrayList<>(drawingHistory)) {
            String[] parts = action.split(":", 2);
            String command = parts[0];
            String content = parts[1];
            switch (command) {
                case "DRAW": drawData(content); break;
                case "ERASE": eraseData(content); break;
                case "RECTANGLE": drawRectangle(content); break;
                case "OVAL": drawOval(content); break;
                case "STICKY_NOTE": drawStickyNote(content); break;
            }
        }
    }

    private void createTemporaryTextArea(double x, double y) {
        TextArea textArea = new TextArea();
        textArea.setLayoutX(x);
        textArea.setLayoutY(y);
        textArea.setPrefSize(150, 100);
        textArea.setStyle("-fx-font-size: 14px; -fx-background-color: #FFFFE0;");
        textArea.setWrapText(true);
        textArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                String text = textArea.getText().replace(":", "").replace(",", "");
                String data = String.format("STICKY_NOTE:%.2f,%.2f,%s", x, y, text);
                collaborationService.send(data);
                canvasPane.getChildren().remove(textArea);
                event.consume();
            }
        });
        canvasPane.getChildren().add(textArea);
        textArea.requestFocus();
    }

    private void drawData(String content) {
        try {
            String[] params = content.split(",");
            double startX = Double.parseDouble(params[0]);
            double startY = Double.parseDouble(params[1]);
            double endX = Double.parseDouble(params[2]);
            double endY = Double.parseDouble(params[3]);
            Color color = Color.valueOf(params[4]);
            graphicsContext.setStroke(color);
            graphicsContext.setLineWidth(2.0);
            graphicsContext.beginPath();
            graphicsContext.moveTo(startX, startY);
            graphicsContext.lineTo(endX, endY);
            graphicsContext.stroke();
        } catch (Exception e) {
            System.err.println("Error drawing data: " + content);
        }
    }

    private void eraseData(String content) {
        try {
            String[] params = content.split(",");
            double x = Double.parseDouble(params[0]);
            double y = Double.parseDouble(params[1]);
            double size = Double.parseDouble(params[2]);
            graphicsContext.clearRect(x - size / 2, y - size / 2, size, size);
        } catch (Exception e) {
            System.err.println("Error erasing data: " + content);
        }
    }

    private void drawRectangle(String content) {
        try {
            String[] params = content.split(",");
            double x = Double.parseDouble(params[0]);
            double y = Double.parseDouble(params[1]);
            double width = Double.parseDouble(params[2]);
            double height = Double.parseDouble(params[3]);
            Color color = Color.valueOf(params[4]);
            graphicsContext.setStroke(color);
            graphicsContext.setLineWidth(2.0);
            graphicsContext.strokeRect(x, y, width, height);
        } catch (Exception e) {
            System.err.println("Error drawing rectangle: " + content);
        }
    }

    private void drawOval(String content) {
        try {
            String[] params = content.split(",");
            double x = Double.parseDouble(params[0]);
            double y = Double.parseDouble(params[1]);
            double width = Double.parseDouble(params[2]);
            double height = Double.parseDouble(params[3]);
            Color color = Color.valueOf(params[4]);
            graphicsContext.setStroke(color);
            graphicsContext.setLineWidth(2.0);
            graphicsContext.strokeOval(x, y, width, height);
        } catch (Exception e) {
            System.err.println("Error drawing oval: " + content);
        }
    }

    private void drawStickyNote(String content) {
        try {
            String[] params = content.split(",", 3);
            double x = Double.parseDouble(params[0]);
            double y = Double.parseDouble(params[1]);
            String text = params[2];
            double width = 150;
            double height = 100;
            graphicsContext.setFill(Color.web("#FFFFE0"));
            graphicsContext.setStroke(Color.DARKGRAY);
            graphicsContext.setLineWidth(1.0);
            graphicsContext.fillRect(x, y, width, height);
            graphicsContext.strokeRect(x, y, width, height);
            graphicsContext.setFill(Color.BLACK);
            graphicsContext.setFont(new Font("System", 14));
            String[] lines = text.split("\n");
            for(int i = 0; i < lines.length; i++) {
                if (i < 5) {
                    graphicsContext.fillText(lines[i], x + 5, y + 20 + (i * 18));
                }
            }
        } catch (Exception e) {
            System.err.println("Error drawing sticky note: " + content);
        }
    }
}