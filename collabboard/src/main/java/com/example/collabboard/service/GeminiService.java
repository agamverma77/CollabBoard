package com.example.collabboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> generateStickyNotes(String topic) {
       String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro:generateContent?key=" + apiKey;

        String prompt = "Generate 5 short, distinct brainstorming ideas for: " + topic + ". You must respond ONLY with a valid JSON array of strings. Do not include markdown formatting, code blocks, or conversational text. Example: [\"Idea 1\", \"Idea 2\"]";

        try {
            // NEW: Let Java securely build the JSON payload to prevent quote/escaping errors
            java.util.Map<String, Object> textPart = java.util.Map.of("text", prompt);
            java.util.Map<String, Object> content = java.util.Map.of("parts", java.util.List.of(textPart));
            java.util.Map<String, Object> requestBodyMap = java.util.Map.of("contents", java.util.List.of(content));
            
            String requestBody = objectMapper.writeValueAsString(requestBodyMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String aiText = root.path("candidates").get(0)
                                .path("content").path("parts").get(0)
                                .path("text").asText();

            aiText = aiText.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(aiText, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

        } catch (Exception e) {
            e.printStackTrace();
            return List.of("Failed to generate ideas. Please check server logs.");
        }
    }
}