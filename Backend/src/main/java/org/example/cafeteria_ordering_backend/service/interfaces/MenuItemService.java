package org.example.cafeteria_ordering_backend.service.interfaces;

import org.example.cafeteria_ordering_backend.entity.MenuItems;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MenuItemService {

    // Creates a menu item and associates it with a category.
    MenuItems createMenuItem (Long categoryId, MenuItems menuItem);

    MenuItems updateMenuItem (Long id, MenuItems menuItemDetails);

    void deleteMenuItem (Long id);

    // Retrieves all menu items belonging to a specific category.
    List<MenuItems> getMenuItemsByCategory (Long categoryId);

    // Retrieves all menu items.
    List<MenuItems> getAvailableMenuItems ();

    // For manual availability override
    MenuItems updateAvailability (Long id, boolean available);

    List<MenuItems> getAllMenuItems ();

    // Retrieves a menu item by its ID.
    MenuItems getMenuItemById (Long id);

    // Uploads an image and associates it with a menu item.
    MenuItems uploadMenuItemImage (Long menuItemId, MultipartFile file);

    // Deletes the image associated with a menu item.
    MenuItems deleteMenuItemImage (Long MenuItemId);
}