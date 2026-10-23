package org.example.cafeteria_ordering_backend.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Represents the login details submitted by a user.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    // Require a non-empty email address in a valid format.
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // Require a non-empty password.
    @NotBlank(message = "Password is required")
    private String password;
}