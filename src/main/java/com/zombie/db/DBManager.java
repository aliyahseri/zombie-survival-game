package com.zombie.db;

import com.zombie.exception.DatabaseException;
import com.zombie.util.Constants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBManager {

    private static DBManager instance;
    private Connection connection;

    private DBManager() throws DatabaseException {
        try {
            Class.forName("org.sqlite.JDBC");
            connect();
            createTables();
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("SQLite driver not found", e);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to initialize database connection", e);
        }
    }

    private void connect() throws SQLException {
        String url = "jdbc:sqlite:" + Constants.DB_FILE;
        connection = DriverManager.getConnection(url);
        // Set busy timeout FIRST before anything else
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA busy_timeout = 5000");
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }

    public static synchronized DBManager getInstance() throws DatabaseException {
        if (instance == null) {
            instance = new DBManager();
        } else {
            // Reconnect if connection is closed
            try {
                if (instance.connection == null || instance.connection.isClosed()) {
                    instance.connect();
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to reconnect to database", e);
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    private void createTables() throws DatabaseException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS characters (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    image_path TEXT NOT NULL,
                    ability TEXT
                )
            """);

            // Add ability column if it doesn't exist (for existing databases)
            try {
                stmt.execute("ALTER TABLE characters ADD COLUMN ability TEXT");
            } catch (SQLException e) {
                // Column already exists, ignore error
            }

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    character_id INTEGER,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (character_id) REFERENCES characters(id)
                )
            """);

            // Add last_selected_character column if it doesn't exist (for existing databases)
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN last_selected_character INTEGER");
            } catch (SQLException e) {
                // Column already exists, ignore error
            }

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS scores (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    difficulty TEXT NOT NULL,
                    completion_time_seconds INTEGER NOT NULL,
                    waves_cleared INTEGER NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);

            insertDefaultCharacters(stmt);

        } catch (SQLException e) {
            throw new DatabaseException("Failed to create database tables", e);
        }
    }

    private void insertDefaultCharacters(Statement stmt) throws SQLException {
        var rs = stmt.executeQuery("SELECT COUNT(*) as count FROM characters");
        if (rs.next() && rs.getInt("count") == 0) {
            // Insert the 3 specific characters with their abilities
            String[][] characters = {
                {"Jax", "character_1.png", "SHIELD"},
                {"Vi", "character_2.png", "PIERCING"},
                {"Flash", "character_3.png", "SMOKE"}
            };

            for (String[] character : characters) {
                String name = character[0];
                String imagePath = character[1];
                String ability = character[2];
                stmt.execute(String.format(
                    "INSERT INTO characters (name, image_path, ability) VALUES ('%s', '%s', '%s')",
                    name, imagePath, ability
                ));
            }
        } else {
            // Update existing characters to have correct names and abilities
            String[][] characterUpdates = {
                {"1", "Jax", "SHIELD"},
                {"2", "Vi", "PIERCING"},
                {"3", "Flash", "SMOKE"}
            };
            
            for (String[] update : characterUpdates) {
                String id = update[0];
                String name = update[1];
                String ability = update[2];
                stmt.execute(String.format(
                    "UPDATE characters SET name = '%s', ability = '%s' WHERE id = %s",
                    name, ability, id
                ));
            }
        }
        rs.close();

        // Ensure the 4th character exists (fresh and older databases)
        stmt.execute("INSERT OR IGNORE INTO characters (id, name, image_path, ability) VALUES (4, 'Peter', 'character_4.png', 'HEALTH')");
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}
