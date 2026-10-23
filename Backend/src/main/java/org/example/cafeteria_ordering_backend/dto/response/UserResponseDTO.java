package org.example.cafeteria_ordering_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    // Identifies the user and their customer code.
    private Long userId;
    private String customerCode;

    // User's personal and contact details.
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    // Indicates whether the user account is active.
    private Boolean active;

    // Role assigned to the user.
    private String roleName;

    // Timestamps recording when the account was created and last updated.
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}