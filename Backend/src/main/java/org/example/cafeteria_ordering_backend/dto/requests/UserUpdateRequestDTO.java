package org.example.cafeteria_ordering_backend.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Contains the details required to update an existing user account.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDTO {

    // Requires a first name with a maximum length of 255 characters.
    @Size(max = 255, message = "First name must not exceed 255 characters")
    private String firstName;

    // Requires a last name with a maximum length of 255 characters.
    @Size(max = 255, message = "Last name must not exceed 255 characters")
    private String lastName;

    // Requires a valid email address with a maximum length of 255 characters.
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    // Stores an optional phone number with a maximum length of 20 characters.
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Please provide a valid phone number")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    // Requires a password value between 8 and 255 characters.
    @NotBlank(message = "Current password is required to save changes")
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    private String currentPassword;

    // Requires a password value between 8 and 255 characters.
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    private String newPassword;
}