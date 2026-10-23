package org.example.cafeteria_ordering_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    // Primary key, generated automatically by the database.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stores the hashed refresh token instead of the original token.
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // User associated with this refresh token.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Date and time when the refresh token expires.
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // Indicates whether the refresh token has been revoked.
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    // Timestamp recording when the refresh token was created.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Sets the creation timestamp before the entity is first saved.
    @PrePersist
    protected void onCreate () {
        createdAt = Instant.now();
    }
}
