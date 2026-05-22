package com.example.collabboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is the main entry point for the DEPLOYED BACKEND SERVER ONLY.
 * It does NOT launch the JavaFX application.
 */
@SpringBootApplication
public class ServerApplication {

    public static void main(String[] args) {
        // This starts only the Spring Boot backend (web server, websockets, etc.)
        SpringApplication.run(ServerApplication.class, args);
    }
}
