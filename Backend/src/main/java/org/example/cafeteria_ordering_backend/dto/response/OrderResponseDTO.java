package org.example.cafeteria_ordering_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

    // Identifies the order and its customer-facing order number.
    private Long orderId;
    private String customerOrderNumber;

    // Current status of the order.
    private String status;

    // Total price of the order.
    private BigDecimal totalPrice;

    // Date and time when the order was created.
    private LocalDateTime createdAt;

    // Items included in the order.
    private List<OrderItemResponse> items;

    private String customerName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {

        // Name of the ordered menu item.
        private String menuItemName;

        // Number of units ordered.
        private Integer quantity;

        // Price of the item at the time of purchase.
        private BigDecimal priceAtPurchase;

        // Price of the single order item plus modifiers
        private BigDecimal lineTotal;

        // Additional instructions provided for the item.
        private String specialInstructions;

        // Names of the selected modifiers for the item.
        private List<String> selectedModifiers;
    }
}