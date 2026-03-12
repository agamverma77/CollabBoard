package com.example.collabboard.model;

public class DashboardStats {
    private int activeSessions;
    private int totalProjects;
    private int teamMembers;
    private int hoursCollaborated;

    public DashboardStats() {}

    public DashboardStats(int activeSessions, int totalProjects, int teamMembers, int hoursCollaborated) {
        this.activeSessions = activeSessions;
        this.totalProjects = totalProjects;
        this.teamMembers = teamMembers;
        this.hoursCollaborated = hoursCollaborated;
    }

    
    public int getActiveSessions() { return activeSessions; }
    public void setActiveSessions(int activeSessions) { this.activeSessions = activeSessions; }

    public int getTotalProjects() { return totalProjects; }
    public void setTotalProjects(int totalProjects) { this.totalProjects = totalProjects; }

    public int getTeamMembers() { return teamMembers; }
    public void setTeamMembers(int teamMembers) { this.teamMembers = teamMembers; }

    public int getHoursCollaborated() { return hoursCollaborated; }
    public void setHoursCollaborated(int hoursCollaborated) { this.hoursCollaborated = hoursCollaborated; }
}
