package com.chatapp.yahoochatapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ProfileController {

    @FXML
    private TextField newUsernameField;

    @FXML
    private PasswordField newPasswordField;

    // Handle Update Profile
    @FXML
    private void handleUpdateProfile(ActionEvent event) {
        String newUsername = newUsernameField.getText();
        String newPassword = newPasswordField.getText();

        if (newUsername.isEmpty() || newPassword.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Update Failed", "Fields cannot be empty.");
            return;
        }

        if (updateUserProfile(newUsername, newPassword)) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not update profile.");
        }
    }

    // Update the user profile in the database
    private boolean updateUserProfile(String newUsername, String newPassword) {
        String query = "UPDATE users SET username = ?, password = ? WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, newUsername);
            pstmt.setString(2, hashPassword(newPassword)); // FIXED: Hash the password
            pstmt.setString(3, SessionManager.getUser());  // FIXED: Use getUser()

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                SessionManager.setUser(newUsername); // FIXED: Use setUser()
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
