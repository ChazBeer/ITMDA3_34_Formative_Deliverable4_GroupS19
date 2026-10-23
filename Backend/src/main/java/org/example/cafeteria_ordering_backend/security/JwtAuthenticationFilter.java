package org.example.cafeteria_ordering_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Services used to validate JWTs, load user details, and check blacklisted tokens.
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(
            JwtUtils jwtUtils,
            CustomUserDetailsService userDetailsService,
            TokenBlacklistService tokenBlacklistService) {

        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    // Processes each request to authenticate the user using a JWT.
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Gets the authorization header from the requests.
        String authHeader = request.getHeader("Authorization");

        // No Authorization header.
        // Let Spring Security decide whether the endpoint is public or protected.
        if (authHeader == null || authHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization header exists but is not a Bearer token.
        if (!authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response,"Invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7).trim();

        // Reject empty Bearer tokens.
        if (token.isEmpty()) {
            sendUnauthorized(response,"Bearer token is missing");
            return;
        }

        // Reject logged-out / blacklisted tokens.
        if (tokenBlacklistService.isBlacklistedToken(token)) {
            sendUnauthorized(response,"Token has been blacklisted / logged out");
            return;
        }

        // Reject expired or incorrectly signed JWTs.
        if (!jwtUtils.validateToken(token)) {sendUnauthorized(response,"Invalid or expired token");
            return;
        }

        try {
            // Get the email stored inside the JWT.
            String email = jwtUtils.getUsernameFromToken(token);

            // Load the CURRENT user and role from the database.
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Create the authenticated Spring Security user.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,null,userDetails.getAuthorities()
                    );

            // Adds request details to the authentication object.
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Stores the authenticated user in Spring Security's context.
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (UsernameNotFoundException e) {
            // Rejects the token if associated user doesnt exist.
            sendUnauthorized(response,"User associated with token no longer exists");
            return;

        } catch (Exception e) {
            // Rejects the request if an unexpected authentication error occurs.
            sendUnauthorized(response,"Invalid authentication token");
            return;
        }

        // Continues processing the request after successful authentication.
        filterChain.doFilter(request, response);
    }

    // Sends an HTTP 401 response with a JSON error message.
    private void sendUnauthorized (HttpServletResponse response, String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}