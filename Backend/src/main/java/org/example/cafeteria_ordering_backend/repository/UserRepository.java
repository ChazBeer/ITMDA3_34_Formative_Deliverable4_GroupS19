package org.example.cafeteria_ordering_backend.repository;

import org.example.cafeteria_ordering_backend.entity.Role;
import org.example.cafeteria_ordering_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Custom query method to look up a user by email (crucial for future logins)
    Optional<User> findByEmail (String email);

    // Custom query method to check if an email is already registered
    boolean existsByEmail (String email);

    // Custom query method to check user exists by their customer code
    boolean existsByCustomerCode (String customerCode);

    // Look up users by role name
    List<User> findByRole_RoleName (String roleName);

    // find all active users
    List<User> findByActive (Boolean active);
}