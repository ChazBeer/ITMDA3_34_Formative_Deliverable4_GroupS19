package org.example.cafeteria_ordering_backend.service.interfaces;

import org.example.cafeteria_ordering_backend.entity.Category;
import java.util.List;

public interface CategoryService {

    Category createCategory (Category category);

    // Gets all Categories marked as currently active
    List<Category> getAllActiveCategories ();

    Category getCategoryById (Long id);

    Category updateCategory (Long id, Category categoryDetails);

    void deleteCategory (Long id);
}