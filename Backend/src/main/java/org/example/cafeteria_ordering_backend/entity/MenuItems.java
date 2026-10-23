package org.example.cafeteria_ordering_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItems {

    // Primary key generated automatically by the database.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_item_id")
    private Long menuItemId;

    // Links this item directly to its parent Category record
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Modifier groups associated with this menu item.
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<MenuItemModifierGroup> menuItemModifierGroups = new java.util.HashSet<>();

    // Basic menu item details.
    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Base price of the menu item.
    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    // Indicates whether the menu item is available for ordering.
    @Column(name = "available")
    @Builder.Default
    private Boolean available = true;

    // Tracks the remaining quantity available for ordering.
    @Builder.Default
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    // Path used to access the menu item's image.
    @Column(name = "image_path")
    private String imagePath;

    // Determines the display order of the menu item.
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    // Timestamps recording when the menu item was created and last updated.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Sets the creation and update timestamps before the entity is first saved.
    @PrePersist
    protected void onCreate () {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // Updates the modification timestamp before an existing entity is updated.
    @PreUpdate
    protected void onUpdate () {
        updatedAt = LocalDateTime.now();
    }
}