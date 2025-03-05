package com.chatapp.yahoochatapp;

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
        loginButton.setOnAction(event -> handleLogin());
        signupButton.setOnAction(event -> handleSignup());
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        System.out.println("Login Attempt: " + username + " / " + password);
        // Here we will later connect to MySQL
    }

    private void handleSignup() {
        System.out.println("Redirecting to Signup...");
        // Later, we will add navigation to the signup screen
    }
}
