package org.example.cafeteria_ordering_backend.repository;

import org.example.cafeteria_ordering_backend.entity.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, Long> {

    // Update this method name to match your new relationship property path
    List<ModifierGroup> findByMenuItemModifierGroups_MenuItem_MenuItemId (Long menuItemId);
}