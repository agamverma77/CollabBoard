package com.example.collabboard.controller;

import com.example.collabboard.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private GeminiService geminiService;

    @PostMapping("/brainstorm")
    public List<String> brainstorm(@RequestBody Map<String, String> request) {
        String topic = request.getOrDefault("topic", "general collaboration");
        return geminiService.generateStickyNotes(topic);
    }
}