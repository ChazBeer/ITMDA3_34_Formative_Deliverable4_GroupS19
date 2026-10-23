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
public class UserProfileDTO {

    // Identifies the user.
    private Long userId;

    // User's personal details.
    private String firstName;
    private String lastName;
    private String email;

    // Unique code assigned to the customer.
    private String customerCode;

    // Role assigned to the user.
    private String role;

    private LocalDateTime lastLogin;
}
