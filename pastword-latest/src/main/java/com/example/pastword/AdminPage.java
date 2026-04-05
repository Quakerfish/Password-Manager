package com.example.pastword;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

// Shows the admin-only account management view for editing or deleting saved users.
public class AdminPage extends StackPane {

    private final PastwordApp app;
    private final VBox accountsList = new VBox(18);
    private final StackPane overlayLayer = new StackPane();

    public AdminPage(PastwordApp app) {
        this.app = app;

        getStyleClass().add("manager-root");

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("manager-shell");
        shell.setLeft(createSidebar());
        shell.setCenter(createContent());
        shell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        overlayLayer.getStyleClass().add("overlay-layer");
        overlayLayer.setVisible(false);
        overlayLayer.setManaged(false);
        overlayLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        getChildren().addAll(shell, overlayLayer);
        refreshAccountsList();
    }

    // Builds the admin sidebar with a single accounts section and logout.
    private VBox createSidebar() {
        VBox sidebar = new VBox(18);
        sidebar.getStyleClass().addAll("sidebar-panel", "settings-sidebar");
        sidebar.setPadding(new Insets(18, 16, 18, 16));
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(200);

        Node logo = BrandAssetFactory.createSidebarLogo();

        Button accountsButton = new Button("Accounts");
        accountsButton.getStyleClass().addAll("sidebar-button", "settings-nav-button", "settings-nav-button-active");
        accountsButton.setMaxWidth(Double.MAX_VALUE);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().addAll("mini-action", "settings-back-button");
        logoutButton.setOnAction(event -> app.logout());

        sidebar.getChildren().addAll(logo, accountsButton, spacer, logoutButton);
        return sidebar;
    }

    // Builds the account cards area in the center of the admin scene.
    private VBox createContent() {
        VBox content = new VBox(18);
        content.getStyleClass().add("content-panel");
        content.setPadding(new Insets(18, 28, 18, 28));

        Label title = new Label("Existing Accounts");
        title.getStyleClass().add("settings-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        ScrollPane scrollPane = new ScrollPane(accountsList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        content.getChildren().addAll(title, scrollPane);
        return content;
    }

    // Refreshes the admin list with the latest locally stored accounts.
    private void refreshAccountsList() {
        accountsList.getChildren().clear();
        List<UserAccount> accounts = app.getAllAccounts();

        if (accounts.isEmpty()) {
            Label empty = new Label("No saved accounts yet.");
            empty.getStyleClass().add("empty-state");
            accountsList.getChildren().add(empty);
            return;
        }

        for (UserAccount account : accounts) {
            accountsList.getChildren().add(createAccountRow(account));
        }
    }

    private HBox createAccountRow(UserAccount account) {
        StackPane avatar = AvatarViewFactory.createAvatar(account, 76, "admin-avatar");

        Label username = createAccountInfo("Username: " + account.getUsername());
        Label email = createAccountInfo("Email: " + account.getEmail());
        Label password = createAccountInfo("Password: Protected");
        Label pin = createAccountInfo("Pin: " + account.getPinCode());

        VBox info = new VBox(8, username, email, password, pin);
        info.setAlignment(Pos.CENTER_LEFT);

        HBox card = new HBox(24, avatar, info);
        card.getStyleClass().add("admin-account-card");
        card.setAlignment(Pos.CENTER_LEFT);

        Button editButton = SvgIconFactory.createActionButton("pencil", "Edit account");
        editButton.getStyleClass().add("admin-action-button");
        editButton.setOnAction(event -> showEditAccount(account));

        Button deleteButton = SvgIconFactory.createActionButton("trash", "Delete account");
        deleteButton.getStyleClass().add("admin-action-button");
        deleteButton.setOnAction(event -> showDeleteAccount(account));

        VBox actions = new VBox(18, editButton, deleteButton);
        actions.setAlignment(Pos.CENTER);

        HBox row = new HBox(18, card, actions);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private Label createAccountInfo(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("admin-account-info");
        return label;
    }

    // Opens the admin edit overlay for a selected saved account.
    private void showEditAccount(UserAccount account) {
        VBox card = createOverlayCard("EDIT ACCOUNT", 620);

        TextField usernameField = createOverlayTextField(account.getUsername());
        TextField emailField = createOverlayTextField(account.getEmail());
        TogglePasswordField passwordField = createOverlayPasswordField();

        TextField pinField = createOverlayTextField(account.getPinCode());

        Label photoLabel = new Label(account.getPhotoPath().isBlank() ? "No photo selected." : new File(account.getPhotoPath()).getName());
        photoLabel.getStyleClass().add("detail-value");
        Button photoButton = createOverlayButton("Change Photo", "primary");
        final String[] selectedPhotoPath = {account.getPhotoPath()};
        photoButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Profile Photo");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            File chosenFile = chooser.showOpenDialog(app.getStage());
            if (chosenFile != null) {
                selectedPhotoPath[0] = chosenFile.getAbsolutePath();
                photoLabel.setText(chosenFile.getName());
            }
        });

        GridPane form = createOverlayForm();
        form.add(createFormLabel("Username:"), 0, 0);
        form.add(usernameField, 1, 0);
        form.add(createFormLabel("Email:"), 0, 1);
        form.add(emailField, 1, 1);
        form.add(createFormLabel("Password:"), 0, 2);
        form.add(passwordField, 1, 2);
        form.add(createFormLabel("Pin:"), 0, 3);
        form.add(pinField, 1, 3);
        form.add(createFormLabel("Photo:"), 0, 4);
        form.add(new VBox(8, photoLabel, photoButton), 1, 4);

        Label errorLabel = createOverlayErrorLabel();

        HBox actions = createCenteredActions();
        Button cancelButton = createOverlayButton("Cancel", "secondary");
        cancelButton.setOnAction(event -> hideOverlay());
        Button saveButton = createOverlayButton("Save", "success");
        saveButton.setOnAction(event -> {
            String result = app.adminUpdateAccount(
                    account,
                    emailField.getText().trim(),
                    usernameField.getText().trim(),
                    passwordField.getText(),
                    pinField.getText().trim(),
                    selectedPhotoPath[0]
            );

            if (result != null) {
                errorLabel.setText(result);
                return;
            }

            hideOverlay();
            refreshAccountsList();
        });
        actions.getChildren().addAll(cancelButton, saveButton);

        card.getChildren().addAll(form, errorLabel, actions);
        showOverlay(card);
    }

