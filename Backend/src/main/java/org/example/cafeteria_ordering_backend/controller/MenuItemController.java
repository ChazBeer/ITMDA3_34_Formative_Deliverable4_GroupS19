
        package org.example.cafeteria_ordering_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.cafeteria_ordering_backend.entity.MenuItems;
import org.example.cafeteria_ordering_backend.service.interfaces.MenuItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.example.cafeteria_ordering_backend.entity.Category;

import java.util.List;

// Handles menu item retrieval, creation and image management.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menu-items")
public class MenuItemController {

    private final MenuItemService menuItemService;

    // Create a menu item within a specified category. Only administrators can access this endpoint.
    @PostMapping("/category/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MenuItems> createMenuItem (
            @PathVariable Long categoryId, @Valid @RequestBody MenuItems menuItem) {

        // Initialise the category if it is missing from the request body.
        if (menuItem.getCategory() == null) {
            menuItem.setCategory(new Category());
        }

        MenuItems savedItem = menuItemService.createMenuItem(categoryId, menuItem);

        return new ResponseEntity<>(savedItem,HttpStatus.CREATED);
    }

    // Update an existing menu item. ADMIN
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MenuItems> updateMenuItem (@PathVariable Long id, @Valid @RequestBody MenuItems menuItem) {

        MenuItems updatedItem = menuItemService.updateMenuItem(id, menuItem);
        return ResponseEntity.ok(updatedItem);
    }

    // Delete a menu item by its ID. ADMIN
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMenuItem (@PathVariable Long id) {

        menuItemService.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }

    // Retrieve all menu items belonging to a specific category.
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<MenuItems>> getMenuItemsByCategory (@PathVariable Long categoryId) {

        return ResponseEntity.ok(menuItemService.getMenuItemsByCategory(categoryId));
    }

    // PUBLIC / CUSTOMER / STUDENT: Get only available menu items for ordering
    @GetMapping
    public ResponseEntity<List<MenuItems>> getAvailableMenuItems () {

        return ResponseEntity.ok(menuItemService.getAvailableMenuItems());
    }

    // Manually toggle an item's availability (regardless of stock). ADMIN / STAFF
    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<MenuItems> updateAvailability (@PathVariable Long id, @RequestParam boolean available) {

        MenuItems updatedItem = menuItemService.updateAvailability(id, available);
        return ResponseEntity.ok(updatedItem);
    }

    // STAFF / ADMIN: Get all menu items including inactive ones for management
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<List<MenuItems>> getAllMenuItems () {

        return ResponseEntity.ok(menuItemService.getAllMenuItems());
    }

    // Retrieve a menu item by its ID.
    @GetMapping("/{id}")
    public ResponseEntity<MenuItems> getMenuItemById (@PathVariable Long id) {

        return ResponseEntity.ok(menuItemService.getMenuItemById(id)
        );
    }

    // Upload or replace the image associated with a menu item.
    // Only administrators and staff can access this endpoint.
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<MenuItems> uploadMenuItemImage (
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {

        MenuItems updatedMenuItem = menuItemService.uploadMenuItemImage(id, file);

        return ResponseEntity.ok(updatedMenuItem);
    }

    // Remove the image associated with a menu item.
    // Only administrators and staff can access this endpoint.
    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<MenuItems> deleteMenuItemImage (@PathVariable Long id) {

        MenuItems updatedMenuItem = menuItemService.deleteMenuItemImage(id);
        return ResponseEntity.ok(updatedMenuItem);
    }
}

