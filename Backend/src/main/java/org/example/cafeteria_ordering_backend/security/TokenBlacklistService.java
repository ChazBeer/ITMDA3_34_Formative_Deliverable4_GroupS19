package org.example.cafeteria_ordering_backend.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    // Stores blacklisted tokens and their expiration times.
    // ConcurrentHashMap allows safe access from multiple threads.
    private final Map<String, Instant> blacklistedTokens =
            new ConcurrentHashMap<>();

    // Removes the Bearer prefix and surrounding whitespace from a token.
    private String sanitizeToken(String token) {

        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7).trim();
        }

        return token != null ? token.trim() : "";
    }

    // Adds a token to the blacklist until its expiration time.
    public void blacklistToken(String token, Instant expiryTime) {

        String cleanToken = sanitizeToken(token);

        if (!cleanToken.isEmpty()) {
            blacklistedTokens.put(cleanToken, expiryTime);
        }
    }

    // Checks whether a token has been blacklisted.
    public boolean isBlacklistedToken(String token) {

        String cleanToken = sanitizeToken(token);
        return blacklistedTokens.containsKey(cleanToken);
    }

    // Removes expired tokens every 15 minutes to keep the blacklist smaller.
    @Scheduled(fixedRate = 900000)
    public void purgeExpiredTokens() {

        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(entry ->entry.getValue().isBefore(now));
    }
}