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
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardController {

    @FXML
    private Label usernameLabel;

    @FXML
    private ImageView profileImageView; // Profile Image View

    @FXML
    private Button uploadPictureButton; // Upload Button

    @FXML
    private void initialize() {
        // Get the current logged-in user
        String currentUser = SessionManager.getUser();
        if (currentUser != null) {
            usernameLabel.setText("Logged in as: " + currentUser);
            loadProfilePicture(currentUser);
        } else {
            usernameLabel.setText("Not logged in");
        }
    }

    // Method to load profile picture from database
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
                    applyCircularClip();  // Apply round frame after setting image
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Handle Upload Picture from Dashboard
    @FXML
    private void handleUploadPicture(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Picture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            String imagePath = selectedFile.toURI().toString();
            profileImageView.setImage(new Image(imagePath));

            // Save the image path to the database
            updateProfilePicture(imagePath);
        }
    }

    @FXML
    private void handleOpenChat(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("chat-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Chat Window");
            stage.setScene(new Scene(root, 400, 600));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Update profile picture in the database
    private void updateProfilePicture(String imagePath) {
        String query = "UPDATE users SET profile_picture = ? WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, imagePath);
            pstmt.setString(2, SessionManager.getUser());

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Profile picture updated successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not update profile picture.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Show alert method
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Handle Logout
    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clearSession();
        showAlert(Alert.AlertType.INFORMATION, "Logout", "You have been logged out.");
        SceneSwitcher.switchScene(event, "login-view.fxml");
    }

    // Handle Profile Settings Navigation
    @FXML
    private void handleProfileSettings(ActionEvent event) {
        SceneSwitcher.switchScene(event, "profile-view.fxml");
    }

    private void applyCircularClip() {
        double radius = Math.min(profileImageView.getFitWidth(), profileImageView.getFitHeight()) / 2;
        Circle clip = new Circle(radius);
        clip.setCenterX(profileImageView.getFitWidth() / 2);
        clip.setCenterY(profileImageView.getFitHeight() / 2);
        profileImageView.setClip(clip);
    }
}
