package org.example.cafeteria_ordering_backend.dto.requests;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

// Defines the data required to create or update a menu items.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// TODO: Investigate lack of DTO use, same goes for response for MenuItem
public class MenuItemRequestDTO {

    // Identifies the category to which the menu item belongs.
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // Stores the menu item's name, limited to 150 characters.
    @NotBlank(message = "Item name is required")
    @Size(max = 150, message = "Item name must not exceed 150 characters")
    private String name;

    // Provides an optional description of the menu item.
    @Size(max = 1000, message = "Description is too long") // Maps to database `text` type
    private String description;

    // Stores the price, allowing up to 8 integer digits and 2 decimal places.
    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be positive or zero")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid (max 8 digits, 2 decimal places)")
    private BigDecimal price;

    // Indicate whether the menu item is available for ordering.
    private Boolean available;

    // Indicates if there is stock available

    // Stores the available stock quantity; cannot be negative.
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    // Stores the pass or URL used to access the menu item's image.
    @Size(max = 255, message = "Image path must not exceed 255 characters") // Maps to `varchar(255)`
    private String imagePath;

    // Determines the display order of the menu item in the menu.
    private Integer displayOrder;
}
