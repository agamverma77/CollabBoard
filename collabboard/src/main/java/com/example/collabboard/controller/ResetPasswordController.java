package com.example.collabboard.controller;

import com.example.collabboard.config.FxmlView;
import com.example.collabboard.service.StageManager;
import com.example.collabboard.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ResetPasswordController {

    @Lazy
    @Autowired
    private StageManager stageManager;

    @Autowired
    private UserService userService;

    @FXML
    private TextField otpField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label messageLabel;

    private String email;

    public void setEmail(String email) {
        this.email = email;
    }

    @FXML
    void handleResetPasswordAction(ActionEvent event) {
        String otp = otpField.getText();
        String newPassword = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (otp.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("All fields are required.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        try {
            userService.resetPassword(email, otp, newPassword);
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Password reset successfully! You can now log in.");
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
