package org.example.cafeteria_ordering_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// TODO: Investigate this DTO's lack of use
public class MenuItemResponseDTO {

    // Identifies the menu item.
    private Long menuItemId;

    // Identifies the category the menu item belongs to.
    private Long categoryId;
    private String categoryName;

    // Basic menu item details
    private String name;
    private String description;
    private BigDecimal price;

    // Indicates whether the menu item is available for ordering.
    private Boolean available;

    // Indicates whether there is available stock.
    private Integer stockQuantity;

    // Path used to access the menu item's image.
    private String imagePath;

    // Determines the display order of the menu item.
    private Integer displayOrder;
}