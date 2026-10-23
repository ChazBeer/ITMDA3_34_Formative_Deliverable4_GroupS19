package org.example.cafeteria_ordering_backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.cafeteria_ordering_backend.dto.requests.AdminPasswordResetRequestDTO;
import org.example.cafeteria_ordering_backend.dto.requests.UserCreateRequestDTO;
import org.example.cafeteria_ordering_backend.dto.requests.UserUpdateRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserProfileDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserResponseDTO;
import org.example.cafeteria_ordering_backend.service.interfaces.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

// Handles user creation and retrieval of user info
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Create a user with given role
     * Config matcher: POST /api/users?roleId=1
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> registerUser (
            @Valid @RequestBody UserCreateRequestDTO requestDTO, @RequestParam Long roleId) {

        UserResponseDTO responseDTO = userService.createUser(requestDTO, roleId);
        return new ResponseEntity<>(responseDTO,HttpStatus.CREATED);
    }

    /**
     * Get all users as detailed UserResponseDTO list
     * Config matcher: GET /api/users -> ADMIN only
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers () {

        List<UserResponseDTO> users = userService.getAllUserResponses();
        return ResponseEntity.ok(users);
    }

    /**
     * Get detailed user response by ID
     * Config matcher: GET /api/users/{id} -> ADMIN only
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById (@PathVariable Long id) {

        UserResponseDTO userResponse = userService.getUserResponseById(id);
        return ResponseEntity.ok(userResponse);
    }

    /**
     * Get user lightweight profile representation by ID
     * Restricted to profile owner OR Staff/Admin
     */
    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasAnyRole('CUSTOMER', 'STUDENT') " +
            "and #id == authentication.principal.userId)")
    public ResponseEntity<UserProfileDTO> getUserProfileById (@PathVariable Long id) {

        UserProfileDTO profile = userService.getUserProfileById(id);
        return ResponseEntity.ok(profile);
    }

    /**
     * Get all lightweight user profiles
     */
    @GetMapping("/profiles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileDTO>> getAllUserProfiles () {

        List<UserProfileDTO> profiles = userService.getAllUserProfiles();
        return ResponseEntity.ok(profiles);
    }

    /**
     * Get currently authenticated user's detailed profile
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> getCurrentUser (Authentication authentication) {

        String email = authentication.getName();
        UserResponseDTO response = userService.getUserResponseByEmail(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Update currently authenticated user's details
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> updateCurrentUser (
            @Valid @RequestBody UserUpdateRequestDTO requestDTO, Authentication authentication) {

        String email = authentication.getName();
        UserResponseDTO response = userService.updateCurrentUser(email, requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete or deactivate currently authenticated user's account and invalidate tokens
     */
    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteCurrentUser (
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            Authentication authentication) {

        String email = authentication.getName();
        userService.deleteCurrentUser(email, authHeader);

        return ResponseEntity.ok(Map.of("message", "Account successfully deactivated"));
    }

    /**
     * Reset a user's password by ID (Admin only - requires admin password re-auth)
     */
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetUserPassword (
            @PathVariable Long id,
            @Valid @RequestBody AdminPasswordResetRequestDTO requestDTO,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        userService.resetUserPassword(adminEmail, id, requestDTO);

        return ResponseEntity.ok(Map.of("message", "User password successfully reset by administrator"));
    }

    /**
     * Gets all users role name (STUDENT, ADMIN, STAFF, etc). ADMIN use only
     */
    @GetMapping("/role/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getUsersByRole (@PathVariable String roleName) {
        List<UserResponseDTO> users = userService.getUsersByRoleName(roleName);
        return ResponseEntity.ok(users);
    }

    /**
     * Suspends non admin user, sets active status to false. ADMIN use only
     */
    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> suspendUser (@PathVariable Long id) {
        UserResponseDTO suspendedUser = userService.suspendUser(id);
        return ResponseEntity.ok(suspendedUser);
    }

    /**
     * Unsuspends a user by setting their active status to true. ADMIN use only
     */
    @PutMapping("/{id}/unsuspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> unsuspendUser (@PathVariable Long id) {
        UserResponseDTO unsuspendedUser = userService.unsuspendUser(id);
        return ResponseEntity.ok(unsuspendedUser);
    }

    /**
     * Gets users filtered by active status (e.g., active = false(0) for suspended users). Admin use only
     */
    @GetMapping("/status/{active}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getUsersByActiveStatus (@PathVariable Boolean active) {
        List<UserResponseDTO> users = userService.getUsersByActiveStatus(active);
        return ResponseEntity.ok(users);
    }
}