package org.example.cafeteria_ordering_backend.service;

import org.example.cafeteria_ordering_backend.entity.Category;
import org.example.cafeteria_ordering_backend.entity.MenuItems;
import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.repository.CategoryRepository;
import org.example.cafeteria_ordering_backend.repository.MenuItemRepository;
import org.example.cafeteria_ordering_backend.service.implementation.MenuItemServiceImpl;
import org.example.cafeteria_ordering_backend.service.interfaces.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MenuItemServiceImplTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ImageStorageService imageStorageService;

    // Injecting mocks into service being tested
    @InjectMocks
    private MenuItemServiceImpl menuItemService;

    private Category mockCategory;
    private MenuItems mockMenuItem;

    // Initialize reusable category/ item for testing
    @BeforeEach
    void setUp() {
        mockCategory = new Category();
        mockCategory.setCategoryId(1L);
        mockCategory.setCategoryName("Beverages");

        mockMenuItem = new MenuItems();
        mockMenuItem.setMenuItemId(100L);
        mockMenuItem.setName("Cappuccino");
        mockMenuItem.setPrice(new BigDecimal("35.00"));
        mockMenuItem.setDisplayOrder(1);
        mockMenuItem.setAvailable(true);
    }

    @Nested
    @DisplayName("Create Menu Item Tests")
    class CreateMenuItemTests {

        @Test
        @DisplayName("Should successfully create a menu item with default vals when category exists")
        void createMenuItem_Success() {
            Long categoryId = 1L;

            MenuItems inputMenuItem = new MenuItems();
            inputMenuItem.setName("Cappuccino");
            inputMenuItem.setPrice(new BigDecimal("35.00"));

            // Sim finding category linked with new menu item
            when(categoryRepository.findById(categoryId))
                    .thenReturn(Optional.of(mockCategory));

            // Sim saving menu item and its ID
            when(menuItemRepository.save(any(MenuItems.class)))
                    .thenAnswer(invocation -> {
                        MenuItems itemToSave = invocation.getArgument(0);
                        itemToSave.setMenuItemId(100L);
                        itemToSave.setCreatedAt(LocalDateTime.now());
                        itemToSave.setUpdatedAt(LocalDateTime.now());
                        return itemToSave;
                    });

            MenuItems result = menuItemService.createMenuItem(categoryId, inputMenuItem);

            // Checking saved item has expected category, defailts & timesstamps
            assertThat(result).isNotNull();
            assertThat(result.getMenuItemId()).isEqualTo(100L);
            assertThat(result.getCategory()).isEqualTo(mockCategory);
            assertThat(result.getAvailable()).isTrue();
            assertThat(result.getDisplayOrder()).isEqualTo(0);
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();

            // Check cat was retireved and new item saved
            verify(categoryRepository, times(1)).findById(categoryId);
            verify(menuItemRepository, times(1)).save(inputMenuItem);
        }

        @Test
        @DisplayName("Should preserve displayOrder if provided when creating item")
        void createMenuItem_PreservesDisplayOrder() {
            Long categoryId = 1L;
            mockMenuItem.setDisplayOrder(5);

            // Sim category lookups and menu item saving
            when(categoryRepository.findById(categoryId))
                    .thenReturn(Optional.of(mockCategory));

            when(menuItemRepository.save(any(MenuItems.class)))
                    .thenAnswer(invocation -> {
                        MenuItems itemToSave = invocation.getArgument(0);
                        itemToSave.setUpdatedAt(LocalDateTime.now());
                        return itemToSave;
                    });

            MenuItems result = menuItemService.createMenuItem(categoryId, mockMenuItem);

            // Verify given display order is saved
            assertThat(result.getDisplayOrder()).isEqualTo(5);

            verify(menuItemRepository, times(1)).save(mockMenuItem);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when category ID is not found")
        void createMenuItem_CategoryNotFound_ThrowsException () {

            Long invalidCategoryId = 999L;

            // Simulate category lookup returing no matching cat
            when(categoryRepository.findById(invalidCategoryId))
                    .thenReturn(Optional.empty());

            // Check creation fails and correct exceptoin thrown
            assertThatThrownBy(() ->
                    menuItemService.createMenuItem(invalidCategoryId, mockMenuItem))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found with id: " + invalidCategoryId);

            // Check no menu item saved
            verify(categoryRepository, times(1)).findById(invalidCategoryId);
            verify(menuItemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Menu Item Tests")
    class UpdateMenuItemTests {

        @Test
        @DisplayName("Should successfully update menu item details")
        void updateMenuItem_Success () {

            Long menuItemId = 100L;
            MenuItems details = new MenuItems();
            details.setName("Espresso");
            details.setDescription("Strong black coffee");
            details.setPrice(new BigDecimal("30.00"));
            details.setAvailable(false);
            details.setDisplayOrder(2);

            // Sim getting existing item and saving its new details
            when(menuItemRepository.findById(menuItemId))
                    .thenReturn(Optional.of(mockMenuItem));

            when(menuItemRepository.save(any(MenuItems.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MenuItems result = menuItemService.updateMenuItem(menuItemId, details);

            // Check all given fields are applied to  existing item
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Espresso");
            assertThat(result.getDescription()).isEqualTo("Strong black coffee");
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.getAvailable()).isFalse();
            assertThat(result.getDisplayOrder()).isEqualTo(2);

            // Check existing item can be retrieved and is updated
            verify(menuItemRepository, times(1)).findById(menuItemId);
            verify(menuItemRepository, times(1)).save(mockMenuItem);
        }

        @Test
        @DisplayName("Should update category when category ID is provided in update details")
        void updateMenuItem_WithNewCategory_Success () {

            Long menuItemId = 100L;

            Category newCategory = new Category();
            newCategory.setCategoryId(2L);
            newCategory.setCategoryName("Hot Drinks");

            MenuItems details = new MenuItems();
            details.setName("Cappuccino");
            details.setPrice(new BigDecimal("35.00"));
            details.setCategory(newCategory);

            // Sim getting existing item and its new category
            when(menuItemRepository.findById(menuItemId))
                    .thenReturn(Optional.of(mockMenuItem));
            when(categoryRepository.findById(2L))
                    .thenReturn(Optional.of(newCategory));
            when(menuItemRepository.save(any(MenuItems.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MenuItems result = menuItemService.updateMenuItem(menuItemId, details);

            // Check menu item is assigned to correct category
            assertThat(result.getCategory()).isEqualTo(newCategory);

            // Check new cat is correctly fetched and saved
            verify(categoryRepository, times(1)).findById(2L);
            verify(menuItemRepository, times(1)).save(mockMenuItem);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent menu item")
        void updateMenuItem_NotFound_ThrowsException () {

            Long invalidId = 999L;

            MenuItems details = new MenuItems();

            // Sim lookup for menu item that is non-existent
            when(menuItemRepository.findById(invalidId))
                    .thenReturn(Optional.empty());

            // Check update fails with correct exception
            assertThatThrownBy(() -> menuItemService.updateMenuItem(invalidId, details))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Menu item not found with id: " + invalidId);


            // Verify no update is saved
            verify(menuItemRepository, times(1)).findById(invalidId);
            verify(menuItemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Menu Item Tests")
    class DeleteMenuItemTests {

        @Test
        @DisplayName("Should successfully delete menu item and cleanup image if present")
        void deleteMenuItem_Success_WithImage () {

            Long menuItemId = 100L;
            mockMenuItem.setImagePath("http://localhost:8080/uploads/menu-items/test.jpg");

            // Simulate getting item while its related image and record is deleted
            when(menuItemRepository.findById(menuItemId))
                    .thenReturn(Optional.of(mockMenuItem));
            doNothing().when(imageStorageService).deleteImage(anyString());
            doNothing().when(menuItemRepository).delete(any(MenuItems.class));

            menuItemService.deleteMenuItem(menuItemId);

            // Check its image is removed before the item is deleted
            verify(menuItemRepository, times(1)).findById(menuItemId);
            verify(imageStorageService, times(1)).deleteImage("http://localhost:8080/uploads/menu-items/test.jpg");
            verify(menuItemRepository, times(1)).delete(mockMenuItem);
        }

        @Test
        @DisplayName("Should successfully delete menu item without image cleanup when no image exists")
        void deleteMenuItem_Success_WithoutImage () {

            Long menuItemId = 100L;
            mockMenuItem.setImagePath(null);

            // Getting item without its image
            when(menuItemRepository.findById(menuItemId))
                    .thenReturn(Optional.of(mockMenuItem));
            doNothing().when(menuItemRepository).delete(any(MenuItems.class));

            menuItemService.deleteMenuItem(menuItemId);

            // Is item deleted without calling image cleanup meth
            verify(menuItemRepository, times(1)).findById(menuItemId);
            verify(imageStorageService, never()).deleteImage(any());
            verify(menuItemRepository, times(1)).delete(mockMenuItem);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent menu item")
        void deleteMenuItem_NotFound_ThrowsException () {

            Long invalidId = 999L;

            // Looking up non-existent menu item
            when(menuItemRepository.findById(invalidId))
                    .thenReturn(Optional.empty());

            // Does delete fail with correct exception
            assertThatThrownBy(() -> menuItemService.deleteMenuItem(invalidId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Menu item not found with id: " + invalidId);

            // Verify deletion is not tried as item doesn't exist
            verify(menuItemRepository, times(1)).findById(invalidId);
            verify(menuItemRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Get menu items by category tests")
    class GetMenuItemsByCategoryTests {

        @Test
        @DisplayName("Should return available menu items sorted by display order")
        void getMenuItemsByCategory_ReturnsList () {

            Long categoryId = 1L;

            // Get available items by category
            when(menuItemRepository
                    .findByCategoryCategoryIdAndAvailableTrueOrderByDisplayOrderAsc(categoryId))
                    .thenReturn(List.of(mockMenuItem));

            List<MenuItems> results = menuItemService.getMenuItemsByCategory(categoryId);

            // Checking correct items returned
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("Cappuccino");

            verify(menuItemRepository, times(1))
                    .findByCategoryCategoryIdAndAvailableTrueOrderByDisplayOrderAsc(categoryId);
        }

        @Test
        @DisplayName("Should return empty list when no active items exist in category")
        void getMenuItemsByCategory_ReturnsEmptyList () {

            Long categoryId = 1L;

            // Does a cat with no available items return empty
            when(menuItemRepository
                    .findByCategoryCategoryIdAndAvailableTrueOrderByDisplayOrderAsc(categoryId))
                    .thenReturn(Collections.emptyList());

            List<MenuItems> results = menuItemService.getMenuItemsByCategory(categoryId);

            assertThat(results).isEmpty();

            verify(menuItemRepository, times(1))
                    .findByCategoryCategoryIdAndAvailableTrueOrderByDisplayOrderAsc(categoryId);
        }
    }

    @Nested
    @DisplayName("Get Available Menu Items Tests (Customer Storefront)")
    class GetAvailableMenuItemsTests {

        @Test
        @DisplayName("Should return only available menu items sorted by display order")
        void getAvailableMenuItems_ReturnsList () {

            // Getting avail menu items in their correct display order
            when(menuItemRepository.findByAvailableTrueOrderByDisplayOrderAsc())
                    .thenReturn(List.of(mockMenuItem));

            List<MenuItems> results = menuItemService.getAvailableMenuItems();

            // Check returned items are available
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAvailable()).isTrue();

            verify(menuItemRepository, times(1)).findByAvailableTrueOrderByDisplayOrderAsc();
        }
    }

    @Nested
    @DisplayName("Get All Menu Items Tests (Staff/Admin)")
    class GetAllMenuItemsTests {

        @Test
        @DisplayName("Should return all menu items including inactive ones")
        void getAllMenuItems_ReturnsList () {

            MenuItems inactiveItem = new MenuItems();
            inactiveItem.setMenuItemId(101L);
            inactiveItem.setName("Latte");
            inactiveItem.setAvailable(false);

            // Get all items, including unavailable
            when(menuItemRepository.findAll())
                    .thenReturn(List.of(mockMenuItem, inactiveItem));

            List<MenuItems> results = menuItemService.getAllMenuItems();

            // Check un/available are in list
            assertThat(results).hasSize(2);
            verify(menuItemRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("Get Menu Item By ID Tests")
    class GetMenuItemByIdTests {

        @Test
        @DisplayName("Should return menu item when valid ID is provided")
        void getMenuItemById_Success () {

            Long menuItemId = 100L;

            // Can it get requested item
            when(menuItemRepository.findById(menuItemId))
                    .thenReturn(Optional.of(mockMenuItem));

            MenuItems result = menuItemService.getMenuItemById(menuItemId);

            // Check returned item matches given
            assertThat(result).isNotNull();
            assertThat(result.getMenuItemId()).isEqualTo(menuItemId);
            assertThat(result.getName()).isEqualTo("Cappuccino");

            verify(menuItemRepository, times(1)).findById(menuItemId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when menu item ID is not found")
        void getMenuItemById_NotFound_ThrowsException () {

            Long invalidId = 999L;

            // Lookup a menu item that doesn't exist
            when(menuItemRepository.findById(invalidId))
                    .thenReturn(Optional.empty());

            // Check fails with correct exception
            assertThatThrownBy(() -> menuItemService.getMenuItemById(invalidId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Menu item not found with id: " + invalidId);

            verify(menuItemRepository, times(1)).findById(invalidId);
        }
    }

    @Nested
    @DisplayName("Upload Menu Item Image Tests")
    class UploadMenuItemImageTests {

        @Test
        @DisplayName("Should upload image via ImageStorageService and save new URL")
        void uploadMenuItemImage_Success () {

            Long menuItemId = 100L;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "cappuccino.jpg",
                    "image/jpeg",
                    "fake-image-bytes".getBytes()
            );

            String generatedUrl = "http://localhost:8080/uploads/menu-items/generated-uuid.jpg";

            // Get the menu item and upload image
            when(menuItemRepository.findById(menuItemId))
                    .thenReturn(Optional.of(mockMenuItem));

            when(imageStorageService.uploadImage(eq(file), eq("menu-items")))
                    .thenReturn(generatedUrl);

            // Save item with its new image
            when(menuItemRepository.save(any(MenuItems.class)))
                    .thenAnswer(invocation -> {
                        MenuItems itemToSave = invocation.getArgument(0);
                        itemToSave.setUpdatedAt(LocalDateTime.now());
                        return itemToSave;
                    });

            MenuItems result = menuItemService.uploadMenuItemImage(menuItemId, file);

            // Check generated image url is stored in given menuitem
            assertThat(result.getImagePath()).isEqualTo(generatedUrl);

            verify(menuItemRepository, times(1)).findById(menuItemId);
            verify(imageStorageService, times(1)).uploadImage(file, "menu-items");
            verify(menuItemRepository, times(1)).save(mockMenuItem);
        }

        @Test
        @DisplayName("Should delete old image prior to uploading new image if old image exists")
        void uploadMenuItemImage_ReplacesExistingImage () {

            Long menuItemId = 100L;

            String oldUrl = "http://localhost:8080/uploads/menu-items/old-uuid.jpg";
            String newUrl = "http://localhost:8080/uploads/menu-items/new-uuid.jpg";

            mockMenuItem.setImagePath(oldUrl);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "cappuccino.jpg",
                    "image/jpeg",
                    "fake-image-bytes".getBytes()
            );

            // Get item and replace its existing image
            when(menuItemRepository.findById(menuItemId))
                    .thenReturn(Optional.of(mockMenuItem));

            when(imageStorageService.uploadImage(eq(file), eq("menu-items")))
                    .thenReturn(newUrl);

            when(menuItemRepository.save(any(MenuItems.class)))
                    .thenAnswer(invocation -> {
                        MenuItems itemToSave = invocation.getArgument(0);
                        itemToSave.setUpdatedAt(LocalDateTime.now());
                        return itemToSave;
                    });

            MenuItems result = menuItemService.uploadMenuItemImage(menuItemId, file);

            // Check new URL correctly replaced original
            assertThat(result.getImagePath()).isEqualTo(newUrl);

            // Check old image deleted, reokaced, and saved
            verify(imageStorageService, times(1)).deleteImage(oldUrl);
            verify(imageStorageService, times(1)).uploadImage(file, "menu-items");
            verify(menuItemRepository, times(1)).save(mockMenuItem);
        }
    }

    @Nested
    @DisplayName("Delete Menu Item Image Tests")
    class DeleteMenuItemImageTests {

        @Test
        @DisplayName("Should clear imagePath and delegate deletion to ImageStorageService")
        void deleteMenuItemImage_Success () {

            Long menuItemId = 100L;

            String existingUrl = "http://localhost:8080/uploads/menu-items/sample.jpg";

            mockMenuItem.setImagePath(existingUrl);

            // Get menu item and save update record
            when(menuItemRepository.findById(menuItemId))
                    .thenReturn(Optional.of(mockMenuItem));

            when(menuItemRepository.save(any(MenuItems.class)))
                    .thenAnswer(invocation -> {
                        MenuItems itemToSave = invocation.getArgument(0);
                        itemToSave.setUpdatedAt(LocalDateTime.now());
                        return itemToSave;
                    });

            MenuItems result = menuItemService.deleteMenuItemImage(menuItemId);

            // Ensure image path is empty after delete
            assertThat(result.getImagePath()).isNull();

            // Check stored img is deleted and new menu item saved
            verify(imageStorageService, times(1)).deleteImage(existingUrl);
            verify(menuItemRepository, times(1)).save(mockMenuItem);
        }

        @Test
        @DisplayName("Should handle delete gracefully when no image exists")
        void deleteMenuItemImage_NoExistingImage () {

            Long menuItemId = 100L;

            mockMenuItem.setImagePath(null);

            // Sim getting menu item without an image
            when(menuItemRepository.findById(menuItemId))
                    .thenReturn(Optional.of(mockMenuItem));

            MenuItems result = menuItemService.deleteMenuItemImage(menuItemId);

            // Check item still there after image removal
            assertThat(result.getImagePath()).isNull();

            // Check no DB save occurs
            verify(imageStorageService, never()).deleteImage(any());
            verify(menuItemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Menu Item Availability Tests")
    class UpdateAvailabilityTests {

        @Test
        @DisplayName("Should successfully update menu item availability")
        void updateAvailability_Success () {

            Long menuItemId = 100L;

            mockMenuItem.setAvailable(true);

            // Getting existing item and saving its new availability
            when(menuItemRepository.findById(menuItemId))
                    .thenReturn(Optional.of(mockMenuItem));
            when(menuItemRepository.save(any(MenuItems.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MenuItems result = menuItemService.updateAvailability(menuItemId, false);

            // Check item is returned with availability now false
            assertThat(result).isNotNull();
            assertThat(result.getAvailable()).isFalse();

            verify(menuItemRepository, times(1)).findById(menuItemId);
            verify(menuItemRepository, times(1)).save(mockMenuItem);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating availability for non-existent item")
        void updateAvailability_NotFound_ThrowsException () {

            Long invalidId = 999L;

            // Lookup non-existent menu item
            when(menuItemRepository.findById(invalidId))
                    .thenReturn(Optional.empty());

            // Check availability canoot be changed
            assertThatThrownBy(() -> menuItemService.updateAvailability(invalidId, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Menu item not found with id: " + invalidId);

            // Ensure no DB update
            verify(menuItemRepository, times(1)).findById(invalidId);
            verify(menuItemRepository, never()).save(any());
        }
    }
}