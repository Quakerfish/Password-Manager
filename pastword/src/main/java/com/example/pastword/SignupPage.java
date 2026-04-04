package com.example.pastword;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SignupPage extends AuthPageBase {

    private final PastwordApp app;
    private final String infoMessage;

    public SignupPage(PastwordApp app, String infoMessage) {
        super("Signup");
        this.app = app;
        this.infoMessage = infoMessage;
    }

    @Override
    protected Node buildForm() {
        VBox form = createCenteredFormBox("Create an Account");
        form.setSpacing(14);

        Label brand = new Label("PASTWORD");
        brand.getStyleClass().add("brand-title");

        FieldGroup emailGroup = createFieldGroup("Email", new javafx.scene.control.TextField());
        emailGroup.input().setPromptText("Enter your email");

        FieldGroup usernameGroup = createFieldGroup("Username", new javafx.scene.control.TextField());
        usernameGroup.input().setPromptText("Choose a username");

        FieldGroup passwordGroup = createPasswordGroup("Password", "Create a password");
        FieldGroup confirmGroup = createPasswordGroup("Confirm Password", "Confirm your password");

        PinGroup pinGroup = createPinGroup("Pin-Code", 5);
        Label statusLabel = createStatusLabel();

        if (infoMessage != null && !infoMessage.isBlank()) {
            showStatus(statusLabel, infoMessage, false);
        }

        Button registerButton = createPrimaryButton("Register");
        registerButton.setOnAction(event -> {
            hideStatus(statusLabel);
            clearFieldError(emailGroup);
            clearFieldError(usernameGroup);
            clearFieldError(passwordGroup);
            clearFieldError(confirmGroup);
            clearPinError(pinGroup);

            boolean hasError = false;
            String email = emailGroup.input().getText().trim();
            String username = usernameGroup.input().getText().trim();
            String password = passwordGroup.input().getText();
            String confirmPassword = confirmGroup.input().getText();
            String pin = readPin(pinGroup);

            if (email.isEmpty()) {
                setFieldError(emailGroup, "This field is required.");
                hasError = true;
            } else if (!email.contains("@") || !email.contains(".")) {
                setFieldError(emailGroup, "Enter a valid email address.");
                hasError = true;
            }

            if (username.isEmpty()) {
                setFieldError(usernameGroup, "This field is required.");
                hasError = true;
            }

            if (password.isEmpty()) {
                setFieldError(passwordGroup, "This field is required.");
                hasError = true;
            } else if (password.length() < 6) {
                setFieldError(passwordGroup, "Password must be at least 6 characters.");
                hasError = true;
            }

            if (confirmPassword.isEmpty()) {
                setFieldError(confirmGroup, "This field is required.");
                hasError = true;
            } else if (!confirmPassword.equals(password)) {
                setFieldError(confirmGroup, "Passwords do not match.");
                hasError = true;
            }

            if (pin.length() != 5) {
                setPinError(pinGroup, "Enter your 5-digit pin code.");
                hasError = true;
            }

            if (hasError) {
                showStatus(statusLabel, "Please fix the required fields before registering.", true);
                return;
            }

            String errorMessage = app.attemptSignup(email, username, password, pin);
            if (errorMessage != null) {
                showStatus(statusLabel, errorMessage, true);
            }
        });

        form.getChildren().addAll(
                brand,
                emailGroup.container(),
                usernameGroup.container(),
                passwordGroup.container(),
                confirmGroup.container(),
                pinGroup.container(),
                statusLabel,
                registerButton,
                createFooterText("Already signed-up? ", "Login", () -> app.showLoginPage(null))
        );

        return form;
    }

    private Button createPrimaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(40);
        return button;
    }
}
