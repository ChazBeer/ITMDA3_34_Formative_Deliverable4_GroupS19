package org.example.cafeteria_ordering_backend.service.interfaces;

import org.example.cafeteria_ordering_backend.dto.response.CustomizationResponseDTO;
import org.example.cafeteria_ordering_backend.entity.Modifier;
import org.example.cafeteria_ordering_backend.entity.ModifierGroup;
import java.util.List;

public interface MenuCustomizationService {

    // Retrieves menu customizations in a response DTO for a specific menu item.
    CustomizationResponseDTO getCustomizationByMenuItemDTO (Long menuItemId);

    // Creates a modifier group and associates it with a menu item.
    ModifierGroup createModifierGroup (Long menuItemId, ModifierGroup group);

    // Creates a modifier within an existing modifier group.
    Modifier createModifier (Long groupId, Modifier modifier);

    // Retrieves all modifier groups associated with a menu item.
    List<ModifierGroup> getCustomizationsByMenuItem (Long menuItemId);

    // Updates a modifier group
    ModifierGroup updateModifierGroup (Long groupId, ModifierGroup groupUpdate);

    // Deletes a modifier group
    void deleteModifierGroup (Long groupId);

    // Updates a modifier
    Modifier updateModifier (Long modifierId, Modifier modifierUpdate);

    // Deletes a modifier
    void deleteModifier (Long modifierId);
}