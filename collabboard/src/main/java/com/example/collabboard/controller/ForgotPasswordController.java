package com.example.collabboard.controller;

import com.example.collabboard.config.FxmlView;
import com.example.collabboard.service.StageManager;
import com.example.collabboard.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ForgotPasswordController {

    @Lazy
    @Autowired
    private StageManager stageManager;

    @Autowired
    private UserService userService;

    @FXML
    private TextField emailField;

    @FXML
    private Label messageLabel;

    @FXML
    void handleSendOtpAction(ActionEvent event) {
        String email = emailField.getText();
        if (email == null || email.trim().isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Please enter an email address.");
            return;
        }

        try {
            userService.generateAndSendOtp(email);
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("OTP sent successfully. Please check your email.");
            // Navigate to the reset password screen, passing the email
            ResetPasswordController nextController = stageManager.switchScene(FxmlView.RESET_PASSWORD);
            nextController.setEmail(email);
        } catch (Exception e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    void handleBackToLoginAction(ActionEvent event) {
        stageManager.switchScene(FxmlView.LOGIN);
    }
}
