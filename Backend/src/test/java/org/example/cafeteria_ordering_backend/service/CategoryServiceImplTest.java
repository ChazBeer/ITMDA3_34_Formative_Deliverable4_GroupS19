package org.example.cafeteria_ordering_backend.service;

import org.example.cafeteria_ordering_backend.entity.Category;
import org.example.cafeteria_ordering_backend.repository.CategoryRepository;
import org.example.cafeteria_ordering_backend.service.implementation.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    // Inject mocked repo into service being tested
    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category categoryOne;
    private Category categoryTwo;

    @BeforeEach
    void setup() {

        // Initialise categories
        categoryOne = new Category();
        categoryOne.setCategoryId(1L);
        categoryOne.setCategoryName("Beverages");
        categoryOne.setDisplayOrder(2);

        categoryTwo = new Category();
        categoryTwo.setCategoryId(2L);
        categoryTwo.setCategoryName("Hot Meals");
        categoryTwo.setDisplayOrder(1);
    }

    @Nested
    @DisplayName("CreateCategory Method tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should successfully create category with provided display order")
        void createCategory_Success_WithDisplayOrder () {

            // Confirm the category name is unique
            when(categoryRepository.existsByCategoryName("Beverages")).thenReturn(false);

            // Mock saving the category and populating its timestamp
            when(categoryRepository.save(any(Category.class)))
                    .thenAnswer(invocation -> {
                        Category cat = invocation.getArgument(0);
                        cat.setCreatedAt(java.time.LocalDateTime.now());
                        cat.setUpdatedAt(java.time.LocalDateTime.now());
                        return cat;
                    });

            // Create the category using the service
            Category created = categoryService.createCategory(categoryOne);

            // Verify category still has given details
            assertNotNull(created);
            assertEquals("Beverages", created.getCategoryName());
            assertEquals(2, created.getDisplayOrder());

            // Ensure timestamps exist
            assertNotNull(created.getCreatedAt());
            assertNotNull(created.getUpdatedAt());

            // Ensure repo saves category
            verify(categoryRepository, times(1))
                    .save(categoryOne);
        }

        @Test
        @DisplayName("Should default displayOrder to 0 when user provides null")
        void createCategory_Success_DefaultsDisplayOrderToZero () {

            // Create category without display order
            Category inputCategory = new Category();
            inputCategory.setCategoryName("Snacks");
            inputCategory.setDisplayOrder(null);

            // Check category name is unique
            when(categoryRepository.existsByCategoryName("Snacks")).thenReturn(false);

            // Simulate saving category to populate its timestamps
            when(categoryRepository.save(any(Category.class)))
                    .thenAnswer(invocation -> {
                        Category cat = invocation.getArgument(0);
                        cat.setCreatedAt(java.time.LocalDateTime.now());
                        cat.setUpdatedAt(java.time.LocalDateTime.now());
                        return cat;
                    });

            // Create category without display order
            Category created = categoryService.createCategory(inputCategory);

            // Ensure default display order is given
            assertNotNull(created);
            assertEquals(0, created.getDisplayOrder());

            // Check timestamps exist
            assertNotNull(inputCategory.getCreatedAt());
            assertNotNull(inputCategory.getUpdatedAt());

            verify(categoryRepository, times(1)).save(inputCategory);
        }

        @Test
        @DisplayName("Should throw RuntimeException when category name already exists")
        void createCategory_ThrowsException_WhenNameExists () {

            // Setup category name that currently exists
            when(categoryRepository.existsByCategoryName("Beverages")).thenReturn(true);

            // Ensure the repeated category is rejected
            RuntimeException exception = assertThrows(
                            RuntimeException.class,
                            () -> categoryService.createCategory(categoryOne)
                    );

            assertEquals("Category name already exists!",exception.getMessage());

            // Ensure no save occurs for the duplicate
            verify(categoryRepository, never())
                    .save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("GetAllActiveCategories Method Tests")
    class GetAllActiveCategoriesTests {

        @Test
        @DisplayName("Should fetch all categories sorted by displayOrder ASC from repository")
        void getAllActiveCategories_ReturnsSortedByDisplayOrder () {

            // Mock repo returning categories in ascending order
            when(categoryRepository.findAllByOrderByDisplayOrderAsc())
                    .thenReturn(Arrays.asList(
                            categoryTwo, // displayOrder 1
                            categoryOne  // displayOrder 2
                    ));

            // Get categories via the service
            List<Category> result = categoryService.getAllActiveCategories();

            // Ensure categories are returned
            assertNotNull(result);
            assertEquals(2, result.size());

            // Hot Meals has display order 1
            assertEquals("Hot Meals", result.get(0).getCategoryName());
            assertEquals(1,result.get(0).getDisplayOrder());

            // Beverages has display order 2
            assertEquals("Beverages", result.get(1).getCategoryName());
            assertEquals(2,result.get(1).getDisplayOrder());

            verify(categoryRepository, times(1)).findAllByOrderByDisplayOrderAsc();
        }
    }

    @Nested
    @DisplayName("GetCategoryById Method Tests")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("Should return category when valid ID is provided")
        void getCategoryById_Success () {

            // Setup repo to return category for test
            when(categoryRepository.findById(1L)).thenReturn(java.util.Optional.of(categoryOne));

            Category found = categoryService.getCategoryById(1L);

            // Check correct cat is returned
            assertNotNull(found);
            assertEquals(1L, found.getCategoryId());
            assertEquals("Beverages", found.getCategoryName());

            verify(categoryRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when category ID does not exist")
        void getCategoryById_ThrowsException_WhenNotFound () {

            // Mock lookup for non-existent category
            when(categoryRepository.findById(999L)).thenReturn(java.util.Optional.empty());

            // Check missing cat returns correct exception
            org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException exception = assertThrows(
                            org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException.class,
                            () -> categoryService.getCategoryById(999L)
                    );

            assertTrue(exception.getMessage().contains("Category not found with id: 999"));

            verify(categoryRepository, times(1)).findById(999L);
        }
    }

    @Nested
    @DisplayName("UpdateCategory Method Tests")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Should successfully update category when valid details provided")
        void updateCategory_Success () {

            // Prep new category details
            Category updateDetails = new Category();
            updateDetails.setCategoryName("Cold Beverages");
            updateDetails.setDisplayOrder(3);

            // Config repo to return the existing category and check new name not currently in use
            when(categoryRepository.findById(1L)).thenReturn(java.util.Optional.of(categoryOne));
            when(categoryRepository.existsByCategoryName("Cold Beverages")).thenReturn(false);
            when(categoryRepository.save(any(Category.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Update existing category
            Category updated = categoryService.updateCategory(1L, updateDetails);

            // Verify the category has the updated info
            assertNotNull(updated);
            assertEquals("Cold Beverages", updated.getCategoryName());
            assertEquals(3, updated.getDisplayOrder());

            // Check category can be retrieved name exists and updated entity is saved
            verify(categoryRepository, times(1)).findById(1L);
            verify(categoryRepository, times(1)).existsByCategoryName("Cold Beverages");
            verify(categoryRepository, times(1)).save(categoryOne);
        }

        @Test
        @DisplayName("Should throw RuntimeException when updating to an existing category name")
        void updateCategory_ThrowsException_WhenNameExists () {

            // Prepare an update that uses a category name thats already in use.
            Category updateDetails = new Category();
            updateDetails.setCategoryName("Hot Meals");

            when(categoryRepository.findById(1L)).thenReturn(java.util.Optional.of(categoryOne));
            when(categoryRepository.existsByCategoryName("Hot Meals")).thenReturn(true);

            // Ensure duplicate is rejected with correct exception
            RuntimeException exception = assertThrows(
                            RuntimeException.class,
                            () -> categoryService.updateCategory(1L, updateDetails)
                    );

            assertEquals("Category name already exists!", exception.getMessage());

            // Ensure existing category can be retrieved but is not saved
            verify(categoryRepository, times(1)).findById(1L);
            verify(categoryRepository, never()).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("DeleteCategory Method Tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should successfully delete category when found")
        void deleteCategory_Success () {

            // Setup repo to return existing category
            when(categoryRepository.findById(1L)).thenReturn(java.util.Optional.of(categoryOne));
            doNothing().when(categoryRepository).delete(categoryOne);

            // Delete category by its ID
            categoryService.deleteCategory(1L);

            // Ensure deletion successful
            verify(categoryRepository, times(1)).findById(1L);
            verify(categoryRepository, times(1)).delete(categoryOne);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent category")
        void deleteCategory_ThrowsException_WhenNotFound () {

            // Setup lookup for non-existant category
            when(categoryRepository.findById(999L)).thenReturn(java.util.Optional.empty());

            org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException exception = assertThrows(
                            org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException.class,
                            () -> categoryService.deleteCategory(999L)
                    );

            assertTrue(exception.getMessage().contains("Category not found with id: 999"));

            // ensure lookup happens but no deletion
            verify(categoryRepository, times(1)).findById(999L);
            verify(categoryRepository, never()).delete(any(Category.class));
        }
    }
}