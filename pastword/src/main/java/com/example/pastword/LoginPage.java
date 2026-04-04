package com.example.pastword;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class LoginPage extends AuthPageBase {

    private final PastwordApp app;
    private final String infoMessage;

    public LoginPage(PastwordApp app, String infoMessage) {
        super("Login");
        this.app = app;
        this.infoMessage = infoMessage;
    }

    @Override
    protected Node buildForm() {
        VBox form = createCenteredFormBox("");
        form.setSpacing(16);

        Label brand = new Label("PASTWORD");
        brand.getStyleClass().add("brand-title");

        FieldGroup identityGroup = createFieldGroup("Username/Email", new javafx.scene.control.TextField());
        identityGroup.input().setPromptText("Enter your username or email");

        FieldGroup passwordGroup = createPasswordGroup("Password", "Enter your password");
        Label statusLabel = createStatusLabel();

        if (infoMessage != null && !infoMessage.isBlank()) {
            showStatus(statusLabel, infoMessage, false);
        }

        Button loginButton = createPrimaryButton("Login");
        loginButton.setOnAction(event -> {
            hideStatus(statusLabel);
            clearFieldError(identityGroup);
            clearFieldError(passwordGroup);

            boolean hasError = false;
            if (identityGroup.input().getText().trim().isEmpty()) {
                setFieldError(identityGroup, "This field is required.");
                hasError = true;
            }
            if (passwordGroup.input().getText().trim().isEmpty()) {
                setFieldError(passwordGroup, "This field is required.");
                hasError = true;
            }

            if (hasError) {
                showStatus(statusLabel, "Please fix the required fields.", true);
                return;
            }

            String errorMessage = app.attemptLogin(
                    identityGroup.input().getText().trim(),
                    passwordGroup.input().getText()
            );
            if (errorMessage != null) {
                showStatus(statusLabel, errorMessage, true);
            }
        });

        form.getChildren().addAll(
                brand,
                identityGroup.container(),
                passwordGroup.container(),
                statusLabel,
                loginButton,
                createFooterText("Don't have an account? ", "Sign up", () -> app.showSignupPage(null))
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
