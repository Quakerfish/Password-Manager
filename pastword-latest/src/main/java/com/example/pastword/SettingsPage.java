package com.example.pastword;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

// Displays profile editing and about information for the signed-in user.
public class SettingsPage extends StackPane {

    public enum Section {
        PROFILE,
        ABOUT
    }

    private final PastwordApp app;
    private final UserAccount account;
    private final StackPane contentArea = new StackPane();
    private final Button profileButton = new Button("Profile");
    private final Button aboutButton = new Button("About");
    private Section currentSection;
    private String selectedPhotoPath;

    public SettingsPage(PastwordApp app, UserAccount account, Section initialSection) {
        this.app = app;
        this.account = account;
        this.currentSection = initialSection == null ? Section.PROFILE : initialSection;
        this.selectedPhotoPath = account.getPhotoPath();

        getStyleClass().add("manager-root");

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("manager-shell");
        shell.setLeft(createSidebar());
        shell.setCenter(createContent());
        shell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        getChildren().add(shell);
        showSection(currentSection);
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(18);
        sidebar.getStyleClass().addAll("sidebar-panel", "settings-sidebar");
        sidebar.setPadding(new Insets(18, 16, 18, 16));
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(200);

        Node logo = BrandAssetFactory.createSidebarLogo();

        configureNavButton(profileButton, Section.PROFILE);
        configureNavButton(aboutButton, Section.ABOUT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button backButton = new Button("Back");
        backButton.getStyleClass().addAll("mini-action", "settings-back-button");
        backButton.setOnAction(event -> app.showManagerPage());

        sidebar.getChildren().addAll(logo, profileButton, aboutButton, spacer, backButton);
        return sidebar;
    }

    private VBox createContent() {
        VBox content = new VBox(18);
        content.getStyleClass().add("content-panel");
        content.setPadding(new Insets(18, 28, 18, 28));

        Node headerLogo = BrandAssetFactory.createContentLogo();

        VBox.setVgrow(contentArea, Priority.ALWAYS);
        contentArea.setAlignment(Pos.TOP_CENTER);

        content.getChildren().addAll(headerLogo, contentArea);
        return content;
    }

    private void configureNavButton(Button button, Section section) {
        button.getStyleClass().addAll("sidebar-button", "settings-nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> showSection(section));
    }

    private void showSection(Section section) {
        currentSection = section;
        profileButton.getStyleClass().remove("settings-nav-button-active");
        aboutButton.getStyleClass().remove("settings-nav-button-active");

        if (section == Section.PROFILE) {
            profileButton.getStyleClass().add("settings-nav-button-active");
            contentArea.getChildren().setAll(createProfileView());
        } else {
            aboutButton.getStyleClass().add("settings-nav-button-active");
            contentArea.getChildren().setAll(createAboutView());
        }
    }

    private VBox createProfileView() {
        VBox wrapper = new VBox(18);
        wrapper.getStyleClass().add("settings-content");
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(6, 0, 0, 0));
        wrapper.setMaxWidth(780);

        Label title = new Label("PROFILE SETTINGS");
        title.getStyleClass().add("settings-title");

        TextField usernameField = createSettingsField(account.getUsername(), "Username");
        TextField emailField = createSettingsField(account.getEmail(), "Email");
        TogglePasswordField passwordField = createSettingsPasswordField();

        StackPane avatarHolder = new StackPane();
        refreshAvatarHolder(avatarHolder, usernameField.getText(), selectedPhotoPath, "settings-avatar", 126);
        usernameField.textProperty().addListener((observable, oldValue, newValue) ->
                refreshAvatarHolder(avatarHolder, newValue, selectedPhotoPath, "settings-avatar", 126));

        Button photoButton = new Button("Change Photo");
        photoButton.getStyleClass().addAll("mini-action", "settings-photo-button");
        photoButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Profile Photo");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            File chosenFile = chooser.showOpenDialog(app.getStage());
            if (chosenFile != null) {
                selectedPhotoPath = chosenFile.getAbsolutePath();
                refreshAvatarHolder(avatarHolder, usernameField.getText(), selectedPhotoPath, "settings-avatar", 126);
            }
        });

