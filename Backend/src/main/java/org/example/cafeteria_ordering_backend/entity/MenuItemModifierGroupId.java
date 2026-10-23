package org.example.cafeteria_ordering_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class MenuItemModifierGroupId implements Serializable {

    // Identifies the menu item in the composite primary key.
    @Column(name = "menu_item_id")
    private Long menuItemId;

    // Identifies the modifier group in the composite primary key.
    @Column(name = "modifier_group_id")
    private Long modifierGroupId;
}