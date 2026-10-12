package com.zombie.ui;

import com.zombie.db.UserDAO;
import com.zombie.exception.AuthException;
import com.zombie.exception.DatabaseException;
import com.zombie.model.User;
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

/**
 * Login Screen for user authentication
 */
public class LoginScreen {
    
    private final MainApp mainApp;
    private Scene scene;
    
    public LoginScreen(MainApp mainApp) {
        this.mainApp = mainApp;
        createScene();
    }
    
    private void createScene() {
        // Main container with background
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(50));
        mainContainer.setStyle("-fx-background-color: #1a1a1a;");
        
        // Title
        Text title = new Text("ZOMBIE SURVIVAL");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setFill(Color.RED);
        
        Text subtitle = new Text("Login");
        subtitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        subtitle.setFill(Color.WHITE);
        
        // Form container
        VBox formContainer = new VBox(15);
        formContainer.setAlignment(Pos.CENTER);
        formContainer.setPadding(new Insets(30));
        formContainer.setStyle("-fx-background-color: rgba(40, 40, 40, 0.9); -fx-background-radius: 10;");
        formContainer.setMaxWidth(350);
        
        // Username field
        Label usernameLabel = new Label("Username:");
        usernameLabel.setTextFill(Color.LIGHTGRAY);
        usernameLabel.setFont(Font.font("Arial", 14));
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setMaxWidth(250);
        
        // Password field
        Label passwordLabel = new Label("Password:");
        passwordLabel.setTextFill(Color.LIGHTGRAY);
        passwordLabel.setFont(Font.font("Arial", 14));
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setMaxWidth(250);
        
        // Error label
        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setFont(Font.font("Arial", 12));
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(250);
        
        // Buttons
        Button loginButton = new Button("Login");
        loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 10 20;");
        loginButton.setMaxWidth(250);
        
        Button signUpButton = new Button("Create Account");
        signUpButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 10 20;");
        signUpButton.setMaxWidth(250);
        
        // Add components to form
        formContainer.getChildren().addAll(
            usernameLabel, usernameField,
            passwordLabel, passwordField,
            errorLabel,
            loginButton, signUpButton
        );
        
        // Add everything to main container
        mainContainer.getChildren().addAll(title, subtitle, formContainer);
        
        // Event handlers
        loginButton.setOnAction(e -> handleLogin(
            usernameField.getText(),
            passwordField.getText(),
            errorLabel
        ));
        
        signUpButton.setOnAction(e -> mainApp.showSignUpScreen());
        
        // Enter key handler for form submission
        Runnable submitAction = () -> handleLogin(
            usernameField.getText(),
            passwordField.getText(),
            errorLabel
        );
        
        usernameField.setOnAction(e -> submitAction.run());
        passwordField.setOnAction(e -> submitAction.run());
        
        // Create scene
        scene = new Scene(mainContainer, 800, 600);
        try {
            var cssResource = getClass().getResource(Constants.CSS_FILE);
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }
        } catch (Exception ex) {
            System.err.println("CSS not found: " + ex.getMessage());
        }
    }
    
    private void handleLogin(String username, String password, Label errorLabel) {
        errorLabel.setText("");
        
        // Validation
        if (username == null || username.trim().isEmpty()) {
            errorLabel.setText("Username cannot be empty");
            return;
        }
        
        if (password == null || password.isEmpty()) {
            errorLabel.setText("Password cannot be empty");
            return;
        }
        
        // Authenticate user
        try {
            UserDAO userDAO = new UserDAO();
            User user = userDAO.authenticate(username.trim(), password);
            
            // Set current user in main app
            mainApp.setCurrentUser(user);
            
            // Check if user has selected a character
            if (user.getCharacterId() == null) {
                mainApp.showCharacterSelectScreen();
            } else {
                mainApp.showHomeScreen();
            }
            
        } catch (DatabaseException e) {
            errorLabel.setText("Database error: " + e.getMessage());
        } catch (AuthException e) {
            errorLabel.setText(e.getMessage());
        }
    }
    
    public Scene getScene() {
        return scene;
    }
}
