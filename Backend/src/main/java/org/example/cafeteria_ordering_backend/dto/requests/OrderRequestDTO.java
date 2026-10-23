package org.example.cafeteria_ordering_backend.dto.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

// Represents an order submitted by a user.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {

    //Identifies the user placing the order.
    @NotNull(message = "User ID is required")
    private Long userId;

    // Contains the menu items included in the order.
    // Each item is validated using its own validation rules.
    @NotEmpty(message = "Order must contain at least one item")
    @Valid // Triggers validation on each OrderItemRequest inside the list
    private List<OrderItemRequest> items;

    // Represents an individual item included in an order.
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {

        // Identifies the menu item being ordered.
        @NotNull(message = "Menu item ID is required")
        private Long menuItemId;

        // Specifies the number of units being ordered.
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        // Stores any special instructions for preparing the item.
        @Size(max = 255, message = "Special instructions must not exceed 255 characters")
        private String specialInstructions;

        // Contains the IDs of the modifiers selected for the item.
        private List<Long> modifierIds;
    }
}