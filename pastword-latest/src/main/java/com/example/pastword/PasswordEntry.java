package com.example.pastword;

import java.io.Serializable;
import java.util.UUID;

public class PasswordEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private String title;
    private String username;
    private String password;
    private String notes;

    public PasswordEntry(String title, String username, String password) {
        this(UUID.randomUUID().toString(), title, username, password, "");
    }

    public PasswordEntry(String title, String username, String password, String notes) {
        this(UUID.randomUUID().toString(), title, username, password, notes);
    }

    public PasswordEntry(String id, String title, String username, String password, String notes) {
        this.id = id;
        this.title = title;
        this.username = username;
        this.password = password;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNotes() {
        return notes == null ? "" : notes;
    }
}
