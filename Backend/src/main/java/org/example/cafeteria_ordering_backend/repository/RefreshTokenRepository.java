package org.example.cafeteria_ordering_backend.repository;

import org.example.cafeteria_ordering_backend.entity.RefreshToken;
import org.example.cafeteria_ordering_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Finds a refresh token using its stored has.
    Optional<RefreshToken> findByTokenHash (String tokenHash);

    // Deletes refresh tokens that expired before the specified time.
    void deleteByExpiresAtBefore (Instant now);

    // Deletes all refresh tokens associated with a specific use ID.
    void deleteAllByUser_UserId (Long userId);

    // Deletes all refresh tokens associated with the specified user.
    void deleteByUser (User user);
}