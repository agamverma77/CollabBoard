package com.example.collabboard.service;

import com.example.collabboard.JavaFxApplication.StageReadyEvent;
import com.example.collabboard.config.FxmlView;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
public class StageManager implements ApplicationListener<StageReadyEvent> {

    private Stage primaryStage;
    private final ApplicationContext applicationContext;

    public StageManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        this.primaryStage = event.getStage();
        primaryStage.setTitle("CollabBoard");
        
        // Configure stage to respect screen boundaries
        configureStageForScreen();
        
        switchScene(FxmlView.LOGIN); 
    }

    /**
     * @param view 
     * @param <T> 
     * @return 
     */
    public <T> T switchScene(final FxmlView view) {
        FXMLLoader fxmlLoader = loadViewNodeHierarchy(view.getFxmlFile());
        show(fxmlLoader.getRoot());
        return fxmlLoader.getController();
    }

    private void show(final Parent rootnode) {
        Scene scene = primaryStage.getScene();

        if (scene == null) {
            scene = new Scene(rootnode);
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(rootnode);
        }
        
        // Apply screen size constraints before showing
        applyScreenSizeConstraints();
        primaryStage.sizeToScene();
        primaryStage.centerOnScreen();

        try {
            primaryStage.show();
        } catch (Exception exception) {
            System.err.println("Error showing the stage: " + exception.getMessage());
        }
    }

    /**
     * @param fxmlFilePath
     * @return
     */
    private FXMLLoader loadViewNodeHierarchy(String fxmlFilePath) {
        FXMLLoader fxmlLoader = null;
        try {
            fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFilePath));
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            fxmlLoader.load();
            Objects.requireNonNull(fxmlLoader.getRoot(), "A Root FXML node must not be null");
        } catch (IOException e) {
            System.err.println("Unable to load FXML view: " + fxmlFilePath);
            e.printStackTrace();
            Platform.exit();
        }
        return fxmlLoader;
    }
    
    /**
     * Configure the stage to respect screen boundaries and set appropriate constraints.
     */
    private void configureStageForScreen() {
        // Get the primary screen bounds
        Screen primaryScreen = Screen.getPrimary();
        Rectangle2D screenBounds = primaryScreen.getVisualBounds();
        
        // Set maximum dimensions to screen size
        primaryStage.setMaxWidth(screenBounds.getWidth());
        primaryStage.setMaxHeight(screenBounds.getHeight());
        
        // Set minimum dimensions for usability
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        // Ensure the stage is resizable but constrained to screen
        primaryStage.setResizable(true);
        
        // Set initial position to center of screen
        primaryStage.setX(screenBounds.getMinX() + (screenBounds.getWidth() - 1200) / 2);
        primaryStage.setY(screenBounds.getMinY() + (screenBounds.getHeight() - 800) / 2);
    }
    
    /**
     * Apply screen size constraints to ensure the stage doesn't exceed screen boundaries.
     */
    private void applyScreenSizeConstraints() {
        Screen primaryScreen = Screen.getPrimary();
        Rectangle2D screenBounds = primaryScreen.getVisualBounds();
        
        // Get current stage dimensions
        double currentWidth = primaryStage.getWidth();
        double currentHeight = primaryStage.getHeight();
        
        // Constrain width to screen bounds
        if (currentWidth > screenBounds.getWidth()) {
            primaryStage.setWidth(screenBounds.getWidth());
        }
        
        // Constrain height to screen bounds
        if (currentHeight > screenBounds.getHeight()) {
            primaryStage.setHeight(screenBounds.getHeight());
        }
        
        // Ensure stage position is within screen bounds
        double stageX = primaryStage.getX();
        double stageY = primaryStage.getY();
        
        if (stageX < screenBounds.getMinX()) {
            primaryStage.setX(screenBounds.getMinX());
        } else if (stageX + primaryStage.getWidth() > screenBounds.getMaxX()) {
            primaryStage.setX(screenBounds.getMaxX() - primaryStage.getWidth());
        }
        
        if (stageY < screenBounds.getMinY()) {
            primaryStage.setY(screenBounds.getMinY());
        } else if (stageY + primaryStage.getHeight() > screenBounds.getMaxY()) {
            primaryStage.setY(screenBounds.getMaxY() - primaryStage.getHeight());
        }
    }
}

