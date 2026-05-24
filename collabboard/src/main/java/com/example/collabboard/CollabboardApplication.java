package com.example.collabboard;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is now the main entry point for the DESKTOP CLIENT ONLY
 */
@SpringBootApplication
public class CollabboardApplication {

    public static void main(String[] args) {
        // This launches the JavaFX UI along with the Spring context.
        Application.launch(JavaFxApplication.class, args);
    }
}