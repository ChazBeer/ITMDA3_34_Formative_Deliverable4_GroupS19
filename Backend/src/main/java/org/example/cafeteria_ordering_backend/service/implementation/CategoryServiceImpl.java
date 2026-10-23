package org.example.cafeteria_ordering_backend.service.implementation;

import org.example.cafeteria_ordering_backend.entity.Category;
import org.example.cafeteria_ordering_backend.exception.BusinessException;
import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.repository.CategoryRepository;
import org.example.cafeteria_ordering_backend.service.interfaces.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);
    private final CategoryRepository categoryRepository;

    // Injects the repository used to manage menu categories.
    @Autowired
    public CategoryServiceImpl (CategoryRepository categoryRepository) {

        this.categoryRepository = categoryRepository;
    }

    // Creates a category after checking that its name is not already in use.
    @Transactional
    @Override
    public Category createCategory (Category category) {

        logger.info("Creating category: {}", category.getCategoryName());

        // Prevents duplicate category names.
        if (categoryRepository.existsByCategoryName(category.getCategoryName())) {

            logger.warn("Category creation failed - category already exists: {}",category.getCategoryName());

            throw new BusinessException("Category name already exists!");
        }

        // Assigns the default display order when none is provided.
        if (category.getDisplayOrder() == null) {category.setDisplayOrder(0);}

        Category savedCategory = categoryRepository.save(category);

        logger.info("Category '{}' created successfully with ID: {}",
                savedCategory.getCategoryName(),
                savedCategory.getCategoryId()
        );

        return savedCategory;
    }

    @Override
    public Category getCategoryById (Long id) {

        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Override
    @Transactional
    public Category updateCategory (Long id, Category categoryDetails) {

        Category category = getCategoryById(id);

        // Optional duplicate check if name is being changed
        if (!category.getCategoryName().equalsIgnoreCase(categoryDetails.getCategoryName())
                && categoryRepository.existsByCategoryName(categoryDetails.getCategoryName())) {
            throw new BusinessException("Category name already exists!");
        }

        category.setCategoryName(categoryDetails.getCategoryName());
        if (categoryDetails.getDisplayOrder() != null) {
            category.setDisplayOrder(categoryDetails.getDisplayOrder());
        }

        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory (Long id) {

        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }

    // Retrieves categories ordered by their display order.
    @Override
    public List<Category> getAllActiveCategories () {
        return categoryRepository.findAllByOrderByDisplayOrderAsc();
    }
}

