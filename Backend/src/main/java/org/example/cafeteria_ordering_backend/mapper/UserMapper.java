package org.example.cafeteria_ordering_backend.mapper;

import org.example.cafeteria_ordering_backend.dto.requests.UserCreateRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserProfileDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserResponseDTO;
import org.example.cafeteria_ordering_backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    // Converts a user creation request DTO into a User entity.
    public User toUserEntity (UserCreateRequestDTO dto) {

        if (dto == null) return null;

        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(dto.getPasswordHash()); // Raw password - hashed in Service
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setCustomerCode(dto.getCustomerCode());
        user.setActive(true);

        return user;
    }

    // Converts a User entity into a DTO containing the users profile details.
    public UserProfileDTO toUserProfileDTO (User user) {

        if (user == null) return null;

        // Retrieves the role name if a role is assigned to the user.
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;

        return new UserProfileDTO(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCustomerCode(),
                roleName,
                user.getLastLogin()
        );
    }

    // Converts a User entity into a DTO containing the user's details for API responses.
    public UserResponseDTO toUserResponseDTO (User user) {

        if (user == null) return null;

        // Retrieves the role name if a role is assigned to the user.
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;

        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .customerCode(user.getCustomerCode())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .active(user.getActive())
                .roleName(roleName)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}