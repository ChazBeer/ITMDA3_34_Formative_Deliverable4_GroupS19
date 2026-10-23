package org.example.cafeteria_ordering_backend.service.implementation;

import org.example.cafeteria_ordering_backend.dto.response.CustomizationResponseDTO;
import org.example.cafeteria_ordering_backend.entity.MenuItems;
import org.example.cafeteria_ordering_backend.entity.Modifier;
import org.example.cafeteria_ordering_backend.entity.ModifierGroup;
import org.example.cafeteria_ordering_backend.entity.MenuItemModifierGroup;
import org.example.cafeteria_ordering_backend.exception.BusinessException;
import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.mapper.MenuCustomizationMapper;
import org.example.cafeteria_ordering_backend.repository.MenuItemRepository;
import org.example.cafeteria_ordering_backend.repository.ModifierGroupRepository;
import org.example.cafeteria_ordering_backend.repository.ModifierRepository;
import org.example.cafeteria_ordering_backend.service.interfaces.MenuCustomizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class MenuCustomizationServiceImpl implements MenuCustomizationService {

    private final ModifierGroupRepository groupRepository;
    private final ModifierRepository modifierRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCustomizationMapper menuCustomizationMapper;

    // Injects the repositories and mapper required to manage menu customizations.
    public MenuCustomizationServiceImpl (
            ModifierGroupRepository groupRepository,
            ModifierRepository modifierRepository,
            MenuItemRepository menuItemRepository,
            MenuCustomizationMapper menuCustomizationMapper) {

        this.groupRepository = groupRepository;
        this.modifierRepository = modifierRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuCustomizationMapper = menuCustomizationMapper;
    }

    // Builds & returns customizationResponseDTO for menu item by its ID, includes modifier groups and options
    @Transactional(readOnly = true)
    public CustomizationResponseDTO getCustomizationByMenuItemDTO (Long menuItemId) {

        // Retrieves the menu item or throws an exception if it does not exist.
        MenuItems menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + menuItemId));

        // Extract groups ordered by displayOrder
        List<ModifierGroup> sortedGroups = menuItem.getMenuItemModifierGroups()
                .stream()
                .sorted(Comparator.comparingInt(MenuItemModifierGroup::getDisplayOrder))
                .map(MenuItemModifierGroup::getModifierGroup)
                .toList();

        // Collect all groupIds
        List<Long> groupIds = sortedGroups.stream()
                .map(ModifierGroup::getId)
                .toList();

        // Bulk fetch: Get all modifiers for all groups in a single query
        List<Modifier> allModifiers = modifierRepository.findByModifierGroupIdIn(groupIds);

        // Group modifiers by their groupId in memory using Java Streams
        Map<Long, List<Modifier>> modifiersByGroupId = allModifiers.stream()
                .collect(Collectors.groupingBy(modifier -> modifier.getModifierGroup().getId()));

        // Map to DTOs without querying db again
        List<CustomizationResponseDTO.ModifierGroupDTO> groupDTOs = sortedGroups.stream()
                .map(group -> {
                    List<Modifier> groupModifiers = modifiersByGroupId.getOrDefault(group.getId(), Collections.emptyList());
                    return menuCustomizationMapper.toGroupDTO(group, groupModifiers);
                })
                .toList();

        return menuCustomizationMapper.toCustomizationResponseDTO(menuItem, groupDTOs);
    }

    // Creates a modifier group and links it to the specified menu item.
    @Override
    public ModifierGroup createModifierGroup (Long menuItemId, ModifierGroup group) {

        // Retrieves the menu item or throws an exception if it does not exist.
        MenuItems menuItem =menuItemRepository.findById(menuItemId)
                        .orElseThrow(() ->new ResourceNotFoundException("Menu item not found with id: "+ menuItemId));

        // Saves the modifier group before creating the relationship.
        ModifierGroup savedGroup = groupRepository.save(group);

        // Create the relationship between the menu item and modifier group
        MenuItemModifierGroup link = new MenuItemModifierGroup();

        link.setMenuItem(menuItem);
        link.setModifierGroup(savedGroup);

        // Put the new group after existing groups
        link.setDisplayOrder(menuItem.getMenuItemModifierGroups().size() + 1);

        menuItem.getMenuItemModifierGroups().add(link);
        menuItemRepository.save(menuItem);

        return savedGroup;
    }

    // Updates modifier group of given ID
    @Override
    @Transactional
    public ModifierGroup updateModifierGroup (Long groupId, ModifierGroup groupUpdate) {

        ModifierGroup existingGroup = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Modifier group not found with ID: " + groupId));

        existingGroup.setName(groupUpdate.getName());
        existingGroup.setMinSelections(groupUpdate.getMinSelections());
        existingGroup.setMaxSelections(groupUpdate.getMaxSelections());
        existingGroup.setAvailable(groupUpdate.getAvailable());
        existingGroup.setIsRequired(groupUpdate.getIsRequired());

        return groupRepository.save(existingGroup);
    }

    // Deletes modifier group of given ID or throws an error if tied to historical orders
    @Override
    @Transactional
    public void deleteModifierGroup (Long groupId) {

        ModifierGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Modifier group not found with ID: " + groupId));

        try {
            groupRepository.delete(group);
            groupRepository.flush(); // Force execution to trigger constraint check immediately
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException(
                    "Cannot delete this modifier group because one or more of its options have been purchased in past orders. Please consider marking the group as inactive/unavailable instead."
            );
        }
    }

    // Creates a modifier option and associates it with the specified group.
    @Override
    public Modifier createModifier (Long groupId, Modifier modifier) {

        // Retrieves the modifier group or throws an exception if it does not exist.
        ModifierGroup group =groupRepository.findById(groupId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Modifier group not found with id: " + groupId));

        modifier.setModifierGroup(group);
        return modifierRepository.save(modifier);
    }

    // Updates given modifier ID
    @Override
    @Transactional
    public Modifier updateModifier (Long modifierId, Modifier modifierUpdate) {

        Modifier existingModifier = modifierRepository.findById(modifierId)
                .orElseThrow(() -> new ResourceNotFoundException("Modifier not found with ID: " + modifierId));

        existingModifier.setName(modifierUpdate.getName());
        existingModifier.setPriceModifier(modifierUpdate.getPriceModifier());
        existingModifier.setAvailable(modifierUpdate.getAvailable());

        return modifierRepository.save(existingModifier);
    }

    // Deletes modifier of given ID or throws an error if tied to historical orders
    @Override
    @Transactional
    public void deleteModifier (Long modifierId) {

        Modifier modifier = modifierRepository.findById(modifierId)
                .orElseThrow(() -> new ResourceNotFoundException("Modifier not found with ID: " + modifierId));

        try {
            modifierRepository.delete(modifier);
            groupRepository.flush(); // Force execution to trigger constraint check immediately
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Cannot delete this modifier because it has been purchased in past orders. Please consider marking it as inactive/unavailable instead."
            );
        }
    }

    // Retrieves the modifier groups associated with a menu item in display order.
    @Override
    @Transactional(readOnly = true)
    public List<ModifierGroup> getCustomizationsByMenuItem (Long menuItemId) {

        // Retrieves the menu item or throws an exception if it does not exist.
        MenuItems menuItem =menuItemRepository.findById(menuItemId)
                        .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: "+ menuItemId));

        return menuItem.getMenuItemModifierGroups()
                .stream()
                .sorted(Comparator.comparingInt(MenuItemModifierGroup::getDisplayOrder))
                .map(MenuItemModifierGroup::getModifierGroup)
                .toList();
    }
}

