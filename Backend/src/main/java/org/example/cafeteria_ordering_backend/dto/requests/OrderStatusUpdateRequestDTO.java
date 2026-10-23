package org.example.cafeteria_ordering_backend.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Represents a request to update an order's status.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateRequestDTO {

    // Requires a status name with a maximum length of 50 characters.
    @NotBlank(message = "Status name is required")
    @Size(max = 50, message = "Status name must not exceed 50 characters")
    private String statusName;
}