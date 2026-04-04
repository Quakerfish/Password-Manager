package com.example.pastword;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Optional;

public class PastwordApp extends Application {

    private static final double AUTH_WIDTH = 980;
    private static final double AUTH_HEIGHT = 720;
    private static final double APP_WIDTH = 1280;
    private static final double APP_HEIGHT = 760;

    private Stage stage;
    private LocalDataStore dataStore;
    private UserAccount currentUser;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        dataStore = new LocalDataStore();
        stage.setTitle("Pastword");
        showLoginPage(null);
        stage.setResizable(false);
        stage.show();
    }

    public void showLoginPage(String infoMessage) {
        Scene scene = new Scene(new LoginPage(this, infoMessage), AUTH_WIDTH, AUTH_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setScene(scene);
    }

    public void showSignupPage(String infoMessage) {
        Scene scene = new Scene(new SignupPage(this, infoMessage), AUTH_WIDTH, AUTH_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setScene(scene);
    }

    public String attemptLogin(String identity, String password) {
        Optional<UserAccount> authenticatedUser = dataStore.authenticate(identity, password);
        if (authenticatedUser.isEmpty()) {
            return "Incorrect username/email or password.";
        }

        currentUser = authenticatedUser.get();
        showManagerPage(true);
        return null;
    }

    public String attemptSignup(String email, String username, String password, String pin) {
        try {
            currentUser = dataStore.registerAccount(email, username, password, pin);
            showManagerPage(false);
            return null;
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        }
    }

    public void showManagerPage() {
        showManagerPage(false);
    }

    public void showManagerPage(boolean requirePin) {
        if (currentUser == null) {
            showLoginPage(null);
            return;
        }

        Scene scene = new Scene(new PasswordManagerPage(this, currentUser, requirePin), APP_WIDTH, APP_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setScene(scene);
    }

    public void showSettingsPage() {
        showSettingsPage(SettingsPage.Section.PROFILE);
    }

    public void showSettingsPage(SettingsPage.Section section) {
        if (currentUser == null) {
            showLoginPage(null);
            return;
        }

        Scene scene = new Scene(new SettingsPage(this, currentUser, section), APP_WIDTH, APP_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setScene(scene);
    }

    public boolean verifyCurrentUserPin(String pin) {
        return dataStore.verifyPin(currentUser, pin);
    }

    public String updateCurrentUserProfile(String email, String username, String password, String photoPath) {
        if (currentUser == null) {
            return "No account is currently signed in.";
        }
        return dataStore.updateAccount(currentUser, email, username, password, photoPath);
    }

    public void persistCurrentUser() {
        if (currentUser != null) {
            dataStore.saveUser(currentUser);
        }
    }

    public Stage getStage() {
        return stage;
    }

    public void logout() {
        currentUser = null;
        showLoginPage("You have been logged out.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
