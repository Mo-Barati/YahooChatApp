package com.chatapp.yahoochatapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ProfileController {

    @FXML
    private TextField newUsernameField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private ImageView profileImageView; // Profile picture display

    @FXML
    private Button uploadPictureButton;

    private String profileImagePath = null; // Store image file path

    @FXML
    private void initialize() {
        String profilePic = SessionManager.getProfilePicture();
        if (profilePic != null && !profilePic.isEmpty()) {
            profileImageView.setImage(new Image(profilePic));
        }
    }


    // Handle Profile Picture Upload
    @FXML
    private void handleUploadPicture(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Picture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            profileImagePath = selectedFile.toURI().toString();
            profileImageView.setImage(new Image(profileImagePath));
        }
    }

    // Handle Update Profile
    @FXML
    private void handleUpdateProfile(ActionEvent event) {
        String newUsername = newUsernameField.getText().trim();
        String newPassword = newPasswordField.getText().trim();
        boolean isProfileUpdated = false;

        // Update profile picture if changed
        if (profileImagePath != null) {
            if (updateProfilePicture(profileImagePath)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Profile picture updated successfully.");
                isProfileUpdated = true;
            } else {
                showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not update profile picture.");
            }
        }

        // Update username/password only if provided
        if (!newUsername.isEmpty() || !newPassword.isEmpty()) {
            if (updateUserProfile(newUsername, newPassword)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Profile details updated successfully.");
                isProfileUpdated = true;
            } else {
                showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not update profile details.");
            }
        }

        // If no update was done, show a message
        if (!isProfileUpdated) {
            showAlert(Alert.AlertType.WARNING, "No Changes", "No changes were made.");
        }
    }

    // Update only the profile picture
    private boolean updateProfilePicture(String imagePath) {
        String query = "UPDATE users SET profile_picture = ? WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, imagePath);
            pstmt.setString(2, SessionManager.getUser()); // Use logged-in user

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Update the user profile in the database
    private boolean updateUserProfile(String newUsername, String newPassword) {
        boolean usernameChanged = !newUsername.isEmpty();
        boolean passwordChanged = !newPassword.isEmpty();

        // If neither username nor password is changed, return false
        if (!usernameChanged && !passwordChanged) {
            return false;
        }

        StringBuilder query = new StringBuilder("UPDATE users SET ");
        boolean needComma = false;

        if (usernameChanged) {
            query.append("username = ?");
            needComma = true;
        }

        if (passwordChanged) {
            if (needComma) query.append(", ");
            query.append("password = ?");
        }

        query.append(" WHERE username = ?");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query.toString())) {

            int index = 1;
            if (usernameChanged) {
                pstmt.setString(index++, newUsername);
            }
            if (passwordChanged) {
                pstmt.setString(index++, hashPassword(newPassword));
            }
            pstmt.setString(index, SessionManager.getUser()); // Where clause

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                if (usernameChanged) {
                    SessionManager.setUser(newUsername); // Update session username
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Hash password using BCrypt
    private String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    // Show alert method
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Handle Back button
    @FXML
    private void handleBack(ActionEvent event) {
        SceneSwitcher.switchScene(event, "dashboard-view.fxml");
    }
}
