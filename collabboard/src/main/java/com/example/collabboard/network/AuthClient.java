package com.example.collabboard.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AuthClient {

    private static final String LOGIN_URL = "http://localhost:8080/api/auth/login";
    private static String jwtToken; // Store the token securely in memory

    public static boolean authenticate(String username, String password) {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Construct a simple JSON payload
        String jsonPayload = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Assuming the server returns the JWT directly as a string or in a JSON wrapper
                jwtToken = response.body(); 
                System.out.println("Login successful. Token saved.");
                return true;
            } else {
                System.out.println("Login failed: " + response.body());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getJwtToken() {
        return jwtToken;
    }
}