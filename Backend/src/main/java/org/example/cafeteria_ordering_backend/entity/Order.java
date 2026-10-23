package org.example.cafeteria_ordering_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    // Primary key, generated automatically by the database.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    // User who placed the order.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Current status of the order.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    private OrderStatus orderStatus;

    // Unique order number displayed to the customer.
    @Column(name = "customer_order_number", length = 20, unique = true, nullable = false)
    private String customerOrderNumber;

    // Date and time when the order was collected.
    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    // Total price of the order.
    @Column(name = "total_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    // Timestamps recording when the order was created and last updated.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Items included in the order.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

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