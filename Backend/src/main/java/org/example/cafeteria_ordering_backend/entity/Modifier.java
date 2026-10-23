package org.example.cafeteria_ordering_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "modifiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modifier {

    // Primary key, generated automatically by the database.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "modifier_id")
    private Long id;

    // Name of the modifier option.
    @Column(nullable = false, length = 100)
    private String name;

    // Additional cost of selecting this modifier.
    @Column(name = "added_price", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal priceModifier = BigDecimal.ZERO;

    // Returns zero if the modifier price is null.
    public BigDecimal getPriceModifier () {
        return priceModifier != null ? priceModifier : BigDecimal.ZERO;
    }

    // Indicates whether the modifier is available for selection.
    @Column(name = "available", nullable = false)
    @Builder.Default
    private Boolean available = true;

    // Modifier group to which this option belongs.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifier_group_id", nullable = false)
    @JsonIgnore // Prevents serializing the whole parent group inside every child modifier object
    private ModifierGroup modifierGroup;

    // Timestamps recording when the modifier was created and last updated.
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