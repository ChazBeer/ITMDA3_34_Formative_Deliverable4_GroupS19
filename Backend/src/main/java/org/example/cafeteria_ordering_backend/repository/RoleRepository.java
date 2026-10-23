package org.example.cafeteria_ordering_backend.repository;

import org.example.cafeteria_ordering_backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    // Looks up role by name (requires ROLE_ prepended i think)
    Optional<Role> findByRoleName (String roleName);
}