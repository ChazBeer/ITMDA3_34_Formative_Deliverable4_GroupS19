package org.example.cafeteria_ordering_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "menu_item_modifier_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemModifierGroup {

    // Composite primary key linking the menu item to a modifier group.
    @EmbeddedId
    @Builder.Default
    private MenuItemModifierGroupId id = new MenuItemModifierGroupId();

    // Menu item associated with the modifier group.
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("menuItemId")
    @JoinColumn(name = "menu_item_id")
    @JsonIgnore // Stops infinite JSON if entity directly serialized
    private MenuItems menuItem;

    // Modifier group assigned to the menu item.
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("modifierGroupId")
    @JoinColumn(name = "modifier_group_id")
    @JsonIgnore
    private ModifierGroup modifierGroup;

    // Determines the display order of the modifier group for the menu item.
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    // Timestamps recording when the relationship was created and last updated.
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