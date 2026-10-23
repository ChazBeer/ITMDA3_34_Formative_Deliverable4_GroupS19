package org.example.cafeteria_ordering_backend.repository;

import org.example.cafeteria_ordering_backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Checks if a category name already exists to prevent duplicates
    boolean existsByCategoryName (String categoryName);

    // Handles category sorting on DB side instead of as java stream
    List<Category> findAllByOrderByDisplayOrderAsc ();
}