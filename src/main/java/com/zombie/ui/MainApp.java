package com.zombie.ui;

import com.zombie.db.CharacterDAO;
import com.zombie.db.ScoreDAO;
import com.zombie.exception.DatabaseException;
import com.zombie.game.Game;
import com.zombie.game.GameState;
import com.zombie.model.Character;
import com.zombie.model.User;
import com.zombie.util.Constants;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Main application class that manages screens and navigation
 */
public class MainApp extends Application {
    
    private Stage primaryStage;
    private User currentUser;
    private Game currentGame;
    private String currentDifficulty = "EASY";
    
    // Screen instances
    private LoginScreen loginScreen;
    private SignUpScreen signUpScreen;
    private CharacterSelectScreen characterSelectScreen;
    private HomeScreen homeScreen;
    private LeaderboardScreen leaderboardScreen;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        try {
            // Initialize screens
            initializeScreens();
            
            // Set up primary stage
            primaryStage.setTitle("Zombie Survival Game");
            primaryStage.setResizable(false);
            primaryStage.setOnCloseRequest(e -> {
                e.consume();
                handleExit();
            });
            
            // Show login screen first
            showLoginScreen();
            
        } catch (Exception e) {
            showError("Application Error", "Failed to start application: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeScreens() throws DatabaseException {
        loginScreen = new LoginScreen(this);
        signUpScreen = new SignUpScreen(this);
        // Don't initialize these here - they need a logged-in user
        // characterSelectScreen, homeScreen, leaderboardScreen are created on demand
    }
    
    public void showLoginScreen() {
        primaryStage.setScene(loginScreen.getScene());
        primaryStage.show();
    }
    
    public void showSignUpScreen() {
        primaryStage.setScene(signUpScreen.getScene());
        primaryStage.show();
    }
    
    public void showCharacterSelectScreen() {
        try {
            if (characterSelectScreen == null) {
                characterSelectScreen = new CharacterSelectScreen(this);
            }
            primaryStage.setScene(characterSelectScreen.getScene());
            primaryStage.show();
        } catch (DatabaseException e) {
            showError("Error", "Failed to load character selection: " + e.getMessage());
        }
    }
    
    public void showHomeScreen() {
        try {
            homeScreen = new HomeScreen(this);
            primaryStage.setScene(homeScreen.getScene());
            primaryStage.show();
        } catch (DatabaseException e) {
            showError("Error", "Failed to load home screen: " + e.getMessage());
        }
    }
    
    public void showLeaderboardScreen() {
        try {
            leaderboardScreen = new LeaderboardScreen(this);
            primaryStage.setScene(leaderboardScreen.getScene());
            primaryStage.show();
        } catch (DatabaseException e) {
            showError("Error", "Failed to load leaderboard: " + e.getMessage());
        }
    }
    
    public void startGame(String difficulty) {
        try {
            this.currentDifficulty = difficulty;
            
            // Ensure GameState is synchronized before starting game
            if (getCurrentUser() != null && getCurrentUser().getLastSelectedCharacterId() != null) {
                try {
                    CharacterDAO characterDAO = new CharacterDAO();
                    Character selectedCharacter = characterDAO.findById(getCurrentUser().getLastSelectedCharacterId());
                    if (selectedCharacter != null) {
                        GameState.getInstance().setSelectedCharacter(selectedCharacter);
                    }
                } catch (DatabaseException e) {
                    System.err.println("Warning: Could not sync GameState before game start: " + e.getMessage());
                }
            }
            
            currentGame = new Game(this, difficulty);
            Scene gameScene = new Scene(currentGame, Constants.CANVAS_WIDTH, Constants.CANVAS_HEIGHT);
            primaryStage.setScene(gameScene);
            primaryStage.show();
            currentGame.requestFocus();
        } catch (Exception e) {
            showError("Game Error", "Failed to start game: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void endGame(boolean won, int wavesCleared, int zombiesKilled, double gameTime) {
        showHomeScreen();

        if (won) {
            try {
                ScoreDAO scoreDAO = new ScoreDAO();
                com.zombie.model.Score score = new com.zombie.model.Score(
                    currentUser.getId(),
                    currentDifficulty,
                    (int) gameTime,
                    wavesCleared
                );
                scoreDAO.create(score);
                int rank = scoreDAO.getUserRank(currentUser.getId(), currentDifficulty);
                showGameWon(wavesCleared, zombiesKilled, gameTime, rank);
            } catch (DatabaseException e) {
                showError("Database Error", "Failed to save score: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showGameLost(wavesCleared, zombiesKilled, gameTime);
        }
    }
    
    private String getGameDifficulty() {
        return currentDifficulty;
    }
    
    private void showGameWon(int wavesCleared, int zombiesKilled, double gameTime, int rank) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Victory!");
        alert.setHeaderText("Congratulations! You survived!");
        
        String message = String.format(
            "Waves Cleared: %d\n" +
            "Zombies Killed: %d\n" +
            "Time: %02d:%02d\n" +
            "Your Rank: #%d",
            wavesCleared, zombiesKilled,
            (int) gameTime / 60, (int) gameTime % 60,
            rank
        );
        
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showGameLost(int wavesCleared, int zombiesKilled, double gameTime) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("You didn't survive the zombie apocalypse...");
        
        String message = String.format(
            "Waves Cleared: %d\n" +
            "Zombies Killed: %d\n" +
            "Time: %02d:%02d",
            wavesCleared, zombiesKilled,
            (int) gameTime / 60, (int) gameTime % 60
        );
        
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void handleExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Game");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("Any unsaved progress will be lost.");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            primaryStage.close();
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Getters and setters
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
