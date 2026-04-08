package com.example.collabboard.controller;

import com.example.collabboard.model.User;
import com.example.collabboard.service.SessionManager;
import com.example.collabboard.service.UserService;
import com.example.collabboard.util.SceneManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.Random;

@Component
public class LoginController {

    private final UserService userService;
    private final SessionManager sessionManager;
    private final ApplicationContext applicationContext;

   
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Pane animationPane;


    public LoginController(UserService userService, SessionManager sessionManager, ApplicationContext applicationContext) {
        this.userService = userService;
        this.sessionManager = sessionManager;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        startBackgroundAnimation();
    }

    @FXML
    void handleLoginButtonAction(ActionEvent event) throws IOException {
        String username = usernameField.getText();
        String password = passwordField.getText();

        Optional<User> userOptional = userService.loginUser(username, password);

        if (userOptional.isPresent()) {
           
            sessionManager.setCurrentUser(userOptional.get());

           
            SceneManager.switchScene(event, "DashboardView.fxml", "CollabBoard - Dashboard", applicationContext);
        } else {
            errorLabel.setText("Invalid username or password.");
        }
    }

    @FXML
    void handleSignupLinkAction(ActionEvent event) throws IOException {
        SceneManager.switchScene(event, "SignupView.fxml", "CollabBoard - Sign Up", applicationContext);
    }

    @FXML
    void handleForgotPasswordLinkAction(ActionEvent event) throws IOException {
        System.out.println("Forgot Password link clicked!");
        
    }

    private void startBackgroundAnimation() {
        if (animationPane == null) return;
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            Circle circle = new Circle(random.nextInt(40) + 20);
            circle.setFill(javafx.scene.paint.Color.rgb(33, 150, 243, 0.12 + random.nextDouble() * 0.15));
            circle.setCenterX(random.nextDouble() * 800);
            circle.setCenterY(random.nextDouble() * 600);
            animationPane.getChildren().add(circle);

            TranslateTransition translate = new TranslateTransition(Duration.seconds(10 + random.nextDouble() * 10), circle);
            translate.setByX(random.nextDouble() * 200 - 100);
            translate.setByY(random.nextDouble() * 200 - 100);
            translate.setCycleCount(TranslateTransition.INDEFINITE);
            translate.setAutoReverse(true);
            translate.play();

            ScaleTransition scale = new ScaleTransition(Duration.seconds(5 + random.nextDouble() * 5), circle);
            scale.setByX(0.5);
            scale.setByY(0.5);
            scale.setCycleCount(ScaleTransition.INDEFINITE);
            scale.setAutoReverse(true);
            scale.play();

            FadeTransition fade = new FadeTransition(Duration.seconds(3 + random.nextDouble() * 3), circle);
            fade.setFromValue(0.3);
            fade.setToValue(0.8);
            fade.setCycleCount(FadeTransition.INDEFINITE);
            fade.setAutoReverse(true);
            fade.play();
        }
    }
}