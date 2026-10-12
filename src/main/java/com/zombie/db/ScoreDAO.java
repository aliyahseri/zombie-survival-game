package com.zombie.db;

import com.zombie.exception.DatabaseException;
import com.zombie.model.Score;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Score operations
 */
public class ScoreDAO {
    
    private final DBManager dbManager;
    
    public ScoreDAO() throws DatabaseException {
        this.dbManager = DBManager.getInstance();
    }
    
    /**
     * Create a new score
     */
    public void create(Score score) throws DatabaseException {
        String sql = "INSERT INTO scores (user_id, difficulty, completion_time_seconds, waves_cleared) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, score.getUserId());
            stmt.setString(2, score.getDifficulty());
            stmt.setInt(3, score.getCompletionTimeSeconds());
            stmt.setInt(4, score.getWavesCleared());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Failed to create score - no rows affected");
            }
            
            // Get generated ID
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    score.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseException("Failed to get generated score ID");
                }
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create score", e);
        }
    }
    
    /**
     * Get top scores for a specific difficulty - only winning scores, best time per user
     */
    public List<Score> getTopScores(String difficulty, int limit) throws DatabaseException {
        // Only show best time per user, only winning scores
        // For EASY: must clear 1 wave, MEDIUM: 5 waves, HARD: 5 waves
        String sql = """
            SELECT s.id, s.user_id, s.difficulty, s.completion_time_seconds, 
                   s.waves_cleared, s.created_at, u.username
            FROM scores s
            JOIN users u ON s.user_id = u.id
            WHERE s.difficulty = ?
            AND (
                (s.difficulty = 'EASY' AND s.waves_cleared = 1) OR
                (s.difficulty IN ('MEDIUM', 'HARD') AND s.waves_cleared = 5)
            )
            AND s.completion_time_seconds = (
                SELECT MIN(s2.completion_time_seconds)
                FROM scores s2
                WHERE s2.user_id = s.user_id
                AND s2.difficulty = s.difficulty
                AND (
                    (s2.difficulty = 'EASY' AND s2.waves_cleared = 1) OR
                    (s2.difficulty IN ('MEDIUM', 'HARD') AND s2.waves_cleared = 5)
                )
            )
            GROUP BY s.user_id
            ORDER BY s.completion_time_seconds ASC
            LIMIT ?
        """;

        List<Score> scores = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, difficulty);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Score score = mapResultSetToScore(rs);
                    score.setUsername(rs.getString("username"));
                    scores.add(score);
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to get top scores", e);
        }

        return scores;
    }
    
    /**
     * Get scores by user ID
     */
    public List<Score> findByUserId(int userId) throws DatabaseException {
        String sql = "SELECT * FROM scores WHERE user_id = ? ORDER BY created_at DESC";
        List<Score> scores = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    scores.add(mapResultSetToScore(rs));
                }
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find scores by user ID", e);
        }
        
        return scores;
    }
    
    /**
     * Get score by ID
     */
    public Score findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM scores WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToScore(rs);
                } else {
                    return null;
                }
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find score by ID", e);
        }
    }
    
    /**
     * Get user's rank for a specific difficulty - only counting winning scores
     */
    public int getUserRank(int userId, String difficulty) throws DatabaseException {
        String sql = """
            SELECT COUNT(*) + 1 as rank
            FROM (
                SELECT user_id, MIN(completion_time_seconds) as best_time
                FROM scores
                WHERE difficulty = ?
                AND (
                    (difficulty = 'EASY' AND waves_cleared = 1) OR
                    (difficulty IN ('MEDIUM', 'HARD') AND waves_cleared = 5)
                )
                GROUP BY user_id
            ) best_scores
            WHERE best_scores.best_time < (
                SELECT COALESCE(MIN(completion_time_seconds), 999999)
                FROM scores
                WHERE user_id = ? AND difficulty = ?
                AND (
                    (difficulty = 'EASY' AND waves_cleared = 1) OR
                    (difficulty IN ('MEDIUM', 'HARD') AND waves_cleared = 5)
                )
            )
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, difficulty);
            stmt.setInt(2, userId);
            stmt.setString(3, difficulty);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("rank");
                }
                return 1;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to get user rank", e);
        }
    }
    
    /**
     * Get best score for user in specific difficulty
     */
    public Score getBestScoreForUser(int userId, String difficulty) throws DatabaseException {
        String sql = """
            SELECT * FROM scores 
            WHERE user_id = ? AND difficulty = ? 
            ORDER BY completion_time_seconds ASC 
            LIMIT 1
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, difficulty);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToScore(rs);
                } else {
                    return null;
                }
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get best score for user", e);
        }
    }
    
    /**
     * Map ResultSet to Score object
     */
    private Score mapResultSetToScore(ResultSet rs) throws SQLException {
        Score score = new Score();
        score.setId(rs.getInt("id"));
        score.setUserId(rs.getInt("user_id"));
        score.setDifficulty(rs.getString("difficulty"));
        score.setCompletionTimeSeconds(rs.getInt("completion_time_seconds"));
        score.setWavesCleared(rs.getInt("waves_cleared"));
        score.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return score;
    }
}
