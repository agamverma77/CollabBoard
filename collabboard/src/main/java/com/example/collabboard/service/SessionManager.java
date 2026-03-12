package com.example.collabboard.service;

import com.example.collabboard.model.User;
import org.springframework.stereotype.Service;

@Service
public class SessionManager {

    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    public void clearSession() {
        this.currentUser = null;
    }
}