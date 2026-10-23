package org.example.cafeteria_ordering_backend.component;

import org.example.cafeteria_ordering_backend.dto.requests.OrderRequestDTO;
import org.example.cafeteria_ordering_backend.entity.*;
import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.exception.ResourceUnavailableException;
import org.example.cafeteria_ordering_backend.repository.MenuItemRepository;
import org.example.cafeteria_ordering_backend.repository.ModifierRepository;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderItemCalculator {

    // Repositories used to retrieve menu items and modifiers.
    private final MenuItemRepository menuItemRepository;
    private final ModifierRepository modifierRepository;

    public OrderItemCalculator(MenuItemRepository menuItemRepository, ModifierRepository modifierRepository) {
        this.menuItemRepository = menuItemRepository;
        this.modifierRepository = modifierRepository;
    }

    // Validates the requested items, calculates prices, and builds the order items.
    public CalculationResult calculateAndBuildItems (List<OrderRequestDTO.OrderItemRequest> itemRequests, Order order) {

        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new ResourceUnavailableException(
                    "An order must contain at least one item."
            );
        }

        // Check the total requested quantity for each menu item.
        Map<Long, Integer> requestedQuantities = new HashMap<>();

        for (OrderRequestDTO.OrderItemRequest itemRequest : itemRequests) {
            if (itemRequest == null || itemRequest.getMenuItemId() == null) {
                throw new ResourceUnavailableException("Each order item must have a valid menu item ID.");
            }

            if (itemRequest.getQuantity() == null || itemRequest.getQuantity() < 1) {
                throw new ResourceUnavailableException("Order quantity must be at least 1.");
            }

            Long menuItemId = itemRequest.getMenuItemId();

            requestedQuantities.merge(
                    menuItemId,
                    itemRequest.getQuantity(),
                    Math::addExact
            );
        }

        // Collect all menuItems and modifierIds for batch fetching
        Set<Long> menuItemIds = itemRequests.stream()
                .map(OrderRequestDTO.OrderItemRequest::getMenuItemId)
                .collect(Collectors.toSet());

        // Collect unique menu item IDs to retrieve them in one database query.
        Set<Long> modifierIds = itemRequests.stream()
                .filter(req -> req.getModifierIds() != null)
                .flatMap(req -> req.getModifierIds().stream())
                .collect(Collectors.toSet());

        // Batch fetch from DB for faster processing over manually fetching every line
        Map<Long, MenuItems> menuItemMap = menuItemRepository.findAllById(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItems::getMenuItemId, Function.identity()));

        Map<Long, Modifier> modifierMap = modifierIds.isEmpty() ? Map.of() :
                modifierRepository.findAllById(modifierIds).stream()
                        .collect(Collectors.toMap(Modifier::getId, Function.identity()));

        // Store the completed order items and initialise the order total.
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal orderTotalPrice = BigDecimal.ZERO;

        // Build OrderItems
        for (OrderRequestDTO.OrderItemRequest itemRequest : itemRequests) {
           // Find the requested menu item or report that it does not exist.
            MenuItems menuItem = menuItemMap.get(itemRequest.getMenuItemId());
            if (menuItem == null) {
                throw new ResourceNotFoundException("Menu item not found with ID: " + itemRequest.getMenuItemId());
            }

            // Prevent unavailable menu items from being ordered.
            if (!Boolean.TRUE.equals(menuItem.getAvailable())) {
                throw new ResourceUnavailableException("Menu item '" + menuItem.getName() +
                        "' is currently out of stock.");
            }

            // Resolve and map selected modifiers for this item request
            List<Modifier> selectedModifiers = new ArrayList<>();
            if (itemRequest.getModifierIds() != null && !itemRequest.getModifierIds().isEmpty()) {
                for (Long modifierId : itemRequest.getModifierIds()) {
                    Modifier modifier = modifierMap.get(modifierId);
                    if (modifier == null) {
                        throw new ResourceNotFoundException("Modifier not found with ID: " + modifierId);
                    }
                    if (!Boolean.TRUE.equals(modifier.getAvailable())) {
                        throw new ResourceUnavailableException("Modifier '" + modifier.getName() +
                                "' is currently unavailable.");
                    }
                    selectedModifiers.add(modifier);
                }
            }

            // Validate modifier group constraints
            validateModifierGroupConstraints(menuItem, selectedModifiers);

            // Create an order item and associate it with the order and menu item.
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemRequest.getQuantity());

            // Save the item name and price at purchase time.
            orderItem.setMenuItemNameAtPurchase(menuItem.getName());
            orderItem.setPriceAtPurchase(Objects.requireNonNullElse(menuItem.getPrice(), BigDecimal.ZERO));

            // Store any special instructions provided for this item.
            orderItem.setSpecialInstructions(itemRequest.getSpecialInstructions());

            // Build order item modifiers
            for (Modifier modifier : selectedModifiers) {
                OrderItemModifier orderItemModifier = new OrderItemModifier();
                orderItemModifier.setModifier(modifier);
                orderItemModifier.setModifierNameAtPurchase(modifier.getName());
                orderItemModifier.setPriceAtPurchase(
                        Objects.requireNonNullElse(modifier.getPriceModifier(), BigDecimal.ZERO));
                orderItem.addModifier(orderItemModifier);
            }

            // Add the completed item and update the running order total.
            orderItems.add(orderItem);
            orderTotalPrice = orderTotalPrice.add(orderItem.calculateSubtotal());
        }

        // Return the completed order item and their combined total.
        return new CalculationResult(orderItems, orderTotalPrice);
    }

    /**
     * Validates that the selected modifiers for a given menu item satisfy
     * the minSelections, maxSelections, and isRequired constraints of each modifier group.
     */
    private void validateModifierGroupConstraints (MenuItems menuItem, List<Modifier> selectedModifiers) {

        // Get all modifier groups linked to this menu item
        Set<MenuItemModifierGroup> itemGroups = menuItem.getMenuItemModifierGroups();
        if (itemGroups == null || itemGroups.isEmpty()) {
            if (!selectedModifiers.isEmpty()) {
                throw new ResourceUnavailableException("Menu item '" + menuItem.getName() + "' does not accept any modifiers.");
            }
            return;
        }

        Set<Long> allowedGroupIds = itemGroups.stream()
                .map(MenuItemModifierGroup::getModifierGroup)
                .filter(Objects::nonNull)
                .map(ModifierGroup::getId)
                .collect(Collectors.toSet());

        for (Modifier modifier : selectedModifiers) {
            if (modifier.getModifierGroup() == null
                    || !allowedGroupIds.contains(modifier.getModifierGroup().getId())) {
                throw new ResourceUnavailableException(
                        "Modifier '" + modifier.getName()
                                + "' is not available for menu item '"
                                + menuItem.getName() + "'."
                );
            }
        }

        // Group selected modifiers by their parent ModifierGroup ID for easy counting
        Map<Long, Long> selectionCountsByGroup = selectedModifiers.stream()
                .map(Modifier::getModifierGroup)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(ModifierGroup::getId, Collectors.counting()));

        // Validate each group configuration assigned to this menu item
        for (MenuItemModifierGroup link : itemGroups) {
            ModifierGroup group = link.getModifierGroup();
            if (group == null || !Boolean.TRUE.equals(group.getAvailable())) {
                continue; // Skip inactive/missing groups
            }

            long selectedCount = selectionCountsByGroup.getOrDefault(group.getId(), 0L);

            // Check if group is required or has a minimum selection rule
            boolean isRequired = Boolean.TRUE.equals(group.getIsRequired()) || (group.getMinSelections() != null && group.getMinSelections() > 0);
            int min = group.getMinSelections() != null ? group.getMinSelections() : (Boolean.TRUE.equals(group.getIsRequired()) ? 1 : 0);
            int max = group.getMaxSelections() != null ? group.getMaxSelections() : Integer.MAX_VALUE;

            if (selectedCount < min) {
                throw new ResourceUnavailableException(
                        "Modifier group '" + group.getName() + "' requires at least " + min + " selection(s)."
                );
            }

            if (selectedCount > max) {
                throw new ResourceUnavailableException(
                        "Modifier group '" + group.getName() + "' allows a maximum of " + max + " selection(s)."
                );
            }
        }
    }

    public record CalculationResult(List<OrderItem> orderItems, BigDecimal totalPrice) {}
}