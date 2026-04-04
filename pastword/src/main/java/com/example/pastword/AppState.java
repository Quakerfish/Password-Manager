package com.example.pastword;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class AppState implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, UserAccount> accounts = new LinkedHashMap<>();

    public Map<String, UserAccount> getAccounts() {
        return accounts;
    }
}
