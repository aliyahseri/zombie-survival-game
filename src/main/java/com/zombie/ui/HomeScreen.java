package com.zombie.ui;

import com.zombie.db.CharacterDAO;
import com.zombie.db.UserDAO;
import com.zombie.exception.DatabaseException;
import com.zombie.game.GameState;
import com.zombie.model.Character;
import com.zombie.util.AssetLoader;
import com.zombie.util.Constants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Home Screen with game options and difficulty selection
 */
public class HomeScreen {
    
    private final MainApp mainApp;
    private Scene scene;
    private final ToggleGroup difficultyGroup;
    private Character selectedCharacter;
    
    public HomeScreen(MainApp mainApp) throws DatabaseException {
        this.mainApp = mainApp;
        this.difficultyGroup = new ToggleGroup();
        loadCharacter();
        createScene();
    }
    
    private void loadCharacter() throws DatabaseException {
        CharacterDAO characterDAO = new CharacterDAO();
        GameState gameState = GameState.getInstance();
        
        // Try to load from last_selected_character first, then fall back to character_id
        Integer characterId = mainApp.getCurrentUser().getLastSelectedCharacterId();
        if (characterId == null) {
            characterId = mainApp.getCurrentUser().getCharacterId();
        }
        
        if (characterId != null) {
            selectedCharacter = characterDAO.findById(characterId);
            if (selectedCharacter != null) {
                gameState.setSelectedCharacter(selectedCharacter);
            }
        }
    }
    
