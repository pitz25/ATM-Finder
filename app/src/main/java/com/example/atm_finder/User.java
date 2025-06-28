package com.example.atm_finder;

public class User {
    public String fullName, username, email, phone;

    public User() {
        // Required for Firebase
    }

    public User(String fullName, String username, String email, String phone) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.phone = phone;
    }
}
