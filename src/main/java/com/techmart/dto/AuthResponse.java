package com.techmart.dto;

public class AuthResponse {
    private boolean success;
    private String message;
    private String sessionToken;
    private Long userId;
    private String username;
    private int concurrentSessions;

    public AuthResponse() {
    }

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getConcurrentSessions() { return concurrentSessions; }
    public void setConcurrentSessions(int count) { this.concurrentSessions = count; }
}