        GridPane form = createSettingsForm();
        form.add(createSettingsLabel("Username"), 0, 0);
        form.add(usernameField, 1, 0);
        form.add(createSettingsLabel("Password"), 0, 1);
        form.add(passwordField, 1, 1);
        form.add(createSettingsLabel("Email"), 0, 2);
        form.add(emailField, 1, 2);

        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("settings-status");
        statusLabel.setVisible(false);
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());

        Button saveButton = new Button("Save Settings");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setPrefWidth(172);
        saveButton.setOnAction(event -> {
            String result = app.updateCurrentUserProfile(
                    emailField.getText().trim(),
                    usernameField.getText().trim(),
                    passwordField.getText(),
                    selectedPhotoPath
            );

            if (result != null) {
                statusLabel.setText(result);
                statusLabel.getStyleClass().remove("settings-status-success");
                if (!statusLabel.getStyleClass().contains("settings-status-error")) {
                    statusLabel.getStyleClass().add("settings-status-error");
                }
                statusLabel.setVisible(true);
                return;
            }

            statusLabel.setText("Profile settings saved.");
            statusLabel.getStyleClass().remove("settings-status-error");
            if (!statusLabel.getStyleClass().contains("settings-status-success")) {
                statusLabel.getStyleClass().add("settings-status-success");
            }
            statusLabel.setVisible(true);
            passwordField.clear();
            refreshAvatarHolder(avatarHolder, usernameField.getText(), selectedPhotoPath, "settings-avatar", 126);
        });

        wrapper.getChildren().addAll(title, avatarHolder, photoButton, form, statusLabel, saveButton);
        return wrapper;
    }

    private VBox createAboutView() {
        VBox wrapper = new VBox(18);
        wrapper.getStyleClass().add("settings-content");
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(18, 0, 0, 0));
        wrapper.setMaxWidth(780);

        Label title = new Label("ABOUT PASTWORD");
        title.getStyleClass().add("settings-title");

        VBox card = new VBox(12);
        card.getStyleClass().add("settings-info-card");

        Label appName = createAboutText("App: Pastword");
        Label summary = createAboutText("A local JavaFX password manager for organizing saved accounts into folders.");
        Label details = createAboutText("Profile, folders, and passwords are stored locally on the device.");
        Label developers = createAboutText("Developers: Add your team names here.");
        Label version = createAboutText("Version: Classroom prototype build");

        card.getChildren().addAll(appName, summary, details, developers, version);
        wrapper.getChildren().addAll(title, card);
        return wrapper;
    }

    private GridPane createSettingsForm() {
        GridPane form = new GridPane();
        form.setAlignment(Pos.CENTER);
        form.setHgap(16);
        form.setVgap(16);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(120);
        labelColumn.setHalignment(HPos.RIGHT);

        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setMinWidth(360);
        inputColumn.setHgrow(Priority.ALWAYS);

        form.getColumnConstraints().setAll(labelColumn, inputColumn);
        return form;
    }

    private TextField createSettingsField(String value, String prompt) {
        TextField field = new TextField(value);
        field.getStyleClass().add("settings-input");
        field.setPromptText(prompt);
        field.setPrefWidth(360);
        return field;
    }

    private TogglePasswordField createSettingsPasswordField() {
        return new TogglePasswordField("settings-input", "Leave blank to keep current password", 360);
    }

    private Label createSettingsLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("settings-label");
        return label;
    }

    private Label createAboutText(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("about-text");
        label.setWrapText(true);
        return label;
    }

    private void refreshAvatarHolder(StackPane holder, String username, String photoPath, String styleClass, double size) {
        holder.getChildren().setAll(AvatarViewFactory.createAvatar(username, photoPath, size, styleClass));
    }

}
