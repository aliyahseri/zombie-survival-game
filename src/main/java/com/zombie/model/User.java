package com.zombie.model;

import java.time.LocalDateTime;

/**
 * User model class
 */
public class User {
    private int id;
    private String username;
    private String passwordHash;
    private Integer characterId;
    private Integer lastSelectedCharacterId;
    private LocalDateTime createdAt;

    public User() {}

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Integer getCharacterId() {
        return characterId;
    }

    public void setCharacterId(Integer characterId) {
        this.characterId = characterId;
    }

    public Integer getLastSelectedCharacterId() {
        return lastSelectedCharacterId;
    }

    public void setLastSelectedCharacterId(Integer lastSelectedCharacterId) {
        this.lastSelectedCharacterId = lastSelectedCharacterId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', characterId=" + characterId + "}";
    }
}
