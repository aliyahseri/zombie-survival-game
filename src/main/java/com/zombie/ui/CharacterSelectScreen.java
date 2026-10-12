package com.zombie.ui;


import com.zombie.db.CharacterDAO;
import com.zombie.db.UserDAO;
import com.zombie.exception.DatabaseException;
import com.zombie.model.Character;
import com.zombie.util.AssetLoader;
import com.zombie.util.Constants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;

/**
 * Character Select Screen for choosing player character
 */
public class CharacterSelectScreen {
    
    private final MainApp mainApp;
    private Scene scene;
    private Character selectedCharacter;
    private final CharacterDAO characterDAO;
    private final UserDAO userDAO;
    
    public CharacterSelectScreen(MainApp mainApp) throws DatabaseException {
        this.mainApp = mainApp;
        this.characterDAO = new CharacterDAO();
        this.userDAO = new UserDAO();
        
        // Initialize default characters if needed
        characterDAO.initializeDefaultCharacters();
        
        createScene();
    }
    
    private void createScene() {
        // Main container with background
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setStyle("-fx-background-color: #1a1a1a;");
        
        // Title
        Text title = new Text("SELECT YOUR CHARACTER");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setFill(Color.RED);
        
        // Instructions
        Text instructions = new Text("Choose your survivor for the zombie apocalypse");
        instructions.setFont(Font.font("Arial", 16));
        instructions.setFill(Color.LIGHTGRAY);
        
        // Character grid container
        GridPane characterGrid = new GridPane();
        characterGrid.setAlignment(Pos.CENTER);
        characterGrid.setHgap(20);
        characterGrid.setVgap(20);
        characterGrid.setPadding(new Insets(20));
        
        // Load characters from database
        try {
            List<Character> characters = characterDAO.findAll();
            
            for (int i = 0; i < characters.size(); i++) {
                Character character = characters.get(i);
                VBox characterCard = createCharacterCard(character);
                
                int row = i / 2;
                int col = i % 2;
                characterGrid.add(characterCard, col, row);
            }
            
        } catch (DatabaseException e) {
            // Show error if can't load characters
            Label errorLabel = new Label("Error loading characters: " + e.getMessage());
            errorLabel.setTextFill(Color.RED);
            characterGrid.add(errorLabel, 0, 0);
        }
        
        // Buttons container
        HBox buttonContainer = new HBox(20);
        buttonContainer.setAlignment(Pos.CENTER);
        
        Button confirmButton = new Button("Confirm Selection");
        confirmButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 12 24;");
        confirmButton.setDisable(true); // Enable only when character is selected
        
        Button backButton = new Button("Back");
        backButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 12 24;");
        
        buttonContainer.getChildren().addAll(backButton, confirmButton);
        
        // Add everything to main container
        mainContainer.getChildren().addAll(title, instructions, characterGrid, buttonContainer);
        
        // Event handlers
        confirmButton.setOnAction(e -> handleCharacterSelection());
        backButton.setOnAction(e -> mainApp.showLoginScreen());
        
        // Create scene
        scene = new Scene(mainContainer, 900, 700);
        scene.getStylesheets().add(getClass().getResource(Constants.CSS_FILE).toExternalForm());
    }
    
    private VBox createCharacterCard(Character character) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: rgba(60, 60, 60, 0.9); -fx-background-radius: 10; -fx-border-color: #444; -fx-border-width: 2; -fx-border-radius: 10;");
        card.setPrefWidth(220);
        card.setPrefHeight(280);
        
        // Character image or placeholder
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        
        // Try to load sprite preview first
        String previewPath = "/assets/characters/char" + character.getId() + "/idle_0.png";
        Image characterImage = null;
        try {
            characterImage = new Image(getClass().getResourceAsStream(previewPath));
        } catch (Exception e) {
            // Fallback to old image path
            characterImage = AssetLoader.loadImage(character.getImagePath());
        }
        
