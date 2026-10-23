package org.example.cafeteria_ordering_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

// TODO: Look into improving exception handling, more detailed exceptions
@Component
public class JwtUtils {

    // Loads the JWT signing secret from application properties.
    // The default value is used only when no jwt.secret property is configured.
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Sets the access token lifetime to 15 minutes by default.
    @Value("${jwt.expiration-ms:900000}")
    private long jwtExpirationMs;

    // Generates secure random values for refresh tokens.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Creates the signing key used to sign and validate access JWTs.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generates a signed access JWT containing the user's email and expiry time.
    public String generateToken(String email) {

        return Jwts.builder().subject(email).issuedAt(new Date()).expiration(
                        new Date(System.currentTimeMillis() + jwtExpirationMs)
                ).signWith(getSigningKey()).compact();
    }

    // Extracts the user's email from a signed JWT after verifying it.
    public String getUsernameFromToken(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    // Validates the JWT signature and checks that the token has not expired.
    public boolean validateToken(String token) {

        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // Returns false if the token is invalid, malformed, or expired.
            return false;
        }
    }

    // Retrieves the expiration date of a signed access JWT.
    public Instant getExpirationDateFromToken (String token) {

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getExpiration().toInstant();
    }

    // Generates cryptographically secure random refresh token.
    // Raw token is sent to client in an HTTP-only cookie.
    // Its SHA-256 hash is stored in the DB instead of the raw value.
    public String generateRefreshToken () {

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // Creates SHA-256 hash of a refresh token for secure database storage.
    public String hashRefreshToken (String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));

            // Converts the hash bytes into a hexadecimal string.
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            // Throws an exception if SHA-256 is unavailable.
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}