package com.example.pastword;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;

abstract class AuthPageBase extends StackPane {

    private static final PseudoClass ERROR_CLASS = PseudoClass.getPseudoClass("error");

    protected AuthPageBase(String pageName) {
        getStyleClass().add("app-root");

        StackPane card = new StackPane();
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(940);
        card.setPrefHeight(650);

        Pane decorationLayer = createDecorationLayer();
        VBox content = createContent(pageName);

        card.getChildren().addAll(decorationLayer, content);
        getChildren().add(card);
    }

    private VBox createContent(String pageName) {
        VBox wrapper = new VBox(18);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(18, 34, 28, 34));
        wrapper.setFillWidth(true);

        Label header = new Label(pageName);
        header.getStyleClass().add("page-caption");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER_LEFT);

        Region topSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);

        Node form = buildForm();

        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        wrapper.getChildren().addAll(header, topSpacer, form, bottomSpacer);
        return wrapper;
    }

    protected VBox createCenteredFormBox(String title) {
        VBox form = new VBox(14);
        form.setAlignment(Pos.TOP_CENTER);
        form.setMaxWidth(420);
        form.setPadding(new Insets(10, 0, 0, 0));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("form-title");

        form.getChildren().add(titleLabel);
        return form;
    }

    protected FieldGroup createFieldGroup(String labelText, TextField field) {
        VBox block = new VBox(6);
        block.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");

        field.getStyleClass().add("auth-input");
        field.setPrefHeight(38);
        field.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = createErrorLabel();
        block.getChildren().addAll(label, field, errorLabel);
        return new FieldGroup(block, field, errorLabel);
    }

    protected FieldGroup createPasswordGroup(String labelText, String promptText) {
        VBox block = new VBox(6);
        block.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");

        PasswordField passwordField = new PasswordField();
        passwordField.getStyleClass().add("auth-input");
        passwordField.getStyleClass().add("auth-password-input");
        passwordField.setPromptText(promptText);
        passwordField.setPrefHeight(38);
        passwordField.setMaxWidth(Double.MAX_VALUE);

        TextField visibleField = new TextField();
        visibleField.getStyleClass().add("auth-input");
        visibleField.getStyleClass().add("auth-password-input");
        visibleField.setPromptText(promptText);
        visibleField.setPrefHeight(38);
        visibleField.setMaxWidth(Double.MAX_VALUE);
        visibleField.setVisible(false);
        visibleField.setManaged(false);
        visibleField.textProperty().bindBidirectional(passwordField.textProperty());

        StackPane fieldStack = new StackPane(passwordField, visibleField);
        fieldStack.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fieldStack, Priority.ALWAYS);

        Button toggleButton = new Button("Show");
        toggleButton.getStyleClass().add("password-toggle-button");
        toggleButton.setOnAction(event -> {
            boolean showing = visibleField.isVisible();
            visibleField.setVisible(!showing);
            visibleField.setManaged(!showing);
            passwordField.setVisible(showing);
            passwordField.setManaged(showing);
            toggleButton.setText(showing ? "Show" : "Hide");
            TextField activeField = showing ? passwordField : visibleField;
            activeField.requestFocus();
            activeField.end();
        });

        HBox row = new HBox(8, fieldStack, toggleButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = createErrorLabel();
        block.getChildren().addAll(label, row, errorLabel);
        return new FieldGroup(block, passwordField, errorLabel, visibleField);
    }

    protected PinGroup createPinGroup(String labelText, int count) {
        VBox container = new VBox(8);
        container.setAlignment(Pos.CENTER);

        Label label = new Label(labelText);
        label.getStyleClass().add("pin-label");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);
        row.getStyleClass().add("pin-row");

        List<TextField> fields = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            final int currentIndex = index;
            TextField pinBox = new TextField();
            pinBox.getStyleClass().add("pin-box");
            pinBox.setPrefSize(40, 40);
            pinBox.setAlignment(Pos.CENTER);
            pinBox.textProperty().addListener((observable, oldValue, newValue) -> {
                String sanitized = newValue.replaceAll("[^0-9]", "");
                if (sanitized.length() > 1) {
                    sanitized = sanitized.substring(0, 1);
                }
                if (!sanitized.equals(newValue)) {
                    pinBox.setText(sanitized);
                    return;
                }
                if (!sanitized.isEmpty() && !sanitized.equals(oldValue) && currentIndex < count - 1) {
                    TextField nextField = fields.get(currentIndex + 1);
                    nextField.requestFocus();
                    nextField.selectAll();
                }
            });
            pinBox.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.BACK_SPACE && pinBox.getText().isEmpty() && currentIndex > 0) {
                    TextField previousField = fields.get(currentIndex - 1);
                    previousField.requestFocus();
                    previousField.end();
                } else if (event.getCode() == KeyCode.LEFT && currentIndex > 0) {
                    fields.get(currentIndex - 1).requestFocus();
                } else if (event.getCode() == KeyCode.RIGHT && currentIndex < count - 1) {
                    fields.get(currentIndex + 1).requestFocus();
                }
            });
            pinBox.focusedProperty().addListener((observable, oldValue, focused) -> {
                if (focused) {
                    pinBox.selectAll();
                }
            });
            fields.add(pinBox);
            row.getChildren().add(pinBox);
        }

        Label errorLabel = createErrorLabel();
        container.getChildren().addAll(label, row, errorLabel);
        return new PinGroup(container, row, fields, errorLabel);
    }

    protected HBox createFooterText(String text, String linkText, Runnable action) {
        HBox footer = new HBox(0);
        footer.setAlignment(Pos.CENTER);

        Label plainText = new Label(text);
        plainText.getStyleClass().add("footer-text");

        Button link = new Button(linkText);
        link.getStyleClass().add("switch-link");
        link.setOnAction(event -> action.run());

        footer.getChildren().addAll(plainText, link);
        return footer;
    }

    protected abstract Node buildForm();

    protected Label createStatusLabel() {
        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        return statusLabel;
    }

    protected void showStatus(Label label, String message, boolean error) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
        label.getStyleClass().removeAll("status-error", "status-success");
        label.getStyleClass().add(error ? "status-error" : "status-success");
    }

    protected void hideStatus(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
        label.getStyleClass().removeAll("status-error", "status-success");
    }

    protected void clearFieldError(FieldGroup group) {
        setFieldError(group, null);
    }

    protected void setFieldError(FieldGroup group, String message) {
        group.errorLabel.setText(message == null ? "" : message);
        group.input.pseudoClassStateChanged(ERROR_CLASS, message != null);
        if (group.secondaryInput != null) {
            group.secondaryInput.pseudoClassStateChanged(ERROR_CLASS, message != null);
        }
        group.errorLabel.setVisible(message != null);
        group.errorLabel.setManaged(message != null);
    }

    protected void clearPinError(PinGroup group) {
        setPinError(group, null);
    }

    protected void setPinError(PinGroup group, String message) {
        group.errorLabel.setText(message == null ? "" : message);
        group.errorLabel.setVisible(message != null);
        group.errorLabel.setManaged(message != null);
        for (TextField field : group.fields) {
            field.pseudoClassStateChanged(ERROR_CLASS, message != null);
        }
    }

    protected String readPin(PinGroup group) {
        StringBuilder builder = new StringBuilder();
        for (TextField field : group.fields) {
            builder.append(field.getText().trim());
        }
        return builder.toString();
    }

    private Label createErrorLabel() {
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        return errorLabel;
    }

    private Pane createDecorationLayer() {
        Pane pane = new Pane();
        pane.setMouseTransparent(true);

        pane.getChildren().addAll(
                createCircuit(0, 120, false),
                createCircuit(0, 490, false),
                createCircuit(675, 55, true),
                createCircuit(748, 410, true)
        );

        return pane;
    }

    private Pane createCircuit(double startX, double startY, boolean mirrored) {
        Pane group = new Pane();
        group.setLayoutX(startX);
        group.setLayoutY(startY);

        group.getChildren().addAll(
                createBranch(0, 0, 82, 0, 105, -42),
                createBranch(0, 0, 104, 0, 127, 28),
                createBranch(0, 0, 82, 0, 104, 74),
                createBranch(0, 0, 56, 0, 72, 46)
        );

        if (mirrored) {
            group.setScaleX(-1);
        }

        return group;
    }

    private Node createBranch(double x1, double y1, double x2, double y2, double x3, double y3) {
        Pane branch = new Pane();
        Line first = new Line(x1, y1, x2, y2);
        Line second = new Line(x2, y2, x3, y3);
        Circle joint = new Circle(x2, y2, 6);
        Circle end = new Circle(x3, y3, 6);

        first.getStyleClass().add("circuit-line");
        second.getStyleClass().add("circuit-line");
        joint.getStyleClass().add("circuit-node");
        end.getStyleClass().add("circuit-node");

        branch.getChildren().addAll(first, second, joint, end);
        return branch;
    }

    protected static final class FieldGroup {
        private final VBox container;
        private final TextField input;
        private final Label errorLabel;
        private final TextField secondaryInput;

        private FieldGroup(VBox container, TextField input, Label errorLabel) {
            this(container, input, errorLabel, null);
        }

        private FieldGroup(VBox container, TextField input, Label errorLabel, TextField secondaryInput) {
            this.container = container;
            this.input = input;
            this.errorLabel = errorLabel;
            this.secondaryInput = secondaryInput;
        }

        public VBox container() {
            return container;
        }

        public TextField input() {
            return input;
        }
    }

    protected static final class PinGroup {
        private final VBox container;
        private final HBox row;
        private final List<TextField> fields;
        private final Label errorLabel;

        private PinGroup(VBox container, HBox row, List<TextField> fields, Label errorLabel) {
            this.container = container;
            this.row = row;
            this.fields = fields;
            this.errorLabel = errorLabel;
        }

        public VBox container() {
            return container;
        }

        public HBox row() {
            return row;
        }
    }
}
