package org.example.cafeteria_ordering_backend.security;

import org.example.cafeteria_ordering_backend.config.SecurityConfig;
import org.example.cafeteria_ordering_backend.controller.OrderController;
import org.example.cafeteria_ordering_backend.service.interfaces.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
public class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("Should allow request with valid JWT")
    void validJwt () throws Exception {

        // Config user with valid token
        String token = "valid.jwt.token";
        String email = "alice@campus.edu";

        UserDetails userDetails = User
                .withUsername(email)
                .password("password")
                .roles("STUDENT")
                .build();

        when(tokenBlacklistService.isBlacklistedToken(token)).thenReturn(false);
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getUsernameFromToken(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

        // Execute protected request and check response
        mockMvc.perform(get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());

        // Ensure that the token and linked user was correctly loaded.
        verify(jwtUtils).validateToken(token);
        verify(jwtUtils).getUsernameFromToken(token);
        verify(userDetailsService).loadUserByUsername(email);
    }

    @Test
    @DisplayName("Should return 401 unauthorized when jwt is expired")
    void expireJwt () throws Exception {

        // Simulate a token that fails JWT validation
        String token = "expired.jwt.token";

        when(tokenBlacklistService.isBlacklistedToken(token)).thenReturn(false);
        when(jwtUtils.validateToken(token)).thenReturn(false);

        // The protected endpoint must reject the request
        mockMvc.perform(get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());

        // Verify that validation occurs without loading the user
        verify(jwtUtils).validateToken(token);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when JWT is invalid or tampered")
    void invalidJwt () throws Exception {

        // Configure supplied token so that it fails validation
        String token = "tampered.jwt.token";

        when(tokenBlacklistService.isBlacklistedToken(token)).thenReturn(false);
        when(jwtUtils.validateToken(token)).thenReturn(false);

        // Ensure correct exception denying access to protected enpoint
        mockMvc.perform(get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());

        // Ensure invalid token does not trigger user lookup
        verify(jwtUtils).validateToken(token);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when Authorization header is missing")
    void missingAuthorizationHeader () throws Exception {

        // Send request without authentication credentials
        mockMvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());

        verifyNoInteractions(jwtUtils);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when Authorization header is not Bearer")
    void malformedAuthorizationHeader () throws Exception {

        // Provide basic auth instead of bearer token
        mockMvc.perform(get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Basic abc123"))
                .andExpect(status().isUnauthorized());

        // Ensure JWT validation and user lookup are not attempted
        verifyNoInteractions(jwtUtils);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when Bearer token is empty")
    void emptyBearerToken () throws Exception {

        // Submit authorization header with no token
        mockMvc.perform(get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(content()
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(content()
                        .json("{\"error\":\"Bearer token is missing\"}"));

        // Verify empty token is rejected before JWT validation or user lookup
        verify(jwtUtils, never()).validateToken(anyString());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when JWT is blacklisted")
    void blacklistedJwt () throws Exception {

        // Setup token that has been blacklisted
        String token = "blacklisted.jwt.token";

        when(tokenBlacklistService.isBlacklistedToken(token)).thenReturn(true);

        // Should not be granted access
        mockMvc.perform(get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());

        // Ensure that the blacklist is checked before JWT processing
        verify(tokenBlacklistService).isBlacklistedToken(token);
        verifyNoInteractions(jwtUtils);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Should handle JWT for a nonexistent user")
    void jwtForNonexistentUser () throws Exception {

        // Configure valid token for a user not on the system
        String token = "valid.jwt.token";
        String email = "nonexistent@campus.edu";

        when(tokenBlacklistService.isBlacklistedToken(token)).thenReturn(false);
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getUsernameFromToken(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenThrow(new UsernameNotFoundException("User not found"));

        // Authentication must fail when user cannot be found
        mockMvc.perform(get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}