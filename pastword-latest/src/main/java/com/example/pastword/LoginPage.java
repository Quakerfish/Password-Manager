package com.example.pastword;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

// Renders the login screen and handles the pre-entry pin verification popup.
public class LoginPage extends AuthPageBase {

    private static final int PIN_LENGTH = 5;

    private final PastwordApp app;
    private final String infoMessage;

    public LoginPage(PastwordApp app, String infoMessage) {
        super("Login");
        this.app = app;
        this.infoMessage = infoMessage;
    }

    @Override
    protected Node buildForm() {
        StackPane wrapper = new StackPane();
        wrapper.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox form = createCenteredFormBox("");
        form.setSpacing(16);

        Node brand = BrandAssetFactory.createAuthLogo();

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

            PastwordApp.LoginAttempt attempt = app.attemptLogin(
                    identityGroup.input().getText().trim(),
                    passwordGroup.input().getText()
            );
            if (attempt.destination() == PastwordApp.LoginDestination.INVALID) {
                showStatus(statusLabel, attempt.message(), true);
            } else if (attempt.destination() == PastwordApp.LoginDestination.ADMIN) {
                app.showAdminPage();
            } else {
                showPinOverlay(wrapper);
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

        wrapper.getChildren().add(form);
        return wrapper;
    }

    private Button createPrimaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(40);
        return button;
    }

    private Button createPopupButton(String text, String variantClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll("overlay-button", variantClass);
        return button;
    }

    // Shows the login pin popup once the username/email and password are correct.
    private void showPinOverlay(StackPane wrapper) {
        VBox card = createOverlayCard("PIN-CODE");
        HBox pinRow = new HBox(12);
        pinRow.setAlignment(javafx.geometry.Pos.CENTER);
        List<TextField> pinFields = createPinFields(pinRow);

        Label errorLabel = createStatusLabel();
        errorLabel.getStyleClass().add("overlay-error");
        errorLabel.setMaxWidth(280);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button cancelButton = createPopupButton("Back", "overlay-button-secondary");
        cancelButton.setOnAction(event -> wrapper.getChildren().removeIf(node -> node.getStyleClass().contains("overlay-backdrop")));

        Button submitButton = createPopupButton("Submit", "overlay-button-primary");
        submitButton.setOnAction(event -> attemptPinLogin(pinFields, errorLabel));

        HBox actions = new HBox(12, cancelButton, submitButton);
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        card.getChildren().addAll(pinRow, errorLabel, actions);

        StackPane backdrop = new StackPane(card);
        backdrop.getStyleClass().add("overlay-backdrop");
        backdrop.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        wrapper.getChildren().removeIf(node -> node.getStyleClass().contains("overlay-backdrop"));
        wrapper.getChildren().add(backdrop);

        pinFields.get(PIN_LENGTH - 1).textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isBlank() && readPin(pinFields).length() == PIN_LENGTH) {
                Platform.runLater(submitButton::fire);
            }
        });
        Platform.runLater(() -> pinFields.get(0).requestFocus());
    }

    // Completes the login when the correct 5-digit pin is entered.
    private void attemptPinLogin(List<TextField> pinFields, Label errorLabel) {
        String pin = readPin(pinFields);
        if (pin.length() != PIN_LENGTH) {
            showStatus(errorLabel, "Enter your 5-digit pin code.", true);
            return;
        }
        if (!app.completePendingLogin(pin)) {
            showStatus(errorLabel, "Incorrect pin. Try again.", true);
            clearPinFields(pinFields);
            Platform.runLater(() -> pinFields.get(0).requestFocus());
        }
    }

    // Builds the popup card used by the login pin prompt.
    private VBox createOverlayCard(String title) {
        VBox card = new VBox(18);
        card.getStyleClass().addAll("overlay-card", "pin-overlay-card");
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPrefHeight(Region.USE_COMPUTED_SIZE);
        card.setMaxWidth(430);
        card.setPrefWidth(430);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("overlay-title");
        card.getChildren().add(titleLabel);
        return card;
    }

    // Creates the digit inputs used by the login pin popup.
    private List<TextField> createPinFields(HBox pinRow) {
        List<TextField> fields = new ArrayList<>();
        for (int index = 0; index < PIN_LENGTH; index++) {
            final int currentIndex = index;
            TextField pinField = new TextField();
            pinField.getStyleClass().add("pin-box");
            pinField.setPrefSize(48, 48);
            pinField.setMaxSize(48, 48);
            pinField.setAlignment(javafx.geometry.Pos.CENTER);
            pinField.textProperty().addListener((observable, oldValue, newValue) -> {
                String sanitized = newValue.replaceAll("[^0-9]", "");
                if (sanitized.length() > 1) {
                    sanitized = sanitized.substring(0, 1);
                }
                if (!sanitized.equals(newValue)) {
                    pinField.setText(sanitized);
                    return;
                }
                if (!sanitized.isEmpty() && !sanitized.equals(oldValue) && currentIndex < PIN_LENGTH - 1) {
                    TextField nextField = fields.get(currentIndex + 1);
                    nextField.requestFocus();
                    nextField.selectAll();
                }
            });
            pinField.focusedProperty().addListener((observable, oldValue, focused) -> {
                if (focused) {
                    pinField.selectAll();
                }
            });
            fields.add(pinField);
            pinRow.getChildren().add(pinField);
        }
        return fields;
    }

    private void clearPinFields(List<TextField> fields) {
        for (TextField field : fields) {
            field.clear();
        }
    }

    private String readPin(List<TextField> fields) {
        StringBuilder builder = new StringBuilder();
        for (TextField field : fields) {
            builder.append(field.getText().trim());
        }
        return builder.toString();
    }
}
