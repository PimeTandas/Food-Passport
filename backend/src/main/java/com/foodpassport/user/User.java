package com.foodpassport.user;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Someone with an account. Each user has their own passport. */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String username;

    /** Never the password itself - a one-way BCrypt hash of it. */
    private String passwordHash;

    private Instant createdAt;

    protected User() {
    }

    public User(String email, String username, String passwordHash) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}
