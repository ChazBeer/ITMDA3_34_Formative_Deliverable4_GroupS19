package org.example.cafeteria_ordering_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "modifier_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ModifierGroup {

    // Primary key, generated automatically by the database.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "modifier_group_id")
    private Long id;

    // Name of the modifier group.
    @Column(nullable = false, length = 100)
    private String name;

    // Minimum and maximum number of options that can be selected.
    @Column(name = "minimum_selections", nullable = false)
    @Builder.Default
    private Integer minSelections = 0;

    @Column(name = "maximum_selections", nullable = false)
    @Builder.Default
    private Integer maxSelections = 1;

    // Indicates whether the modifier group is available for use.
    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    // Indicates whether selecting an option from this group is required.
    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    // Menu items associated with this modifier group.
    @OneToMany(mappedBy = "modifierGroup", cascade = CascadeType.ALL, orphanRemoval = true) // <-- Fixed mapping target
    @Builder.Default
    private Set<MenuItemModifierGroup> menuItemModifierGroups = new java.util.HashSet<>();

    // Modifier options belonging to this group.
    @OneToMany(mappedBy = "modifierGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Modifier> modifiers;

    // Timestamps recording when the group was created and last updated.
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    // Sets the creation and update timestamps before the entity is first saved.
    @PrePersist
    protected void onCreate () {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    // Updates the modification timestamp before an existing entity is updated.
    @PreUpdate
    protected void onUpdate () {
        updatedAt = java.time.LocalDateTime.now();
    }
}