        if (characterImage != null) {
            imageView.setImage(characterImage);
            card.getChildren().add(imageView);
        } else {
            // Create colored rectangle as placeholder
            Rectangle placeholder = new Rectangle(120, 180);
            placeholder.setFill(AssetLoader.getFallbackColor(character.getImagePath()));
            placeholder.setStroke(Color.WHITE);
            placeholder.setStrokeWidth(2);
            card.getChildren().add(placeholder);
        }
        
        // Character name
        Label nameLabel = new Label(character.getName());
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        // Ability description with background for legibility
        String abilityText = getAbilityDescription(character.getAbility());
        Label abilityLabel = new Label(abilityText);
        abilityLabel.setTextFill(Color.LIGHTGRAY);
        abilityLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        abilityLabel.setWrapText(true);
        abilityLabel.setMaxWidth(180);
        abilityLabel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-background-radius: 5; -fx-padding: 5;");
        
        // Selection indicator
        Label selectedLabel = new Label("✓ SELECTED");
        selectedLabel.setTextFill(Color.GREEN);
        selectedLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        selectedLabel.setVisible(false);
        
        card.getChildren().addAll(nameLabel, abilityLabel, selectedLabel);
        
        // Click handler
        card.setOnMouseClicked(e -> {
            // Clear previous selection
            if (selectedCharacter != null) {
                clearPreviousSelection();
            }
            
            // Set new selection
            selectedCharacter = character;
            card.setStyle("-fx-background-color: rgba(76, 175, 80, 0.9); -fx-background-radius: 10; -fx-border-color: #4CAF50; -fx-border-width: 3; -fx-border-radius: 10;");
            selectedLabel.setVisible(true);
            
            // Enable confirm button
            Button confirmButton = (Button) ((HBox) ((VBox) scene.getRoot()).getChildren().get(3)).getChildren().get(1);
            confirmButton.setDisable(false);
        });
        
        // Hover effect
        card.setOnMouseEntered(e -> {
            if (selectedCharacter == null || !selectedCharacter.equals(character)) {
                card.setStyle("-fx-background-color: rgba(80, 80, 80, 0.9); -fx-background-radius: 10; -fx-border-color: #666; -fx-border-width: 2; -fx-border-radius: 10;");
            }
        });
        
        card.setOnMouseExited(e -> {
            if (selectedCharacter == null || !selectedCharacter.equals(character)) {
                card.setStyle("-fx-background-color: rgba(60, 60, 60, 0.9); -fx-background-radius: 10; -fx-border-color: #444; -fx-border-width: 2; -fx-border-radius: 10;");
            }
        });
        
        return card;
    }
    
    private void clearPreviousSelection() {
        GridPane characterGrid = (GridPane) ((VBox) scene.getRoot()).getChildren().get(2);
        for (javafx.scene.Node node : characterGrid.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                card.setStyle("-fx-background-color: rgba(60, 60, 60, 0.9); -fx-background-radius: 10; -fx-border-color: #444; -fx-border-width: 2; -fx-border-radius: 10;");
                
                // Hide selected label
                for (javafx.scene.Node child : card.getChildren()) {
                    if (child instanceof Label && ((Label) child).getText().equals("✓ SELECTED")) {
                        ((Label) child).setVisible(false);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Get ability name for display
     */
    private String getAbilityDescription(String ability) {
        if (ability == null) return "No special ability";
        
        // Convert ability codes to user-friendly names
        switch (ability) {
            case "SHIELD" -> { return "Shield: +200 HP for 7s"; }
            case "SPEED_BOOST" -> { return "Speed Boost: 2x speed for 7s"; }
            case "TRIPLE_SHOT" -> { return "Triple Shot: 3 bullets for 7s"; }
            default -> { return ability; }
        }
    }
    
    private void handleCharacterSelection() {
        if (selectedCharacter == null) {
            return;
        }
        
        try {
            // Update user's character selection in database
            userDAO.updateCharacter(mainApp.getCurrentUser().getId(), selectedCharacter.getId());
            mainApp.getCurrentUser().setCharacterId(selectedCharacter.getId());
            
            // Navigate to home screen
            mainApp.showHomeScreen();
            
        } catch (DatabaseException e) {
            Alert alert = new Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to save character selection");
            alert.setContentText("There was an error saving your character selection: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    public Scene getScene() {
        return scene;
    }
}
