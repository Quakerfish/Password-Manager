package com.example.pastword;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

// Owns scene navigation and shared session state for the whole app.
public class PastwordApp extends Application {

    private static final double AUTH_WIDTH = 980;
    private static final double AUTH_HEIGHT = 720;
    private static final double APP_WIDTH = 1280;
    private static final double APP_HEIGHT = 760;

    private Stage stage;
    private LocalDataStore dataStore;
    private UserAccount currentUser;
    private UserAccount pendingUser;

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
        showScene(new Scene(new LoginPage(this, infoMessage), AUTH_WIDTH, AUTH_HEIGHT));
    }

    public void showSignupPage(String infoMessage) {
        showScene(new Scene(new SignupPage(this, infoMessage), AUTH_WIDTH, AUTH_HEIGHT));
    }

    // Starts login by checking credentials before the pin popup is shown.
    public LoginAttempt attemptLogin(String identity, String password) {
        if ("admin".equals(identity.trim()) && "pass".equals(password)) {
            return new LoginAttempt(LoginDestination.ADMIN, null);
        }

        Optional<UserAccount> authenticatedUser = dataStore.authenticate(identity, password);
        if (authenticatedUser.isEmpty()) {
            pendingUser = null;
            return new LoginAttempt(LoginDestination.INVALID, "Incorrect username/email or password.");
        }

        pendingUser = authenticatedUser.get();
        return new LoginAttempt(LoginDestination.PIN_REQUIRED, null);
    }

    // Finalizes the login only after the correct pin code is entered.
    public boolean completePendingLogin(String pin) {
        if (!dataStore.verifyPin(pendingUser, pin)) {
            return false;
        }

        currentUser = pendingUser;
        pendingUser = null;
        showManagerPage();
        return true;
    }

    public String attemptSignup(String email, String username, String password, String pin) {
        try {
            currentUser = dataStore.registerAccount(email, username, password, pin);
            pendingUser = null;
            showManagerPage();
            return null;
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        }
    }

    // Opens the main password manager scene for the current signed-in user.
    public void showManagerPage() {
        if (currentUser == null) {
            showLoginPage(null);
            return;
        }

        showScene(new Scene(new PasswordManagerPage(this, currentUser), APP_WIDTH, APP_HEIGHT));
    }

    // Opens the normal user settings scene at the requested section.
    public void showSettingsPage() {
        showSettingsPage(SettingsPage.Section.PROFILE);
    }

    public void showSettingsPage(SettingsPage.Section section) {
        if (currentUser == null) {
            showLoginPage(null);
            return;
        }

        showScene(new Scene(new SettingsPage(this, currentUser, section), APP_WIDTH, APP_HEIGHT));
    }

    // Opens the admin account management scene.
    public void showAdminPage() {
        showScene(new Scene(new AdminPage(this), APP_WIDTH, APP_HEIGHT));
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

    // Returns all saved user accounts for the admin accounts list.
    public List<UserAccount> getAllAccounts() {
        return dataStore.getAllAccounts();
    }

    // Applies admin-side edits to a selected user account.
    public String adminUpdateAccount(UserAccount account, String email, String username, String password, String pin, String photoPath) {
        return dataStore.adminUpdateAccount(account, email, username, password, pin, photoPath);
    }

    // Deletes a selected account from the admin accounts list.
    public void deleteAccount(UserAccount account) {
        dataStore.deleteAccount(account);
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
        pendingUser = null;
        showLoginPage("You have been logged out.");
    }

    // Applies the shared stylesheet and resizes the stage to the scene's preferred size.
    private void showScene(Scene scene) {
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setScene(scene);
        stage.sizeToScene();
    }

    public enum LoginDestination {
        INVALID,
        PIN_REQUIRED,
        ADMIN
    }

    public record LoginAttempt(LoginDestination destination, String message) {
    }

    public static void main(String[] args) {
        launch(args);
    }
}
