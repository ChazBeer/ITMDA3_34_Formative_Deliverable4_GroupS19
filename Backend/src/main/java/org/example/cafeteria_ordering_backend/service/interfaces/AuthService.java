package org.example.cafeteria_ordering_backend.service.interfaces;

import org.example.cafeteria_ordering_backend.dto.requests.LoginRequestDTO;
import org.example.cafeteria_ordering_backend.dto.requests.RegisterRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.AuthResponseDTO;
import org.example.cafeteria_ordering_backend.dto.response.AuthTokensDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserProfileDTO;

public interface AuthService {

    // Registers a new user and returns an authentication response.
    AuthResponseDTO register (RegisterRequestDTO request);

    // Authenticates a user with their login credentials.
    AuthResponseDTO login (LoginRequestDTO request);

    // Authenticates a user and returns access and refresh tokens.
    AuthTokensDTO loginWithRefreshToken (LoginRequestDTO request);

    // Registers a new user and returns access and refresh tokens.
    AuthTokensDTO registerWithRefreshToken (RegisterRequestDTO request);

    // Uses a refresh token to obtain a new access token.
    AuthTokensDTO refreshAccessToken (String refreshToken);

    // Logs out the user by invalidating the supplied access token.
    void logout (String token);

    // Revokes a refresh token to prevent it from being used again.
    void revokeRefreshToken (String refreshToken);

    // Retrieves the profile of the currently authenticated user.
    UserProfileDTO getCurrentUserProfile (String email);

    // Returns true if staff or admin
    boolean isStaffOrAdmin (String email);
}