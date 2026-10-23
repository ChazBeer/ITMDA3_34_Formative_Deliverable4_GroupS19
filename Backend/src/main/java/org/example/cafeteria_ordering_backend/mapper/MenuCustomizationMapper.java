package org.example.cafeteria_ordering_backend.mapper;

import org.example.cafeteria_ordering_backend.dto.response.CustomizationResponseDTO;
import org.example.cafeteria_ordering_backend.entity.MenuItems;
import org.example.cafeteria_ordering_backend.entity.Modifier;
import org.example.cafeteria_ordering_backend.entity.ModifierGroup;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class MenuCustomizationMapper {

    /**
     * Builds the full CustomizationResponseDTO for a MenuItem and its modifier groups.
     */
    public CustomizationResponseDTO toCustomizationResponseDTO (
            MenuItems menuItem, List<CustomizationResponseDTO.ModifierGroupDTO> groupDTOs) {

        if (menuItem == null) return null;

        return new CustomizationResponseDTO(
                menuItem.getMenuItemId(),
                menuItem.getName(),
                menuItem.getPrice(),
                groupDTOs != null ? groupDTOs : Collections.emptyList()
        );
    }

    /**
     * Converts a modifier group and its modifiers into a modifier group DTO.
     */
    public CustomizationResponseDTO.ModifierGroupDTO toGroupDTO (ModifierGroup group, List<Modifier> modifiers) {

        if (group == null) return null;

        // Converts each modifier into an option DTO, or uses an empty list if none are provided.
        List<CustomizationResponseDTO.ModifierOptionDTO> options = (modifiers != null)
                ? modifiers.stream().map(this::toOptionDTO).toList()
                : Collections.emptyList();

        // Determine selection type string ("SINGLE" or "MULTIPLE")
        String selectionType = (group.getMaxSelections() != null && group.getMaxSelections() == 1)
                ? "SINGLE" : "MULTIPLE";

        return new CustomizationResponseDTO.ModifierGroupDTO(
                group.getId(),
                group.getName(),
                selectionType,
                group.getMinSelections(),
                group.getMaxSelections(),
                options
        );
    }

    /**
     * Converts a modifier entity into a modifier option DTO.
     */
    public CustomizationResponseDTO.ModifierOptionDTO toOptionDTO (Modifier modifier) {

        if (modifier == null) return null;

        return new CustomizationResponseDTO.ModifierOptionDTO(
                modifier.getId(),
                modifier.getName(),
                modifier.getPriceModifier(),
                modifier.getAvailable() // Maps cleanly to isAvailable field in ModifierOptionDTO
        );
    }
}