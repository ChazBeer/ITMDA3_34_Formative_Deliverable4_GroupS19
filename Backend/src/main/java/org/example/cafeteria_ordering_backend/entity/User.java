package org.example.cafeteria_ordering_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    // Primary key, generated automatically by the database.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    // Unique code assigned to identify the customer.
    @Column(name = "customer_code", nullable = false, unique = true)
    private String customerCode;

    // First name of the user; must not be blank.
    @NotBlank(message = "First name is required")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    // Last name of the user; must not be blank.
    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    // Email address used to identify and contact the user.
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // Optional contact number for the user.
    @Column(name = "phone_number")
    private String phoneNumber;

    // Stores the hashed password rather than the original password.
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Indicates whether the user's account is active.
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    // Many users can share the same single role
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // Timestamps recording when the user was created and last updated.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // Sets the creation and update timestamps before the entity is first saved.
    @PrePersist
    protected void onCreate () {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Updates the modification timestamp before an existing user is updated.
    @PreUpdate
    protected void onUpdate () {
        this.updatedAt = LocalDateTime.now();
    }
}