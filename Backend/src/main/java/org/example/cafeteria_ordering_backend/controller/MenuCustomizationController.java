package org.example.cafeteria_ordering_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.cafeteria_ordering_backend.entity.Modifier;
import org.example.cafeteria_ordering_backend.entity.ModifierGroup;
import org.example.cafeteria_ordering_backend.service.interfaces.MenuCustomizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Handles menu item customizations, including modifier groups and modifiers.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menu-items")
public class MenuCustomizationController {

    private final MenuCustomizationService customizationService;

    // Create a modifier group for a menu item. Only administrators can access this endpoint.
    // POST /api/menu-items/{menuItemId}/modifier-groups
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{menuItemId}/modifier-groups")
    public ResponseEntity<ModifierGroup> createModifierGroup (
            @PathVariable Long menuItemId, @Valid @RequestBody ModifierGroup group) {

        ModifierGroup savedGroup =customizationService.createModifierGroup(menuItemId,group);

        return new ResponseEntity<>(savedGroup, HttpStatus.CREATED);
    }

    // Update an existing modifier group
    @PutMapping("/modifier-groups/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModifierGroup> updateModifierGroup (
            @PathVariable Long groupId, @Valid @RequestBody ModifierGroup groupUpdate) {

        ModifierGroup updated = customizationService.updateModifierGroup(groupId, groupUpdate);
        return ResponseEntity.ok(updated);
    }

    // Delete a modifier group
    @DeleteMapping("/modifier-groups/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteModifierGroup (@PathVariable Long groupId) {

        customizationService.deleteModifierGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    // Create a modifier within an existing modifier group. Only administrators can access this endpoint.
    // POST /api/menu-items/modifier-groups/{groupId}/modifiers
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/modifier-groups/{groupId}/modifiers")
    public ResponseEntity<Modifier> createModifier (@PathVariable Long groupId, @Valid @RequestBody Modifier modifier) {

        Modifier savedModifier = customizationService.createModifier(groupId, modifier);

        return new ResponseEntity<>(savedModifier, HttpStatus.CREATED);
    }

    // Update an existing modifier option
    @PutMapping("/modifiers/{modifierId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Modifier> updateModifier (
            @PathVariable Long modifierId, @Valid @RequestBody Modifier modifierUpdate) {

        Modifier updated = customizationService.updateModifier(modifierId, modifierUpdate);
        return ResponseEntity.ok(updated);
    }

    // Delete a modifier option
    @DeleteMapping("/modifiers/{modifierId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteModifier (@PathVariable Long modifierId) {

        customizationService.deleteModifier(modifierId);
        return ResponseEntity.noContent().build();
    }

    // Retrieve the available customization groups for a specific menu item.
    // GET /api/menu-items/{menuItemId}/customizations
    @GetMapping("/{menuItemId}/customizations")
    public ResponseEntity<List<ModifierGroup>> getCustomizations (@PathVariable Long menuItemId) {
        List<ModifierGroup> groups = customizationService.getCustomizationsByMenuItem(menuItemId);
        return ResponseEntity.ok(groups);
    }
}

