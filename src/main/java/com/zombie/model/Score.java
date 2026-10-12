package com.zombie.model;

import java.time.LocalDateTime;

/**
 * Score model class
 */
public class Score {
    private int id;
    private int userId;
    private String difficulty;
    private int completionTimeSeconds;
    private int wavesCleared;
    private LocalDateTime createdAt;
    private String username;

    public Score() {}

    public Score(int userId, String difficulty, int completionTimeSeconds, int wavesCleared) {
        this.userId = userId;
        this.difficulty = difficulty;
        this.completionTimeSeconds = completionTimeSeconds;
        this.wavesCleared = wavesCleared;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getCompletionTimeSeconds() {
        return completionTimeSeconds;
    }

    public void setCompletionTimeSeconds(int completionTimeSeconds) {
        this.completionTimeSeconds = completionTimeSeconds;
    }

    public int getWavesCleared() {
        return wavesCleared;
    }

    public void setWavesCleared(int wavesCleared) {
        this.wavesCleared = wavesCleared;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "Score{id=" + id + ", userId=" + userId + ", difficulty='" + difficulty + 
               "', time=" + completionTimeSeconds + "s, waves=" + wavesCleared + "}";
    }
}
