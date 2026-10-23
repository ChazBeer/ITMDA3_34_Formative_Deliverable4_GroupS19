package org.example.cafeteria_ordering_backend.mapper;

import org.example.cafeteria_ordering_backend.dto.response.OrderResponseDTO;
import org.example.cafeteria_ordering_backend.entity.Order;
import org.example.cafeteria_ordering_backend.entity.OrderItem;
import org.example.cafeteria_ordering_backend.entity.OrderItemModifier;
import org.example.cafeteria_ordering_backend.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class OrderMapper {

    // Converts and order entity into a response DTO for the frontend.
    public OrderResponseDTO toResponseDTO(Order order) {
        if (order == null) return null;

        // Converts non-null order items into response DTOs.
        List<OrderResponseDTO.OrderItemResponse> itemDTOs = (order.getOrderItems() != null)
                ? order.getOrderItems().stream()
                .filter(Objects::nonNull)
                .map(this::toItemResponse)
                .toList()
                : Collections.emptyList();

        // Retrieves the order status name if a status is assigned.
        String statusName = (order.getOrderStatus() != null)
                ? order.getOrderStatus().getStatusName()
                : null;

        // Combine first and last name inline
        String customerName = (order.getUser() != null)
                ? ((order.getUser().getFirstName() != null ? order.getUser().getFirstName() : "") + " " +
                (order.getUser().getLastName() != null ? order.getUser().getLastName() : "")).trim()
                : null;

        return new OrderResponseDTO(
                order.getOrderId(),
                order.getCustomerOrderNumber(),
                statusName,
                order.getTotalPrice(),
                order.getCreatedAt(),
                itemDTOs,
                customerName
        );
    }

    // Converts an order item into its response DTO.
    private OrderResponseDTO.OrderItemResponse toItemResponse (OrderItem item) {

        if (item == null) return null;

        // Collects the name of the modifiers selected for this order item.
        List<String> modifiersChosen = (item.getOrderItemModifiers() != null)
                ? item.getOrderItemModifiers().stream()
                .filter(Objects::nonNull) // Safely skip null records
                .map(OrderItemModifier::getModifierNameAtPurchase)
                .toList()
                : Collections.emptyList();

        // Calculate sum of modifier prices chosen for this item
        BigDecimal modifiersTotal = (item.getOrderItemModifiers() != null)
                ? item.getOrderItemModifiers().stream()
                .filter(Objects::nonNull)
                .map(mod -> mod.getPriceAtPurchase() != null ? mod.getPriceAtPurchase() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        // Base price + modifiers total
        BigDecimal unitTotal = (item.getPriceAtPurchase() != null ? item.getPriceAtPurchase() : BigDecimal.ZERO)
                .add(modifiersTotal);

        // Multiply by quantity to get the final line total
        BigDecimal lineTotal = unitTotal.multiply(BigDecimal.valueOf(item.getQuantity()));

        return new OrderResponseDTO.OrderItemResponse(
                item.getMenuItemNameAtPurchase(),
                item.getQuantity(),
                item.getPriceAtPurchase(),
                lineTotal,
                item.getSpecialInstructions(),
                modifiersChosen
        );
    }
}