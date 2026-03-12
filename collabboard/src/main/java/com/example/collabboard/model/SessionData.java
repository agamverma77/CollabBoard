package com.example.collabboard.model;

public class SessionData {
    private String id;
    private String title;
    private String lastActive;
    private int participantCount;
    private String colorCode;

    public SessionData() {}

    public SessionData(String id, String title, String lastActive, int participantCount, String colorCode) {
        this.id = id;
        this.title = title;
        this.lastActive = lastActive;
        this.participantCount = participantCount;
        this.colorCode = colorCode;
    }

    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLastActive() { return lastActive; }
    public void setLastActive(String lastActive) { this.lastActive = lastActive; }

    public int getParticipantCount() { return participantCount; }
    public void setParticipantCount(int participantCount) { this.participantCount = participantCount; }

    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }
}
