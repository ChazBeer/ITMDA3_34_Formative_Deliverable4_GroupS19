package org.example.cafeteria_ordering_backend.repository;

import org.example.cafeteria_ordering_backend.entity.MenuItemModifierGroup;
import org.example.cafeteria_ordering_backend.entity.MenuItemModifierGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuItemModifierGroupRepository extends JpaRepository<MenuItemModifierGroup, MenuItemModifierGroupId> {

}