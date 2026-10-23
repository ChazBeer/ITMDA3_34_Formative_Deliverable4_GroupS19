package org.example.cafeteria_ordering_backend.repository;

import org.example.cafeteria_ordering_backend.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OrderStatusRepository extends JpaRepository<OrderStatus, Long> {

    // Finds an order status by its name and returns an Optional if it exists.
    Optional<OrderStatus> findByStatusName (String statusName);
}