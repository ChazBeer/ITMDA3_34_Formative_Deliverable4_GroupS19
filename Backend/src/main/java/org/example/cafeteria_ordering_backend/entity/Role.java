package org.example.cafeteria_ordering_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    // Primary key, generated automatically by the database.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    // Unique name identifying the user's role, such as CUSTOMER or ADMIN.
    @Column(name = "role_name", length = 50, nullable = false, unique = true)
    private String roleName;

    // Timestamps recording when the role was created and last updated.
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

    // Updates the modification timestamp before an existing role is updated.
    @PreUpdate
    protected void onUpdate () {
        updatedAt = LocalDateTime.now();
    }
}
