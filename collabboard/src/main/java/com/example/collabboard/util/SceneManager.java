package com.example.collabboard.util;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext; 

import java.io.IOException;

public class SceneManager {

    // The method now requires the applicationContext
    public static void switchScene(ActionEvent event, String fxmlFile, String title, ApplicationContext context) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/fxml/" + fxmlFile));
        loader.setControllerFactory(context::getBean); 

        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle(title);
        stage.show();
    }
}