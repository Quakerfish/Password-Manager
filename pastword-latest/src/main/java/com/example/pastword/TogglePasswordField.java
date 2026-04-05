package com.example.pastword;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

// Reusable password input that keeps the eye icon inside the field boundary.
public class TogglePasswordField extends StackPane {

    private final PasswordField hiddenField = new PasswordField();
    private final TextField visibleField = new TextField();

    // Creates a password field pair that can switch between hidden and visible text.
    public TogglePasswordField(String styleClass, String promptText, double width) {
        hiddenField.getStyleClass().add(styleClass);
        hiddenField.getStyleClass().add("toggle-password-input");
        hiddenField.setPromptText(promptText);
        hiddenField.setPrefWidth(width);
        hiddenField.setMaxWidth(width);

        visibleField.getStyleClass().add(styleClass);
        visibleField.getStyleClass().add("toggle-password-input");
        visibleField.setPromptText(promptText);
        visibleField.setPrefWidth(width);
        visibleField.setMaxWidth(width);
        visibleField.setVisible(false);
        visibleField.setManaged(false);
        visibleField.textProperty().bindBidirectional(hiddenField.textProperty());

        Button toggleButton = SvgIconFactory.createInlineEyeButton(false);
        toggleButton.setOnAction(event -> {
            boolean showing = visibleField.isVisible();
            visibleField.setVisible(!showing);
            visibleField.setManaged(!showing);
            hiddenField.setVisible(showing);
            hiddenField.setManaged(showing);
            SvgIconFactory.setEyeGraphic(toggleButton, !showing);
            TextField activeField = showing ? hiddenField : visibleField;
            activeField.requestFocus();
            activeField.end();
        });

        getChildren().addAll(hiddenField, visibleField, toggleButton);
        setAlignment(toggleButton, Pos.CENTER_RIGHT);
        setMargin(toggleButton, new Insets(0, 10, 0, 0));
        setMaxWidth(width);
    }

    // Returns the current password text from the shared field value.
    public String getText() {
        return hiddenField.getText();
    }

    // Replaces the current password text in both hidden and visible states.
    public void setText(String text) {
        hiddenField.setText(text == null ? "" : text);
    }

    // Clears the current password text from the control.
    public void clear() {
        hiddenField.clear();
    }
}
