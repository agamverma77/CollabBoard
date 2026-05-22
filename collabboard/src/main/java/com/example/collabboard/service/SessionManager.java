package com.example.collabboard.service;

import com.example.collabboard.model.User;
import org.springframework.stereotype.Service;

import java.util.prefs.Preferences;

@Service
public class SessionManager {
    private static final String JWT_PREF_KEY = "collabboard.jwt.token";
    private final Preferences preferences = Preferences.userNodeForPackage(SessionManager.class);

    private User currentUser;
    private String jwtToken;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
        if (jwtToken == null || jwtToken.isBlank()) {
            preferences.remove(JWT_PREF_KEY);
        } else {
            preferences.put(JWT_PREF_KEY, jwtToken);
        }
    }

    public String getJwtToken() {
        if ((jwtToken == null || jwtToken.isBlank())) {
            jwtToken = preferences.get(JWT_PREF_KEY, null);
        }
        return jwtToken;
    }

    public void clearSession() {
        this.currentUser = null;
        this.jwtToken = null;
    }
}