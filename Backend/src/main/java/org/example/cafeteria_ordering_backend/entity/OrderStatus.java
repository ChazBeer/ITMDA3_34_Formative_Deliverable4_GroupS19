package org.example.cafeteria_ordering_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatus {

    // Primary key, generated automatically by the database.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long statusId;

    // Unique name identifying the order status.
    @Column(name = "status_name", length = 50, nullable = false, unique = true)
    private String statusName;

    // Determines the display order of the status.
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    // Indicates whether this status represents the end of the order process.
    @Column(name = "is_final")
    @Builder.Default
    private Boolean isFinal = false;

    // Timestamps recording when the status was created and last updated.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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