    private void createScene() {
        // Main container with background
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setStyle("-fx-background-color: #1a1a1a;");
        
        // Top section with user info and character display
        HBox userInfoContainer = new HBox(20);
        userInfoContainer.setAlignment(Pos.CENTER_LEFT);
        userInfoContainer.setPadding(new Insets(20));
        userInfoContainer.setStyle("-fx-background-color: rgba(40, 40, 40, 0.9); -fx-background-radius: 10;");
        
        // Character display area
        VBox characterDisplayContainer = new VBox(10);
        characterDisplayContainer.setAlignment(Pos.CENTER);
        characterDisplayContainer.setPadding(new Insets(15));
        characterDisplayContainer.setStyle("-fx-background-color: rgba(60, 60, 60, 0.8); -fx-background-radius: 8; -fx-border-color: #444; -fx-border-width: 1; -fx-border-radius: 8;");
        characterDisplayContainer.setPrefWidth(200);
        
        // Character avatar
        ImageView avatarView = new ImageView();
        avatarView.setFitWidth(80);
        avatarView.setFitHeight(80);
        avatarView.setPreserveRatio(true);
        
        if (selectedCharacter != null) {
            try {
                String previewPath = "/assets/characters/char" + selectedCharacter.getId() + "/idle_0.png";
                Image characterImage = new Image(getClass().getResourceAsStream(previewPath));
                if (characterImage != null && !characterImage.isError()) {
                    avatarView.setImage(characterImage);
                } else {
                    Image fallbackImage = AssetLoader.loadImage(selectedCharacter.getImagePath());
                    if (fallbackImage != null) {
                        avatarView.setImage(fallbackImage);
                    }
                }
            } catch (Exception e) {
                Image fallbackImage = AssetLoader.loadImage(selectedCharacter.getImagePath());
                if (fallbackImage != null) {
                    avatarView.setImage(fallbackImage);
                }
            }
        }
        
        if (selectedCharacter == null || avatarView.getImage() == null) {
            Rectangle placeholder = new Rectangle(80, 80);
            placeholder.setFill(Color.GRAY);
            placeholder.setStroke(Color.WHITE);
            placeholder.setStrokeWidth(2);
            avatarView.setImage(null);
            characterDisplayContainer.getChildren().add(placeholder);
        } else {
            characterDisplayContainer.getChildren().add(avatarView);
        }
        
        // Character info
        Label characterNameLabel = new Label(selectedCharacter != null ? selectedCharacter.getName() : "No Character");
        characterNameLabel.setTextFill(Color.WHITE);
        characterNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        Label characterAbilityLabel = new Label(selectedCharacter != null ? selectedCharacter.getAbility() : "None");
        characterAbilityLabel.setTextFill(Color.LIGHTBLUE);
        characterAbilityLabel.setFont(Font.font("Arial", 12));
        
        // Change Character button
        Button changeCharacterButton = new Button("CHANGE CHARACTER");
        changeCharacterButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 8 15;");
        changeCharacterButton.setPrefWidth(180);
        changeCharacterButton.setFocusTraversable(false);
        changeCharacterButton.setOnAction(e -> showCharacterSelectionModal());
        
        characterDisplayContainer.getChildren().addAll(characterNameLabel, characterAbilityLabel, changeCharacterButton);
        
        // User info
        VBox userTextContainer = new VBox(5);
        userTextContainer.setAlignment(Pos.CENTER_LEFT);
        
        Label welcomeLabel = new Label("Welcome back,");
        welcomeLabel.setTextFill(Color.LIGHTGRAY);
        welcomeLabel.setFont(Font.font("Arial", 14));
        
        Label usernameLabel = new Label(mainApp.getCurrentUser().getUsername());
        usernameLabel.setTextFill(Color.WHITE);
        usernameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        
        Label statusLabel = new Label("Ready to survive?");
        statusLabel.setTextFill(Color.LIGHTGREEN);
        statusLabel.setFont(Font.font("Arial", 12));
        
        userTextContainer.getChildren().addAll(welcomeLabel, usernameLabel, statusLabel);
        
        userInfoContainer.getChildren().addAll(characterDisplayContainer, userTextContainer);
        
        // Main title
        Text title = new Text("ZOMBIE SURVIVAL");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setFill(Color.RED);
        
        // Difficulty selection
        VBox difficultyContainer = new VBox(15);
        difficultyContainer.setAlignment(Pos.CENTER);
        difficultyContainer.setPadding(new Insets(20));
        difficultyContainer.setStyle("-fx-background-color: rgba(40, 40, 40, 0.9); -fx-background-radius: 10;");
        
        Label difficultyLabel = new Label("SELECT DIFFICULTY");
        difficultyLabel.setTextFill(Color.WHITE);
        difficultyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        
        HBox difficultyButtons = new HBox(20);
        difficultyButtons.setAlignment(Pos.CENTER);
        
        RadioButton easyRadio = new RadioButton("EASY");
        easyRadio.setToggleGroup(difficultyGroup);
        easyRadio.setTextFill(Color.GREEN);
        easyRadio.setFont(Font.font("Arial", 14));
        easyRadio.setUserData("EASY");
        easyRadio.setSelected(true); // Default selection
        
        RadioButton mediumRadio = new RadioButton("MEDIUM");
        mediumRadio.setToggleGroup(difficultyGroup);
        mediumRadio.setTextFill(Color.YELLOW);
        mediumRadio.setFont(Font.font("Arial", 14));
        mediumRadio.setUserData("MEDIUM");
        
        RadioButton hardRadio = new RadioButton("HARD");
        hardRadio.setToggleGroup(difficultyGroup);
        hardRadio.setTextFill(Color.RED);
        hardRadio.setFont(Font.font("Arial", 14));
        hardRadio.setUserData("HARD");
        
        difficultyButtons.getChildren().addAll(easyRadio, mediumRadio, hardRadio);
        
        // Difficulty descriptions
        VBox descriptions = new VBox(5);
        descriptions.setAlignment(Pos.CENTER);
        
        Label easyDesc = new Label("Fewer zombies, less damage");
        easyDesc.setTextFill(Color.LIGHTGRAY);
        easyDesc.setFont(Font.font("Arial", 10));
        
        Label mediumDesc = new Label("Balanced challenge");
        mediumDesc.setTextFill(Color.LIGHTGRAY);
        mediumDesc.setFont(Font.font("Arial", 10));
        
        Label hardDesc = new Label("Brutal zombie horde");
        hardDesc.setTextFill(Color.LIGHTGRAY);
        hardDesc.setFont(Font.font("Arial", 10));
        
        descriptions.getChildren().addAll(easyDesc, mediumDesc, hardDesc);
        
        difficultyContainer.getChildren().addAll(difficultyLabel, difficultyButtons, descriptions);
        
        // Main buttons
        VBox buttonContainer = new VBox(15);
        buttonContainer.setAlignment(Pos.CENTER);
        
        Button startGameButton = new Button("START GAME");
        startGameButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 15 30;");
        startGameButton.setPrefWidth(200);
        startGameButton.setFocusTraversable(false);
        
        Button leaderboardButton = new Button("LEADERBOARD");
        leaderboardButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 12 25;");
        leaderboardButton.setPrefWidth(200);
        leaderboardButton.setFocusTraversable(false);
        
        Button logoutButton = new Button("LOGOUT");
        logoutButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 12 25;");
        logoutButton.setPrefWidth(200);
        logoutButton.setFocusTraversable(false);
        
        buttonContainer.getChildren().addAll(startGameButton, leaderboardButton, logoutButton);
        
        // Add everything to main container
        mainContainer.getChildren().addAll(userInfoContainer, title, difficultyContainer, buttonContainer);
        
        // Event handlers
        startGameButton.setOnAction(e -> {
            String difficulty = (String) difficultyGroup.getSelectedToggle().getUserData();
            mainApp.startGame(difficulty);
        });
        
        leaderboardButton.setOnAction(e -> mainApp.showLeaderboardScreen());
        
        logoutButton.setOnAction(e -> {
            mainApp.setCurrentUser(null);
            mainApp.showLoginScreen();
        });
        
        // Create scene
        scene = new Scene(mainContainer, 900, 700);
        scene.getStylesheets().add(getClass().getResource(Constants.CSS_FILE).toExternalForm());
    }
    
    public Scene getScene() {
        return scene;
    }
    
