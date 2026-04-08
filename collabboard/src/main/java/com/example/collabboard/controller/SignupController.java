package com.example.collabboard.controller;

import com.example.collabboard.service.UserService;
import com.example.collabboard.util.SceneManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.shape.Circle;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Random;
import javafx.util.Duration;

@Component
public class SignupController {

    private final UserService userService;
    private final ApplicationContext applicationContext;

    public SignupController(UserService userService, ApplicationContext applicationContext) {
        this.userService = userService;
        this.applicationContext = applicationContext;
    }

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private CheckBox termsCheckBox;
    @FXML private Pane animationPane;

    @FXML
    public void initialize() {
        startBackgroundAnimation();
    }

    private void startBackgroundAnimation() {
        if (animationPane == null) {
            return;
        }
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            Circle circle = new Circle();
            circle.setRadius(random.nextInt(40) + 20);
            int colorChoice = random.nextInt(3);
            javafx.scene.paint.Color color = colorChoice == 0
                    ? javafx.scene.paint.Color.rgb(33, 150, 243, 0.12 + random.nextDouble() * 0.15)
                    : colorChoice == 1
                        ? javafx.scene.paint.Color.rgb(76, 175, 80, 0.12 + random.nextDouble() * 0.15)
                        : javafx.scene.paint.Color.rgb(255, 152, 0, 0.12 + random.nextDouble() * 0.15);
            circle.setFill(color);
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

    @FXML
    protected void handleSignupButtonAction(ActionEvent event) throws IOException { 
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please fill in all fields.");
            return;
        }

        if (termsCheckBox != null && !termsCheckBox.isSelected()) {
            messageLabel.setText("Please accept the terms and conditions.");
            return;
        }

        try {
            
            userService.registerUser(username, email, password);

            
            SceneManager.switchScene(
                event,
                "LoginView.fxml",
                "CollabBoard Login",
                applicationContext
            );

        } catch (Exception e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    protected void handleLoginLinkAction(ActionEvent event) throws IOException {
       
        SceneManager.switchScene(event, "LoginView.fxml", "CollabBoard Login", applicationContext);
    }
}