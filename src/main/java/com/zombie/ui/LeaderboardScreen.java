package com.zombie.ui;

import com.zombie.db.ScoreDAO;
import com.zombie.exception.DatabaseException;
import com.zombie.model.Score;
import com.zombie.util.Constants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Leaderboard Screen showing top scores for each difficulty
 */
public class LeaderboardScreen {
    
    private final MainApp mainApp;
    private Scene scene;
    private final ScoreDAO scoreDAO;
    
    public LeaderboardScreen(MainApp mainApp) throws DatabaseException {
        this.mainApp = mainApp;
        this.scoreDAO = new ScoreDAO();
        createScene();
    }
    
    private void createScene() {
        // Main container with background
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setStyle("-fx-background-color: #1a1a1a;");
        
        // Title
        Text title = new Text("LEADERBOARD");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setFill(Color.RED);
        
        // Tab pane for different difficulties
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");
        
        // Create tabs for each difficulty
        Tab easyTab = createDifficultyTab("EASY", "EASY", Color.GREEN);
        Tab mediumTab = createDifficultyTab("MEDIUM", "MEDIUM", Color.YELLOW);
        Tab hardTab = createDifficultyTab("HARD", "HARD", Color.RED);
        
        tabPane.getTabs().addAll(easyTab, mediumTab, hardTab);
        
        // Back button
        Button backButton = new Button("Back to Home");
        backButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 12 25;");
        backButton.setPrefWidth(200);
        
        backButton.setOnAction(e -> mainApp.showHomeScreen());
        
        // Add everything to main container
        mainContainer.getChildren().addAll(title, tabPane, backButton);
        
        // Create scene
        scene = new Scene(mainContainer, 900, 700);
        scene.getStylesheets().add(getClass().getResource(Constants.CSS_FILE).toExternalForm());
    }
    
    private Tab createDifficultyTab(String tabName, String difficulty, Color accentColor) {
        Tab tab = new Tab(tabName);
        
        // Content container
        VBox tabContent = new VBox(20);
        tabContent.setAlignment(Pos.CENTER);
        tabContent.setPadding(new Insets(20));
        tabContent.setStyle("-fx-background-color: rgba(40, 40, 40, 0.9); -fx-background-radius: 10;");
        
        // Difficulty label
        Text difficultyLabel = new Text(difficulty + " MODE");
        difficultyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        difficultyLabel.setFill(accentColor);
        
        // Scores table
        TableView<ScoreEntry> scoresTable = createScoresTable(difficulty);
        
        // Load scores
        loadScoresIntoTable(scoresTable, difficulty);
        
        // Add components to tab content
        tabContent.getChildren().addAll(difficultyLabel, scoresTable);
        
        tab.setContent(tabContent);
        
        return tab;
    }
    
    private TableView<ScoreEntry> createScoresTable(String difficulty) {
        TableView<ScoreEntry> table = new TableView<>();
        table.setPrefHeight(400);
        table.setPrefWidth(800);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Style the table
        table.setStyle("-fx-background-color: transparent; -fx-border-color: #444; -fx-border-width: 1;");
        
        // Create columns
        TableColumn<ScoreEntry, Integer> rankColumn = new TableColumn<>("Rank");
        rankColumn.setCellValueFactory(data -> data.getValue().rankProperty().asObject());
        rankColumn.setPrefWidth(80);
        
        TableColumn<ScoreEntry, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(data -> data.getValue().usernameProperty());
        usernameColumn.setPrefWidth(200);
        
        TableColumn<ScoreEntry, String> timeColumn = new TableColumn<>("Time");
        timeColumn.setCellValueFactory(data -> data.getValue().timeProperty());
        timeColumn.setPrefWidth(100);
        
        TableColumn<ScoreEntry, Integer> wavesColumn = new TableColumn<>("Waves");
        wavesColumn.setCellValueFactory(data -> data.getValue().wavesProperty().asObject());
        wavesColumn.setPrefWidth(100);
        
        TableColumn<ScoreEntry, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(data -> data.getValue().dateProperty());
        dateColumn.setPrefWidth(150);
        
        table.getColumns().addAll(rankColumn, usernameColumn, timeColumn, wavesColumn, dateColumn);
        
        // Style columns
        for (TableColumn<?, ?> column : table.getColumns()) {
            column.setStyle("-fx-alignment: CENTER; -fx-font-size: 14; -fx-font-weight: bold;");
        }
        
        return table;
    }
    
    private void loadScoresIntoTable(TableView<ScoreEntry> table, String difficulty) {
        try {
            List<Score> scores = scoreDAO.getTopScores(difficulty, 10);
            
            for (int i = 0; i < scores.size(); i++) {
                Score score = scores.get(i);
                ScoreEntry entry = new ScoreEntry(
                    i + 1, // Rank
                    score.getUsername(), // Username from SQL join
                    formatTime(score.getCompletionTimeSeconds()),
                    score.getWavesCleared(),
                    formatDate(score.getCreatedAt())
                );
                table.getItems().add(entry);
            }
            
            // Highlight current user's score if present
            if (mainApp.getCurrentUser() != null) {
                highlightUserScore(table);
            }
            
        } catch (DatabaseException e) {
            // Show error in table
            table.setPlaceholder(new Label("Error loading scores: " + e.getMessage()));
        }
    }
    
        
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    private String formatDate(java.time.LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
    
    private void highlightUserScore(TableView<ScoreEntry> table) {
        // This would highlight the current user's score if present
        // Implementation depends on having user information in the scores
    }
    
    // Helper class for table entries
    public static class ScoreEntry {
        private final javafx.beans.property.IntegerProperty rank = new javafx.beans.property.SimpleIntegerProperty();
        private final javafx.beans.property.StringProperty username = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.StringProperty time = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.IntegerProperty waves = new javafx.beans.property.SimpleIntegerProperty();
        private final javafx.beans.property.StringProperty date = new javafx.beans.property.SimpleStringProperty();
        
        public ScoreEntry(int rank, String username, String time, int waves, String date) {
            setRank(rank);
            setUsername(username);
            setTime(time);
            setWaves(waves);
            setDate(date);
        }
        
        // Property getters
        public javafx.beans.property.IntegerProperty rankProperty() { return rank; }
        public javafx.beans.property.StringProperty usernameProperty() { return username; }
        public javafx.beans.property.StringProperty timeProperty() { return time; }
        public javafx.beans.property.IntegerProperty wavesProperty() { return waves; }
        public javafx.beans.property.StringProperty dateProperty() { return date; }
        
        // Getters and setters
        public int getRank() { return rank.get(); }
        public void setRank(int rank) { this.rank.set(rank); }
        
        public String getUsername() { return username.get(); }
        public void setUsername(String username) { this.username.set(username); }
        
        public String getTime() { return time.get(); }
        public void setTime(String time) { this.time.set(time); }
        
        public int getWaves() { return waves.get(); }
        public void setWaves(int waves) { this.waves.set(waves); }
        
        public String getDate() { return date.get(); }
        public void setDate(String date) { this.date.set(date); }
    }
    
    public Scene getScene() {
        return scene;
    }
}