    /**
     * Show character selection modal
     */
    private void showCharacterSelectionModal() {
        try {
            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle("Select Character");
            modalStage.initStyle(StageStyle.UTILITY);
            
            VBox modalContainer = new VBox(20);
            modalContainer.setAlignment(Pos.CENTER);
            modalContainer.setPadding(new Insets(30));
            modalContainer.setStyle("-fx-background-color: #2a2a2a;");
            
            Label titleLabel = new Label("SELECT YOUR CHARACTER");
            titleLabel.setTextFill(Color.WHITE);
            titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            
            HBox characterGrid = new HBox(20);
            characterGrid.setAlignment(Pos.CENTER);
            
            CharacterDAO characterDAO = new CharacterDAO();
            var characters = characterDAO.findAll();
            
            for (Character character : characters) {
                VBox characterCard = createModalCharacterCard(character, modalStage);
                characterGrid.getChildren().add(characterCard);
            }
            
            Button cancelButton = new Button("CANCEL");
            cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 10 20;");
            cancelButton.setFocusTraversable(false);
            cancelButton.setOnAction(e -> modalStage.close());
            
            modalContainer.getChildren().addAll(titleLabel, characterGrid, cancelButton);
            
            Scene modalScene = new Scene(modalContainer, 600, 300);
            modalScene.getStylesheets().add(getClass().getResource(Constants.CSS_FILE).toExternalForm());
            modalStage.setScene(modalScene);
            modalStage.showAndWait();
            
        } catch (DatabaseException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load characters");
            alert.setContentText("Unable to load character selection: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * Create character card for modal
     */
    private VBox createModalCharacterCard(Character character, Stage modalStage) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: rgba(60, 60, 60, 0.9); -fx-background-radius: 10; -fx-border-color: #444; -fx-border-width: 2; -fx-border-radius: 10;");
        card.setPrefWidth(120);
        card.setPrefHeight(200);
        
        // Highlight currently selected character
        if (selectedCharacter != null && selectedCharacter.getId() == character.getId()) {
            card.setStyle("-fx-background-color: rgba(76, 175, 80, 0.9); -fx-background-radius: 10; -fx-border-color: #4CAF50; -fx-border-width: 3; -fx-border-radius: 10;");
        }
        
        // Character image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(60);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);
        
        try {
            String previewPath = "/assets/characters/char" + character.getId() + "/idle_0.png";
            Image characterImage = new Image(getClass().getResourceAsStream(previewPath));
            if (characterImage != null && !characterImage.isError()) {
                imageView.setImage(characterImage);
            } else {
                Image fallbackImage = AssetLoader.loadImage(character.getImagePath());
                if (fallbackImage != null) {
                    imageView.setImage(fallbackImage);
                }
            }
        } catch (Exception e) {
            Image fallbackImage = AssetLoader.loadImage(character.getImagePath());
            if (fallbackImage != null) {
                imageView.setImage(fallbackImage);
            }
        }
        
        if (imageView.getImage() == null) {
            Rectangle placeholder = new Rectangle(60, 80);
            placeholder.setFill(AssetLoader.getFallbackColor(character.getImagePath()));
            placeholder.setStroke(Color.WHITE);
            placeholder.setStrokeWidth(2);
            card.getChildren().add(placeholder);
        } else {
            card.getChildren().add(imageView);
        }
        
        // Character name
        Label nameLabel = new Label(character.getName());
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        // Ability
        Label abilityLabel = new Label(character.getAbility());
        abilityLabel.setTextFill(Color.LIGHTBLUE);
        abilityLabel.setFont(Font.font("Arial", 10));
        
        // Select button
        Button selectButton = new Button("SELECT");
        selectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 5 10;");
        selectButton.setFocusTraversable(false);
        selectButton.setOnMouseClicked(e -> {
            e.consume();
            selectCharacter(character);
            modalStage.close();
        });
        
        card.getChildren().addAll(nameLabel, abilityLabel, selectButton);
        
        // Add mouse click handler to the entire card
        card.setOnMouseClicked(e -> {
            e.consume();
            selectCharacter(character);
            modalStage.close();
        });
        
        return card;
    }
    
    /**
     * Handle character selection
     */
    private void selectCharacter(Character character) {
        try {
            this.selectedCharacter = character;
            
            // Update GameState
            GameState.getInstance().setSelectedCharacter(character);
            
            // Update database
            UserDAO userDAO = new UserDAO();
            userDAO.updateLastSelectedCharacter(mainApp.getCurrentUser().getId(), character.getId());
            mainApp.getCurrentUser().setLastSelectedCharacterId(character.getId());
            
            // Refresh the UI
            refreshCharacterDisplay();
            
        } catch (DatabaseException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to save character selection");
            alert.setContentText("Unable to save your character choice: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * Refresh character display on the home screen
     */
    private void refreshCharacterDisplay() {
        // Update the character display components with the new selection
        // This ensures UI sync with the single source of truth (GameState)
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Character Selected");
        alert.setHeaderText("Character Updated");
        alert.setContentText("You have selected " + selectedCharacter.getName() + " with " + selectedCharacter.getAbility() + " ability!");
        alert.showAndWait();
        
        // Note: For a more sophisticated implementation, we would update the UI components
        // directly instead of showing a dialog. This would involve storing references
        // to the character display components and updating their values.
    }
}
