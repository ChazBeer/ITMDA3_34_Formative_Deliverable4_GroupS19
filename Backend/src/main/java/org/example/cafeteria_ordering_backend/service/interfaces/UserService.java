package org.example.cafeteria_ordering_backend.service.interfaces;

import org.example.cafeteria_ordering_backend.dto.requests.AdminPasswordResetRequestDTO;
import org.example.cafeteria_ordering_backend.dto.requests.UserCreateRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserProfileDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserResponseDTO;
import org.example.cafeteria_ordering_backend.dto.requests.UserUpdateRequestDTO;
import org.example.cafeteria_ordering_backend.entity.Role;
import org.example.cafeteria_ordering_backend.entity.User;

import java.util.List;

public interface UserService {

    // Creates a user and assigns the specified role.
    UserResponseDTO createUser (UserCreateRequestDTO requestDTO, Long roleId);

    // Retrieves a user entity by its ID.
    User getUserById (Long id);

    // Retrieves a user's profile by their ID.
    UserProfileDTO getUserProfileById (Long id);

    // Retrieves a user's response details by their ID.
    UserResponseDTO getUserResponseById (Long id);

    // Retrieves all users as user entities.
    List<User> getAllUsers ();

    // Retrieves the profiles of all users.
    List<UserProfileDTO> getAllUserProfiles ();

    // Retrieves response details for all users.
    List<UserResponseDTO> getAllUserResponses ();

    // Updates the profile of the currently authenticated user.
    UserResponseDTO updateCurrentUser (String email, UserUpdateRequestDTO requestDTO);

    // Retrieves a user's response details by their email address.
    UserResponseDTO getUserResponseByEmail (String email);

    // Deactivates the current user's account and handles their authentication tokens.
    void deleteCurrentUser (String email, String authHeader);

    // Resets a user's password through an administrator.
    void resetUserPassword (String adminEmail, Long targetUserId, AdminPasswordResetRequestDTO requestDTO);

    // gets users by their role names
    List<UserResponseDTO> getUsersByRoleName (String roleName);

    // Admins can deactivate and reactivate existing accounts
    UserResponseDTO suspendUser (Long userId);
    UserResponseDTO unsuspendUser (Long userId);

    // Admins can sort users by active status
    public List<UserResponseDTO> getUsersByActiveStatus (Boolean active);
}