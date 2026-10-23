package org.example.cafeteria_ordering_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    // Primary key, generated automatically by the database.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    // Order to which this item belongs.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Menu item selected for the order.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItems menuItem;

    // Number of units ordered.
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Menu item name stored at the time of purchase.
    @Column(name = "menu_item_name_at_purchase", length = 150, nullable = false)
    private String menuItemNameAtPurchase;

    // Unit price stored at the time of purchase.
    @Column(name = "price_at_purchase", precision = 10, scale = 2, nullable = false)
    private BigDecimal priceAtPurchase;

    // Additional instructions provided for the item.
    @Column(name = "special_instructions", length = 255)
    private String specialInstructions;

    // Timestamps recording when the order item was created and last updated.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Modifiers selected for this order item.
    @Builder.Default
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<OrderItemModifier> orderItemModifiers = new ArrayList<>();

    @PrePersist
    protected void onCreate () {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // Sets the creation and update timestamps before the entity is first saved.
    @PreUpdate
    protected void onUpdate () {
        updatedAt = LocalDateTime.now();
    }

    // Domain & Helper Methods
    /**
     * Adds an OrderItemModifier to the list and sets the parent reference for JPA cascading.
     */
    public void addModifier (OrderItemModifier modifier) {

        if (orderItemModifiers == null) {
            orderItemModifiers = new ArrayList<>();
        }
        orderItemModifiers.add(modifier);
        modifier.setOrderItem(this);
    }

    /**
     * Helper to calc the total subtotal for this line item (unit price including modifiers * quantity).
     */
    public BigDecimal calculateSubtotal () {

        // Null guard for null priceAtPurchase
        BigDecimal basePrice = this.priceAtPurchase != null ? this.priceAtPurchase : BigDecimal.ZERO;

        // Add modifier prices
        if (this.orderItemModifiers != null) {
            for (OrderItemModifier modifier : this.orderItemModifiers) {
                BigDecimal modifierPrice = (modifier != null && modifier.getPriceAtPurchase() != null)
                        ? modifier.getPriceAtPurchase()
                        : BigDecimal.ZERO;
                basePrice = basePrice.add(modifierPrice);
            }
        }

        // Null guard for null quantity
        int qty = this.quantity != null ? this.quantity : 0;

        return basePrice.multiply(BigDecimal.valueOf(qty));
    }
}