package org.example.cafeteria_ordering_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_item_modifiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemModifier {

    // Primary key, generated automatically by the database.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_modifier_id")
    private Long orderItemModifierId;

    // Order item to which this modifier belongs.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    // Modifier selected for the order item.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifier_id", nullable = false)
    private Modifier modifier;

    // Modifier name stored at the time of purchase.
    @Column(name = "modifier_name_at_purchase", length = 150, nullable = false)
    private String modifierNameAtPurchase;

    // Modifier price stored at the time of purchase.
    @Column(name = "price_at_purchase", precision = 10, scale = 2, nullable = false)
    private BigDecimal priceAtPurchase;

    // Timestamp recording when the modifier was added to the order.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Sets the creation timestamp before the entity is first saved.
    @PrePersist
    protected void onCreate () {
        createdAt = LocalDateTime.now();
    }
}