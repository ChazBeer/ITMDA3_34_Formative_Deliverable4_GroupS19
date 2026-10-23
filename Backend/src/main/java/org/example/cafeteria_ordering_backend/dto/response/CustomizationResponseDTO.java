package org.example.cafeteria_ordering_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizationResponseDTO {

    // Detail of the menu item being customized.
    private Long menuItemId;
    private String menuItemName;
    private BigDecimal basePrice;

    // Modifier groups available for the menu item.
    private List<ModifierGroupDTO> modifierGroups;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifierGroupDTO {

        // Identifies the modifier group
        private Long modifierGroupId;
        private String groupName;

        // Defines whether one or multiple options can be selected.
        private String selectionType;

        // Minimum and maximum number of options that can be selected.
        private Integer minSelections;
        private Integer maxSelections;

        // Available modifier options within the group.
        private List<ModifierOptionDTO> modifiers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifierOptionDTO {

        // Identifies the modifier option.
        private Long modifierId;
        private String name;

        // Additional cost of selecting this modifier.
        private BigDecimal priceModifier;

        // Indicates whether the modifier can currently be selected.
        private Boolean isAvailable;
    }
}