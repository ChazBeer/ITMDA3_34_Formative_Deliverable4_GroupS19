package org.example.cafeteria_ordering_backend.component;

import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.exception.ResourceUnavailableException;
import org.example.cafeteria_ordering_backend.repository.UserRepository;
import org.example.cafeteria_ordering_backend.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("orderSecurityEvaluator")
public class OrderSecurityEvaluator {

    // Provides access to user records in the database.
    private final UserRepository userRepository;

    public OrderSecurityEvaluator (UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Grabs the User entity corresponding to the currently authenticated user.
     */
    public User getCurrentUser () {

        // Retrieve the current authentication details from Spring Security.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Ensure a valid authenticated user is available.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("No active security context found.");
        }

        // Use the authenticated user's email to retrieve their database record.
        String currentUserEmail = authentication.getName();
        return userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found."));
    }

    /**
     * Checks if authenticated user has ROLE_STAFF or ROLE_ADMIN
     */
    public boolean isStaffOrAdmin () {

        // Retrieve the current authentication details.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Return false if no authentication is available.
        if (authentication == null) {
            return false;
        }

        // Check whether the user has either of the required roles.
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_STAFF") || role.equals("ROLE_ADMIN"));
    }

    /**
     * Checks if the current user is staff/admin OR matches the target user ID.
     */
    public boolean canAccessUser (Long targetUserId) {

       // Staff and admins can access any user's data.
        if (isStaffOrAdmin()) {
            return true;
        }
        try {
            // Allow regular users to access only their own data.
            User currentUser = getCurrentUser();
            return currentUser.getUserId().equals(targetUserId);
        } catch (Exception e) {
            // Deny access if the current user cannot be verified.
            return false;
        }
    }

    /**
     * Checks if the current user is staff/admin OR owns the order user ID.
     */
    public boolean canAccessOrder (Long orderUserId) {

        // Staff and admin can access orders belonging to any user.
        if (isStaffOrAdmin()) {
            return true;
        }
        try {
            // Allow regular users to access only orders linked to their user ID.
            User currentUser = getCurrentUser();
            return currentUser.getUserId().equals(orderUserId);
        } catch (Exception e) {
            // Deny access if the current user cannot be verified.
            return false;
        }
    }

    /**
     * Validates user access, throwing an exception if unauthorized.
     */
    public void validateUserAccess (Long targetUserId) {

        if (!canAccessUser(targetUserId)) {
            throw new ResourceUnavailableException("You are not allowed to access these orders.");
        }
    }

    /**
     * Validates order ownership, throwing an exception if unauthorized.
     */
    public void validateOrderOwnership (Long orderUserId) {

        if (!canAccessOrder(orderUserId)) {
            throw new ResourceUnavailableException("You are not allowed to access or modify this order.");
        }
    }
}
