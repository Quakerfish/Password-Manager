package com.example.pastword;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private String email;
    private String username;
    private String passwordHash;
    private final String pinCode;
    private String photoPath;
    private final Map<String, List<PasswordEntry>> folderEntries = new LinkedHashMap<>();

    public UserAccount(String email, String username, String passwordHash, String pinCode) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.pinCode = pinCode;
    }

    public String getEmail() {
        return email == null ? "" : email;
    }

    public String getUsername() {
        return username == null ? "" : username;
    }

    public String getPasswordHash() {
        return passwordHash == null ? "" : passwordHash;
    }

    public String getPinCode() {
        return pinCode;
    }

    public String getPhotoPath() {
        return photoPath == null ? "" : photoPath;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public List<String> getFolders() {
        return new ArrayList<>(folderEntries.keySet());
    }

    public boolean hasFolder(String folderName) {
        return folderEntries.containsKey(folderName);
    }

    public void addFolder(String folderName) {
        if (!folderName.isBlank()) {
            folderEntries.putIfAbsent(folderName, new ArrayList<>());
        }
    }

    public void renameFolder(String oldName, String newName) {
        if (!folderEntries.containsKey(oldName) || oldName.equals(newName)) {
            return;
        }

        List<PasswordEntry> movedEntries = folderEntries.get(oldName);
        LinkedHashMap<String, List<PasswordEntry>> reordered = new LinkedHashMap<>();
        for (Map.Entry<String, List<PasswordEntry>> entry : folderEntries.entrySet()) {
            if (entry.getKey().equals(oldName)) {
                reordered.put(newName, movedEntries);
            } else {
                reordered.put(entry.getKey(), entry.getValue());
            }
        }
        folderEntries.clear();
        folderEntries.putAll(reordered);
    }

    public void deleteFolder(String folderName) {
        folderEntries.remove(folderName);
    }

    public List<PasswordEntry> getEntriesForFolder(String folderName) {
        return folderEntries.computeIfAbsent(folderName, ignored -> new ArrayList<>());
    }
}
