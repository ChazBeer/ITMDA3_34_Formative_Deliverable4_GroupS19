package org.example.cafeteria_ordering_backend.controller;

import org.example.cafeteria_ordering_backend.dto.requests.LoginRequestDTO;
import org.example.cafeteria_ordering_backend.dto.requests.RegisterRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.AuthResponseDTO;
import org.example.cafeteria_ordering_backend.dto.response.AuthTokensDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserProfileDTO;
import org.example.cafeteria_ordering_backend.service.implementation.AuthServiceImpl;
import org.example.cafeteria_ordering_backend.service.interfaces.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    @Value("${app.cafeteria.closing-time:17:30}")
    private String closingTimeConfig;

    // Inject secure cookie setting from application.properties (defaults to false if not found)
    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Register a new user and issue authentication token.
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register (
            @Valid @RequestBody RegisterRequestDTO request, HttpServletResponse response) {

        AuthTokensDTO tokens = authService.registerWithRefreshToken(request);
        setRefreshTokenCookie(response, tokens.getRefreshToken(), "refreshToken");

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponseDTO(tokens.getAccessToken()));
    }

    // Authenticate an existing customer user and issue authentication tokens. Rejects admins and staff
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login (
            @Valid @RequestBody LoginRequestDTO request,HttpServletResponse response) {

        AuthTokensDTO tokens = authService.loginWithRefreshToken(request);

        // enforces lest privilege.
        boolean isStaffOrAdmin = authService.isStaffOrAdmin(request.getEmail());

        if (isStaffOrAdmin) {
            logger.warn("Security Alert: Staff/Admin user {} attempted to log into the student portal.",
                    request.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AuthResponseDTO.builder()
                                    .message("Staff and admin accounts cannot log in through the student portal. " +
                                            "Please use the staff dashboard.").build()
                    );
        }

        setRefreshTokenCookie(response,tokens.getRefreshToken(), "refreshToken");

        return ResponseEntity.ok(new AuthResponseDTO(tokens.getAccessToken()));
    }

    // Staff Dashboard Login - Rejects Customers/Students
    @PostMapping("/staff-login")
    public ResponseEntity<AuthResponseDTO> staffLogin (
            @Valid @RequestBody LoginRequestDTO request, HttpServletResponse response) {

        AuthTokensDTO tokens = authService.loginWithRefreshToken(request);

        // Enforce east privilege. Ensures user has staff/admin rights
        boolean isStaffOrAdmin = authService.isStaffOrAdmin(request.getEmail());

        if (!isStaffOrAdmin) {
            logger.warn("Security Alert: Non-staff user {} attempted to log into the staff portal.", request.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AuthResponseDTO.builder()
                                    .message("Access denied. Only staff and administrators are allowed " +
                                            "to log into the dashboard.").build()
                    );
        }

        // Issue separate staff cookie name to eliminate localhost port collisions entirely!
        setRefreshTokenCookie(response, tokens.getRefreshToken(), "staffRefreshToken");

        return ResponseEntity.ok(new AuthResponseDTO(tokens.getAccessToken()));
    }

    // Issue a new access token using the student refresh token stored in the cookie.
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh (
            @CookieValue(value = "refreshToken", required = false) String refreshToken,HttpServletResponse response) {

        AuthTokensDTO tokens = authService.refreshAccessToken(refreshToken);
        setRefreshTokenCookie(response, tokens.getRefreshToken(), "refreshToken");

        return ResponseEntity.ok(new AuthResponseDTO(tokens.getAccessToken()));
    }

    // Issue a new access token using the staff refresh token stored in the cookie.
    @PostMapping("/staff-refresh")
    public ResponseEntity<AuthResponseDTO> staffRefresh (
            @CookieValue(value = "staffRefreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        AuthTokensDTO tokens = authService.refreshAccessToken(refreshToken);
        setRefreshTokenCookie(response, tokens.getRefreshToken(), "staffRefreshToken");

        return ResponseEntity.ok(new AuthResponseDTO(tokens.getAccessToken()));
    }

    // Return the profile of the currently authenticated user.
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser (Authentication authentication) {

        // Return 401 if no authenticated user is available.
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Retrieve the user's profile using their authenticated email address.
        String userEmail = authentication.getName();
        UserProfileDTO userProfile = authService.getCurrentUserProfile(userEmail);

        return ResponseEntity.ok(userProfile);
    }

    // Revoke student authentication tokens and clear the customer refresh cookie.
    @PostMapping("/logout")
    public ResponseEntity<?> logout (
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "refreshToken",required = false) String refreshToken,
            HttpServletResponse response) {

        // Revoke the access token if an Authorization header was provided.
        if (authHeader != null) {
            authService.logout(authHeader);
        }

        // Revoke the refresh token if the cookie was provided.
        if (refreshToken != null) {
            authService.revokeRefreshToken(refreshToken);
        }

        // Remove the student refresh token cookie from the browser.
        clearRefreshTokenCookie(response, "refreshToken");

        return ResponseEntity.ok(Map.of("message", "logout successful"));
    }

    // Revoke staff authentication tokens and clear the staff refresh cookie.
    @PostMapping("/staff-logout")
    public ResponseEntity<?> staffLogout (
            @RequestHeader(value = "Authorization",required = false) String authHeader,
            @CookieValue(value = "staffRefreshToken",required = false) String refreshToken,
            HttpServletResponse response) {

        // Revoke the access token if an Authorization header was provided.
        if (authHeader != null) {
            authService.logout(authHeader);
        }

        // Revoke the refresh token if the cookie was provided.
        if (refreshToken != null) {
            authService.revokeRefreshToken(refreshToken);
        }

        // Remove the staff refresh token cookie from the browser.
        clearRefreshTokenCookie(response, "staffRefreshToken");

        return ResponseEntity.ok(Map.of("message", "staff logout successful"));
    }

    // Create an HTTP-only cookie to store the refresh token.
    private void setRefreshTokenCookie (HttpServletResponse response,String refreshToken, String cookieName) {

        Cookie cookie = new Cookie(cookieName, refreshToken);

        // Prevents JavaScript from directly accessing the refresh token.
        cookie.setHttpOnly(true);

        // Uses application.properties value (false locally, true for Cloudflare/Production)
        cookie.setSecure(cookieSecure);

        // Cookie available to authentication endpoints.
        cookie.setPath("/api/auth");

        // Parse closing time from application.properties (e.g., "17:30")
        LocalTime closingTimeLocal = LocalTime.parse(closingTimeConfig);

        // Calculate seconds until closing time today (or tomorrow if past closing)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime closingTime = now.toLocalDate().atTime(closingTimeLocal);

        if (now.isAfter(closingTime)) {
            closingTime = closingTime.plusDays(1);
        }

        long secondsUntilClosing = Duration.between(now, closingTime).getSeconds();

        // Set the refresh token cookie to expire precisely at closing time
        cookie.setMaxAge((int) secondsUntilClosing);

        response.addCookie(cookie);
    }

    // Expire the refresh token cookie during logout.
    private void clearRefreshTokenCookie (HttpServletResponse response, String cookieName) {

        Cookie cookie = new Cookie(cookieName, null);

        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }
}