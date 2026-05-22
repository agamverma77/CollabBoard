package com.example.collabboard.controller;

import com.example.collabboard.service.SessionManager;
import com.example.collabboard.util.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MainController {


    private final ApplicationContext applicationContext;
    private final SessionManager sessionManager;

   
    @FXML private BorderPane mainContainer;
    @FXML private Button homeBtn;
    @FXML private Button whiteboardBtn;
    
    public MainController(ApplicationContext applicationContext, SessionManager sessionManager) {
        this.applicationContext = applicationContext;
        this.sessionManager = sessionManager;
    }

    @FXML
    public void initialize() {
        
        loadView("/fxml/DashboardView.fxml");
    }

    
    @FXML
    private void handleHomeButtonAction(ActionEvent event) {
        loadView("/fxml/DashboardView.fxml");
    }

   
    @FXML
    private void handleWhiteboardButtonAction(ActionEvent event) {
        loadView("/fxml/WhiteboardView.fxml");
    }
    
   
    @FXML
    void handleLogoutButtonAction(ActionEvent event) throws IOException {
        
        sessionManager.clearSession();
  
        SceneManager.switchScene(event, "LoginView.fxml", "CollabBoard Login", applicationContext);
    }

    /**

     * @param fxmlPath 
     */
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            loader.setControllerFactory(applicationContext::getBean);
            Parent view = loader.load();
            mainContainer.setCenter(view);
        } catch (IOException e) {
            System.err.println("Error loading FXML view: " + fxmlPath);
            e.printStackTrace();
        }
    }
}