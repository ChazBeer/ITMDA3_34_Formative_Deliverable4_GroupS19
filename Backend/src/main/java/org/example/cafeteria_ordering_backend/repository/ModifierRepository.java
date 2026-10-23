package org.example.cafeteria_ordering_backend.repository;

import org.example.cafeteria_ordering_backend.entity.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModifierRepository extends JpaRepository<Modifier, Long> {
    // Single lookup kept in case we still find a reason
    List<Modifier> findByModifierGroupId (Long modifierGroupId);

    // Bulk lookup
    List<Modifier> findByModifierGroupIdIn (List<Long> groupIds);
}