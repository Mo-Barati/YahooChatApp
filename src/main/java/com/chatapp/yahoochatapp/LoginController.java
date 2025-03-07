package com.chatapp.yahoochatapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button signupButton;

    @FXML
    private Button logoutButton;

    @FXML
    private void initialize() {
        loginButton.setOnAction(event -> handleLogin(event));
        signupButton.setOnAction(event -> handleSignup(event));
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Check if fields are empty
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Username and Password cannot be empty!");
            return;
        }

        // Check credentials in the database
        if (validateLogin(username, password)) {
            // 🔥 Store the logged-in user in SessionManager
            SessionManager.setUser(username);

            showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome, " + username + "!");
        } else {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
        }
    }

    private boolean validateLogin(String username, String password) {
        String query = "SELECT password FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) { // User found
                String storedHashedPassword = rs.getString("password");

                // Compare the hashed password with the entered plain text password
                if (BCrypt.checkpw(password, storedHashedPassword)) {
                    return true; // Password matches
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false; // User not found or incorrect password
    }

    @FXML
    private void handleSignup(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Check if fields are empty
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Sign-Up Failed", "Username and Password cannot be empty!");
            return;
        }

        // Check if username already exists
        if (isUsernameTaken(username)) {
            showAlert(Alert.AlertType.ERROR, "Sign-Up Failed", "Username is already taken. Try another.");
            return;
        }

        // Hash the password before saving (security best practice)
        String hashedPassword = hashPassword(password);

        // Insert user into the database
        if (registerUser(username, hashedPassword)) {
            showAlert(Alert.AlertType.INFORMATION, "Sign-Up Successful", "Account created! You can now log in.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Sign-Up Failed", "Something went wrong. Try again.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean registerUser(String username, String hashedPassword) {
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            int rowsInserted = pstmt.executeUpdate();

            return rowsInserted > 0; // If at least one row was inserted, return true

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isUsernameTaken(String username) {
        String query = "SELECT username FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // If there is a result, the username is already taken

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    private String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12)); // Hash the password with salt
    }


    @FXML
    private void handleLogout(ActionEvent event) {
        // Clear the current user session
        SessionManager.clearSession();

        // Show a confirmation alert
        showAlert(Alert.AlertType.INFORMATION, "Logout", "You have been logged out.");

        // Ensure fields are cleared
        usernameField.setText("");
        passwordField.setText("");

        // Debugging: Print session status
        System.out.println("User logged in? " + SessionManager.isUserLoggedIn());
    }


}
