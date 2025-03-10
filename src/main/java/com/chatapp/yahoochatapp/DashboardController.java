package com.chatapp.yahoochatapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardController {

    @FXML
    private Button logoutButton;

    @FXML
    private Label usernameLabel; // Link with FXML Label

    @FXML
    private ImageView profileImageView;

    @FXML
    private void initialize() {
        String currentUser = SessionManager.getUser();
        if (currentUser != null) {
            usernameLabel.setText("Logged in as: " + currentUser);
            loadProfilePicture(currentUser);
        } else {
            usernameLabel.setText("Not logged in");
        }
    }

    private void loadProfilePicture(String username) {
        String query = "SELECT profile_picture FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String imagePath = rs.getString("profile_picture");
                if (imagePath != null && !imagePath.isEmpty()) {
                    profileImageView.setImage(new Image(imagePath));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        // Clear the current user session
        SessionManager.clearSession();
        showAlert(Alert.AlertType.INFORMATION, "Logout", "You have been logged out.");

        // Close the current dashboard window
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        stage.close();

        // Load the login screen with the **original size** (400x800)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chatapp/yahoochatapp/login-view.fxml"));
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.setTitle("Login - Chat App");
            loginStage.setScene(new Scene(root, 400, 800)); // ✅ Set original size
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleProfileSettings(ActionEvent event) {
        SceneSwitcher.switchScene(event, "profile-view.fxml");
    }


    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
