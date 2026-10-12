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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Sign Up Screen for user registration
 */
public class SignUpScreen {
    
    private final MainApp mainApp;
    private Scene scene;
    
    public SignUpScreen(MainApp mainApp) {
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
        
        Text subtitle = new Text("Create Account");
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
        passwordField.setPromptText("Enter password (min 6 chars)");
        passwordField.setMaxWidth(250);
        
        // Confirm password field
        Label confirmPasswordLabel = new Label("Confirm Password:");
        confirmPasswordLabel.setTextFill(Color.LIGHTGRAY);
        confirmPasswordLabel.setFont(Font.font("Arial", 14));
        
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm password");
        confirmPasswordField.setMaxWidth(250);
        
        // Error label
        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setFont(Font.font("Arial", 12));
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(250);
        
        // Buttons
        Button signUpButton = new Button("Sign Up");
        signUpButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 10 20;");
        signUpButton.setMaxWidth(250);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 10 20;");
        cancelButton.setMaxWidth(250);
        
        // Login link
        Hyperlink loginLink = new Hyperlink("Already have an account? Login here");
        loginLink.setTextFill(Color.LIGHTBLUE);
        
        // Add components to form
        formContainer.getChildren().addAll(
            usernameLabel, usernameField,
            passwordLabel, passwordField,
            confirmPasswordLabel, confirmPasswordField,
            errorLabel,
            signUpButton, cancelButton,
            loginLink
        );
        
        // Add everything to main container
        mainContainer.getChildren().addAll(title, subtitle, formContainer);
        
        // Event handlers
        signUpButton.setOnAction(e -> handleSignUp(
            usernameField.getText(),
            passwordField.getText(),
            confirmPasswordField.getText(),
            errorLabel
        ));
        
        cancelButton.setOnAction(e -> mainApp.showLoginScreen());
        
        loginLink.setOnAction(e -> mainApp.showLoginScreen());
        
        // Enter key handler for form submission
        Runnable submitAction = () -> handleSignUp(
            usernameField.getText(),
            passwordField.getText(),
            confirmPasswordField.getText(),
            errorLabel
        );
        
        usernameField.setOnAction(e -> submitAction.run());
        passwordField.setOnAction(e -> submitAction.run());
        confirmPasswordField.setOnAction(e -> submitAction.run());
        
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
    
    private void handleSignUp(String username, String password, String confirmPassword, Label errorLabel) {
        errorLabel.setText("");
    
        if (username == null || username.trim().isEmpty()) {
            errorLabel.setText("Username cannot be empty");
            return;
        }
        
        if (username.length() < 3) {
            errorLabel.setText("Username must be at least 3 characters");
            return;
        }
        
        if (password == null || password.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match");
            return;
        }
        
        try {
            UserDAO userDAO = new UserDAO();
            User user = new User(username.trim(), password);
            userDAO.create(user);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("Account Created!");
            alert.setContentText("Your account has been created. Please login.");
            alert.showAndWait();
            
            mainApp.showLoginScreen();
            
        } catch (DatabaseException e) {
            e.printStackTrace(); // Print full error to terminal
            errorLabel.setText("Database error: " + e.getMessage());
        } catch (AuthException e) {
            e.printStackTrace();
            errorLabel.setText(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // Catch ANY other hidden exception
            errorLabel.setText("Unexpected error: " + e.getMessage());
        }
    }
    
    public Scene getScene() {
        return scene;
    }
}
