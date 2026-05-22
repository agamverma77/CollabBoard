package com.example.collabboard.service;

import com.example.collabboard.dto.AuthRequest;
import com.example.collabboard.dto.AuthResponse;
import com.example.collabboard.dto.SignupRequest;
import com.example.collabboard.model.User;
import com.example.collabboard.util.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthService(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse signup(SignupRequest request) throws Exception {
        userService.registerUser(request.getUsername(), request.getEmail(), request.getPassword());
        User user = userService.findByUsername(request.getUsername())
            .orElseThrow(() -> new IllegalStateException("User was created but could not be loaded."));
        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }

    public AuthResponse login(AuthRequest request) throws Exception {
        User user = userService.loginUser(request.getUsername(), request.getPassword())
            .orElseThrow(() -> new Exception("Invalid username or password."));
        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }
}
