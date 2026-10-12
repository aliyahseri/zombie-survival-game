package com.zombie.db;

import com.zombie.exception.DatabaseException;
import com.zombie.model.Character;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Character operations
 */
public class CharacterDAO {
    
    private final DBManager dbManager;
    
    public CharacterDAO() throws DatabaseException {
        this.dbManager = DBManager.getInstance();
    }
    
    /**
     * Get all characters
     */
    public List<Character> findAll() throws DatabaseException {
        String sql = "SELECT * FROM characters ORDER BY id";
        List<Character> characters = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                characters.add(mapResultSetToCharacter(rs));
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get all characters", e);
        }
        
        return characters;
    }
    
    /**
     * Get character by ID
     */
    public Character findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM characters WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCharacter(rs);
                } else {
                    return null;
                }
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find character by ID", e);
        }
    }
    
    /**
     * Create a new character
     */
    public void create(Character character) throws DatabaseException {
        String sql = "INSERT INTO characters (name, image_path, ability) VALUES (?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, character.getName());
            stmt.setString(2, character.getImagePath());
            stmt.setString(3, character.getAbility());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Failed to create character - no rows affected");
            }
            
            // Get generated ID
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    character.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseException("Failed to get generated character ID");
                }
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create character", e);
        }
    }
    
    /**
     * Update character
     */
    public void update(Character character) throws DatabaseException {
        String sql = "UPDATE characters SET name = ?, image_path = ?, ability = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, character.getName());
            stmt.setString(2, character.getImagePath());
            stmt.setString(3, character.getAbility());
            stmt.setInt(4, character.getId());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Failed to update character - character not found");
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update character", e);
        }
    }
    
    /**
     * Delete character by ID
     */
    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM characters WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Failed to delete character - character not found");
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete character", e);
        }
    }
    
    /**
     * Initialize database with default characters
     */
    public void initializeDefaultCharacters() throws DatabaseException {
        // Check if characters already exist
        List<Character> existing = findAll();
        if (!existing.isEmpty()) {
            return; // Characters already initialized
        }
        
        // Create the 3 specific characters
        Character[] defaultCharacters = {
            new Character("Jax", "character_1.png", "SHIELD"),
            new Character("Vi", "character_2.png", "PIERCING"),
            new Character("Flash", "character_3.png", "SMOKE")
        };
        
        for (Character character : defaultCharacters) {
            create(character);
        }
    }
    
    /**
     * Map ResultSet to Character object
     */
    private Character mapResultSetToCharacter(ResultSet rs) throws SQLException {
        Character character = new Character();
        character.setId(rs.getInt("id"));
        character.setName(rs.getString("name"));
        character.setImagePath(rs.getString("image_path"));
        character.setAbility(rs.getString("ability"));
        return character;
    }
}