    // Opens the delete confirmation overlay for a selected saved account.
    private void showDeleteAccount(UserAccount account) {
        VBox card = createOverlayCard("DELETE ACCOUNT", 500);
        Label message = new Label("Delete account \"" + account.getUsername() + "\"?");
        message.getStyleClass().add("overlay-message");
        message.setWrapText(true);

        HBox actions = createCenteredActions();
        Button cancelButton = createOverlayButton("Back", "secondary");
        cancelButton.setOnAction(event -> hideOverlay());
        Button deleteButton = createOverlayButton("Delete", "danger");
        deleteButton.setOnAction(event -> {
            app.deleteAccount(account);
            hideOverlay();
            refreshAccountsList();
        });
        actions.getChildren().addAll(cancelButton, deleteButton);

        card.getChildren().addAll(message, actions);
        showOverlay(card);
    }

    private GridPane createOverlayForm() {
        GridPane form = new GridPane();
        form.getStyleClass().add("overlay-form");
        form.setAlignment(Pos.CENTER);
        form.setHgap(14);
        form.setVgap(12);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(132);
        labelColumn.setHalignment(HPos.RIGHT);

        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setMinWidth(340);
        inputColumn.setHgrow(Priority.ALWAYS);

        form.getColumnConstraints().setAll(labelColumn, inputColumn);
        return form;
    }

    private HBox createCenteredActions() {
        HBox actions = new HBox(14);
        actions.setAlignment(Pos.CENTER);
        return actions;
    }

    private VBox createOverlayCard(String title, double maxWidth) {
        VBox card = new VBox(22);
        card.getStyleClass().add("overlay-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPrefHeight(Region.USE_COMPUTED_SIZE);
        card.setMaxWidth(maxWidth);
        card.setPrefWidth(maxWidth);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("overlay-title");
        card.getChildren().add(titleLabel);
        return card;
    }

    private TextField createOverlayTextField(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("overlay-textfield");
        field.setPrefWidth(340);
        field.setMaxWidth(340);
        return field;
    }

    // Creates the admin password input with the inline eye icon toggle.
    private TogglePasswordField createOverlayPasswordField() {
        return new TogglePasswordField("overlay-textfield", "Leave blank to keep current password", 340);
    }

    private Label createOverlayErrorLabel() {
        Label label = new Label();
        label.getStyleClass().add("overlay-error");
        label.setWrapText(true);
        label.setMaxWidth(472);
        label.setVisible(false);
        label.managedProperty().bind(label.visibleProperty());
        label.textProperty().addListener((observable, oldValue, newValue) ->
                label.setVisible(newValue != null && !newValue.isBlank()));
        return label;
    }

    private Label createFormLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("overlay-form-label");
        return label;
    }

    private Button createOverlayButton(String text, String variant) {
        Button button = new Button(text);
        button.getStyleClass().addAll("overlay-button", "overlay-button-" + variant);
        return button;
    }

    private void showOverlay(VBox card) {
        StackPane backdrop = new StackPane(card);
        backdrop.getStyleClass().add("overlay-backdrop");
        backdrop.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        backdrop.setOnMouseClicked(event -> {
            if (event.getTarget() == backdrop) {
                hideOverlay();
            }
        });
        overlayLayer.getChildren().setAll(backdrop);
        overlayLayer.setVisible(true);
        overlayLayer.setManaged(true);
    }

    private void hideOverlay() {
        overlayLayer.getChildren().clear();
        overlayLayer.setVisible(false);
        overlayLayer.setManaged(false);
    }

}
