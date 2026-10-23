
package org.example.cafeteria_ordering_backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.cafeteria_ordering_backend.entity.Category;
import org.example.cafeteria_ordering_backend.service.interfaces.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

//Handles API requests related to cafeteria categories.
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // Create a new category. Only administrators can access this endpoint.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> createCategory (@RequestBody Category category) {

        Category savedCategory = categoryService.createCategory(category);
        return new ResponseEntity<>(savedCategory,HttpStatus.CREATED);
    }

    // Get single category by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'STUDENT')")
    public ResponseEntity<Category> getCategoryById (@PathVariable Long id) {

        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    // Update category (Admin only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> updateCategory (@PathVariable Long id, @RequestBody Category category) {

        Category updatedCategory = categoryService.updateCategory(id, category);
        return ResponseEntity.ok(updatedCategory);
    }

    // Delete category (Admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategory (@PathVariable Long id) {

        categoryService.deleteCategory(id);
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
    }

    // Retrieve all active categories for authorised users.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'STUDENT')")
    public ResponseEntity<List<Category>> getAllCategories () {

        return ResponseEntity.ok(categoryService.getAllActiveCategories());
    }
}

