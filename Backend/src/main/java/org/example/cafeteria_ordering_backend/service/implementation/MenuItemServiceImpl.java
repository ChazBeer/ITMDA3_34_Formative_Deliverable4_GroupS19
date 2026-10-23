package org.example.cafeteria_ordering_backend.service.implementation;

import org.example.cafeteria_ordering_backend.entity.Category;
import org.example.cafeteria_ordering_backend.entity.MenuItems;
import org.example.cafeteria_ordering_backend.exception.BusinessException;
import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.repository.CategoryRepository;
import org.example.cafeteria_ordering_backend.repository.MenuItemRepository;
import org.example.cafeteria_ordering_backend.service.interfaces.ImageStorageService;
import org.example.cafeteria_ordering_backend.service.interfaces.MenuItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class MenuItemServiceImpl implements MenuItemService {

    private static final Logger logger = LoggerFactory.getLogger(MenuItemServiceImpl.class);

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;

    // Injects the repositories and image storage service required to manage menu items.
    @Autowired
    public MenuItemServiceImpl (
            MenuItemRepository menuItemRepository,
            CategoryRepository categoryRepository,
            ImageStorageService imageStorageService) {

        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
    }

    // Creates a menu item and associates it with an existing category.
    @Override
    @Transactional
    public MenuItems createMenuItem (Long categoryId,MenuItems menuItem) {

        logger.info("Creating menu item '{}' for category ID: {}",menuItem.getName(),categoryId);

        // Retrieves the category or throws an exception if it does not exist.
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        menuItem.setCategory(category);

        // Lifecycle callbacks (@PrePersist / @PreUpdate) handle createdAt and updatedAt automatically

        // Sets default values when availability or display order is not provided.
        if (menuItem.getAvailable() == null) {menuItem.setAvailable(true);}
        if (menuItem.getDisplayOrder() == null) {menuItem.setDisplayOrder(0);}
        if (menuItem.getStockQuantity() == null) {menuItem.setStockQuantity(0);}

        MenuItems savedMenuItem = menuItemRepository.save(menuItem);

        logger.info("Menu item '{}' created successfully with ID: {}",
                savedMenuItem.getName(),
                savedMenuItem.getMenuItemId()
        );

        return savedMenuItem;
    }

    @Override
    @Transactional
    public MenuItems updateMenuItem (Long id, MenuItems menuItemDetails) {

        logger.info("Updating menu item with ID: {}", id);

        MenuItems existingItem = getMenuItemById(id);

        existingItem.setName(menuItemDetails.getName());
        existingItem.setDescription(menuItemDetails.getDescription());
        existingItem.setPrice(menuItemDetails.getPrice());

        if (menuItemDetails.getAvailable() != null) {
            existingItem.setAvailable(menuItemDetails.getAvailable());
        }

        if (menuItemDetails.getDisplayOrder() != null) {
            existingItem.setDisplayOrder(menuItemDetails.getDisplayOrder());
        }

        // Allow updating category if a category ID is provided in the request
        if (menuItemDetails.getCategory() != null && menuItemDetails.getCategory().getCategoryId() != null) {
            Long categoryId = menuItemDetails.getCategory().getCategoryId();
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
            existingItem.setCategory(category);
        }

        if (menuItemDetails.getStockQuantity() != null) {
            if (menuItemDetails.getStockQuantity() < 0) {
                throw new BusinessException(
                        "Stock quantity cannot be negative."
                );
            }

            existingItem.setStockQuantity(menuItemDetails.getStockQuantity());

            // Automatically set stock to unavailable if stock reaches zero
            if (existingItem.getStockQuantity() == 0) {
                existingItem.setAvailable(false);
                logger.info("Menu item ID: {} stock reached 0. Automatically setting available to false.", id);
            }
        }

        MenuItems updatedItem = menuItemRepository.save(existingItem);
        logger.info("Menu item with ID: {} updated successfully", id);

        return updatedItem;
    }

    @Override
    @Transactional
    public void deleteMenuItem (Long id) {

        logger.info("Deleting menu item with ID: {}", id);

        MenuItems existingItem = getMenuItemById(id);

        // Clean up associated image from storage provider before deleting row
        if (existingItem.getImagePath() != null && !existingItem.getImagePath().isBlank()) {
            imageStorageService.deleteImage(existingItem.getImagePath());
        }

        menuItemRepository.delete(existingItem);
        logger.info("Menu item with ID: {} deleted successfully", id);
    }

    // Retrieves available menu items for a category in display order.
    @Override
    @Transactional(readOnly = true)
    public List<MenuItems> getMenuItemsByCategory (Long categoryId) {

        logger.info("Fetching available menu items for category ID: {}",categoryId);
        return menuItemRepository.findByCategoryCategoryIdAndAvailableTrueOrderByDisplayOrderAsc(categoryId);
    }

    // Retrieves all menu items, including unavailable items.
    // For Customers: Only returns active/available items
    @Override
    @Transactional(readOnly = true)
    public List<MenuItems> getAvailableMenuItems () {

        logger.info("Fetching all available active menu items");
        return menuItemRepository.findByAvailableTrueOrderByDisplayOrderAsc();
    }

    // Update item availability, items marked false customers will not be able to purchase
    @Override
    @Transactional
    public MenuItems updateAvailability (Long id, boolean available) {
        logger.info("Manually updating availability for menu item ID: {} to {}", id, available);

        MenuItems menuItem = getMenuItemById(id);
        menuItem.setAvailable(available);

        MenuItems updatedItem = menuItemRepository.save(menuItem);
        logger.info("Successfully updated availability for menu item ID: {}", id);

        return updatedItem;
    }

    // For Staff/Admin: Returns everything (active and inactive)
    @Override
    @Transactional(readOnly = true)
    public List<MenuItems> getAllMenuItems () {

        logger.info("Fetching all menu items (including inactive)");
        return menuItemRepository.findAll();
    }

    // Retrieves a menu item by ID or throws an exception if it does not exist.
    @Override
    @Transactional(readOnly = true)
    public MenuItems getMenuItemById (Long id) {

        logger.info("Fetching menu item with ID: {}",id);
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
    }

    // Uploads a new image for a menu item and saves its URL.
    @Override
    @Transactional
    public MenuItems uploadMenuItemImage (Long menuItemId, MultipartFile file) {

        logger.info("Uploading image for menu item ID: {}", menuItemId);

        MenuItems menuItem = getMenuItemById(menuItemId);

        // Delete old image if present (delegates to active provider: Local or Cloudinary)
        if (menuItem.getImagePath() != null && !menuItem.getImagePath().isBlank()) {
            imageStorageService.deleteImage(menuItem.getImagePath());
        }

        // Upload new image using active provider
        String newImageUrl = imageStorageService.uploadImage(file, "menu-items");

        // Update entity and save
        menuItem.setImagePath(newImageUrl);

        MenuItems updatedMenuItem = menuItemRepository.save(menuItem);
        logger.info("Successfully updated image for menu item ID: {} -> {}", menuItemId, newImageUrl);

        return updatedMenuItem;
    }

    // Deletes a menu item's image and clears its stored URL.
    @Override
    @Transactional
    public MenuItems deleteMenuItemImage (Long menuItemId) {

        logger.info("Deleting image for menu item ID: {}", menuItemId);

        MenuItems menuItem = getMenuItemById(menuItemId);

        if (menuItem.getImagePath() != null && !menuItem.getImagePath().isBlank()) {
            // Delegates file deletion to active provider (Local disk or Cloudinary)
            imageStorageService.deleteImage(menuItem.getImagePath());

            // Remove image reference from entity
            menuItem.setImagePath(null);

            return menuItemRepository.save(menuItem);
        }

        // Returns the menu item unchanged if it has no image.
        return menuItem;
    }
}

