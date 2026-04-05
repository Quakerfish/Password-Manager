package com.example.pastword;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

// Handles local account persistence, validation, and admin-side account updates.
public class LocalDataStore {

    private final Path storageDirectory = Paths.get(System.getProperty("user.home"), ".pastword");
    private final Path storageFile = storageDirectory.resolve("accounts.dat");
    private final AppState state;

    public LocalDataStore() {
        state = loadState();
    }

    public synchronized Optional<UserAccount> authenticate(String identity, String password) {
        String normalizedIdentity = identity.trim().toLowerCase();
        String passwordHash = hash(password);

        return state.getAccounts().values().stream()
                .filter(account -> account.getUsername().equalsIgnoreCase(normalizedIdentity)
                        || account.getEmail().equalsIgnoreCase(normalizedIdentity))
                .filter(account -> account.getPasswordHash().equals(passwordHash))
                .findFirst();
    }

    public synchronized boolean verifyPin(UserAccount account, String pin) {
        return account != null && account.getPinCode().equals(pin);
    }

    // Returns a snapshot list of all saved user accounts for the admin scene.
    public synchronized List<UserAccount> getAllAccounts() {
        return List.copyOf(state.getAccounts().values());
    }

    public synchronized UserAccount registerAccount(String email, String username, String password, String pin) {
        String normalizedUsername = username.trim().toLowerCase();
        String normalizedEmail = email.trim().toLowerCase();

        for (UserAccount account : state.getAccounts().values()) {
            if (account.getUsername().equalsIgnoreCase(normalizedUsername)) {
                throw new IllegalArgumentException("That username is already taken.");
            }
            if (account.getEmail().equalsIgnoreCase(normalizedEmail)) {
                throw new IllegalArgumentException("An account with that email already exists.");
            }
        }

        UserAccount account = new UserAccount(email.trim(), username.trim(), hash(password), pin);
        state.getAccounts().put(normalizedUsername, account);
        saveState();
        return account;
    }

    public synchronized void saveUser(UserAccount account) {
        String normalizedUsername = account.getUsername().trim().toLowerCase();
        state.getAccounts().entrySet().removeIf(entry ->
                entry.getValue() == account && !entry.getKey().equals(normalizedUsername));
        state.getAccounts().put(normalizedUsername, account);
        saveState();
    }

    public synchronized String updateAccount(UserAccount account, String email, String username, String password, String photoPath) {
        String trimmedEmail = email == null ? "" : email.trim();
        String trimmedUsername = username == null ? "" : username.trim();
        String normalizedEmail = trimmedEmail.toLowerCase();
        String normalizedUsername = trimmedUsername.toLowerCase();

        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return "Enter a valid email address.";
        }
        if (trimmedUsername.isEmpty()) {
            return "Username is required.";
        }
        if (password != null && !password.isBlank() && password.length() < 6) {
            return "Password must be at least 6 characters.";
        }

        for (UserAccount existing : state.getAccounts().values()) {
            if (existing == account) {
                continue;
            }
            if (existing.getUsername().equalsIgnoreCase(normalizedUsername)) {
                return "That username is already taken.";
            }
            if (existing.getEmail().equalsIgnoreCase(normalizedEmail)) {
                return "An account with that email already exists.";
            }
        }

        String previousUsername = account.getUsername().trim().toLowerCase();
        state.getAccounts().remove(previousUsername);

        account.setEmail(trimmedEmail);
        account.setUsername(trimmedUsername);
        if (password != null && !password.isBlank()) {
            account.setPasswordHash(hash(password));
        }
        account.setPhotoPath(photoPath == null ? "" : photoPath.trim());

        state.getAccounts().put(normalizedUsername, account);
        saveState();
        return null;
    }

    // Lets the admin edit another user's account details, including the pin code.
    public synchronized String adminUpdateAccount(UserAccount account, String email, String username, String password, String pin, String photoPath) {
        if (pin == null || pin.isBlank() || !pin.matches("\\d{5}")) {
            return "Pin code must be exactly 5 digits.";
        }
        String result = updateAccount(account, email, username, password, photoPath);
        if (result != null) {
            return result;
        }
        account.setPinCode(pin.trim());
        saveUser(account);
        return null;
    }

    // Removes a saved account entirely from the local datastore.
    public synchronized void deleteAccount(UserAccount account) {
        state.getAccounts().entrySet().removeIf(entry -> entry.getValue() == account);
        saveState();
    }

    private AppState loadState() {
        if (!Files.exists(storageFile)) {
            return new AppState();
        }

        try (ObjectInputStream inputStream = new ObjectInputStream(Files.newInputStream(storageFile))) {
            Object loaded = inputStream.readObject();
            if (loaded instanceof AppState loadedState) {
                return loadedState;
            }
        } catch (IOException | ClassNotFoundException ignored) {
            // Falls back to a fresh state if the local file is missing or unreadable.
        }

        return new AppState();
    }

    private void saveState() {
        try {
            Files.createDirectories(storageDirectory);
            try (ObjectOutputStream outputStream = new ObjectOutputStream(Files.newOutputStream(storageFile))) {
                outputStream.writeObject(state);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save local data.", exception);
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
