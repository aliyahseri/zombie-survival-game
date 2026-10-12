package com.zombie.db;

import com.zombie.exception.AuthException;
import com.zombie.exception.DatabaseException;
import com.zombie.model.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User operations
 */
public class UserDAO {
    
    private final DBManager dbManager;
    
    public UserDAO() throws DatabaseException {
        this.dbManager = DBManager.getInstance();
    }
    
    /**
     * Create a new user
     */
    public void create(User user) throws DatabaseException, AuthException {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, hashPassword(user.getPasswordHash()));
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Failed to create user - no rows affected");
            }
            
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                throw new AuthException("Username already exists");
            }
            throw new DatabaseException("Failed to create user", e);
        }
    }
    
    /**
     * Authenticate user login
     */
    public User authenticate(String username, String password) throws AuthException, DatabaseException {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String inputHash = hashPassword(password);
                    
                    if (storedHash.equals(inputHash)) {
                        return mapResultSetToUser(rs);
                    } else {
                        throw new AuthException("Invalid password");
                    }
                } else {
                    throw new AuthException("User not found");
                }
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to authenticate user", e);
        }
    }
    
    /**
     * Get user by ID
     */
    public User findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                } else {
                    return null;
                }
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find user by ID", e);
        }
    }
    
    /**
     * Get user by username
     */
    public User findByUsername(String username) throws DatabaseException {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                } else {
                    return null;
                }
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find user by username", e);
        }
    }
    
    /**
     * Update user's selected character
     */
    public void updateCharacter(int userId, Integer characterId) throws DatabaseException {
        String sql = "UPDATE users SET character_id = ?, last_selected_character = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (characterId != null) {
                stmt.setInt(1, characterId);
                stmt.setInt(2, characterId);
            } else {
                stmt.setNull(1, Types.INTEGER);
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setInt(3, userId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Failed to update user character - user not found");
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update user character", e);
        }
    }
    
    /**
     * Update user's last selected character
     */
    public void updateLastSelectedCharacter(int userId, Integer characterId) throws DatabaseException {
        String sql = "UPDATE users SET last_selected_character = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (characterId != null) {
                stmt.setInt(1, characterId);
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setInt(2, userId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Failed to update user's last selected character - user not found");
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update user's last selected character", e);
        }
    }
    
    /**
     * Get all users
     */
    public List<User> findAll() throws DatabaseException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> users = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get all users", e);
        }
        
        return users;
    }
    
    /**
     * Map ResultSet to User object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        
        int characterId = rs.getInt("character_id");
        if (!rs.wasNull()) {
            user.setCharacterId(characterId);
        }
        
        // Handle last_selected_character (may be null in older databases)
        try {
            int lastSelectedCharacterId = rs.getInt("last_selected_character");
            if (!rs.wasNull()) {
                user.setLastSelectedCharacterId(lastSelectedCharacterId);
            }
        } catch (SQLException e) {
            // Column doesn't exist in older database versions
            user.setLastSelectedCharacterId(user.getCharacterId());
        }
        
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        
        return user;
    }
    
    /**
     * Hash password using SHA-256
     */
    private String hashPassword(String password) throws DatabaseException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new DatabaseException("Failed to hash password", e);
        }
    }
}
