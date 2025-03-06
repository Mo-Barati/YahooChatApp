package com.chatapp.yahoochatapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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
    private void initialize() {
        loginButton.setOnAction(this::handleLogin);
        signupButton.setOnAction(this::handleSignup);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        System.out.println("Login Attempt: " + username + " / " + password);

        // TODO: Add database authentication logic
    }

    @FXML
    private void handleSignup(ActionEvent event) {
        System.out.println("Redirecting to Signup...");
        // TODO: Add navigation logic to the signup screen
    }
}
