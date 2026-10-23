
package org.example.cafeteria_ordering_backend.repository;

import org.example.cafeteria_ordering_backend.entity.Order;
import org.example.cafeteria_ordering_backend.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT DISTINCT o
            FROM Order o
            LEFT JOIN FETCH o.orderItems
            WHERE o.orderStatus = :status
            """)
    List<Order> findByStatusWithItems(
            @Param("status") OrderStatus status
    );

    @Query("""
            SELECT o
            FROM Order o
            WHERE o.orderStatus.statusName NOT IN :statusNames
            ORDER BY o.orderId ASC
            """)
    List<Order> findActiveOrdersWithItems (@Param("statusNames") List<String> statusNames);

    @Query("""
            SELECT DISTINCT o
            FROM Order o
            LEFT JOIN FETCH o.orderItems
            WHERE o.orderId = :orderId
            """)
    Optional<Order> findByIdWithItems (@Param("orderId") Long orderId);

    // Checks if an order with the given customer order number exists.
    boolean existsByCustomerOrderNumber (String customerOrderNum);

    // Finds an order using its customer order number.
    Optional<Order> findByCustomerOrderNumber (String customerOrderNum);

    // Retrieves the complete order history for a user.
    List<Order> findByUserUserIdOrderByCreatedAtDesc (Long userId);

    // Retrieves active kitchen board orders.
    List<Order> findByOrderStatusStatusNameNotInOrderByOrderIdAsc (List<String> statusNames);

    // Retrieves active tracker orders for a specific user.
    List<Order> findByUserUserIdAndOrderStatusStatusNameNotInOrderByCreatedAtAsc(
            Long userId,List<String> statusNames
    );

    // Finds orders that have exceeded the allowed waiting time.
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.orderStatus.statusName IN :statuses
            AND o.createdAt < :cutoffTime
            """)
    List<Order> findOverdueOrders (
            @Param("statuses") List<String> statuses,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );

    // Locks an order while its status and stock are being updated.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

    // Loads an order and its items.
    // Acquire the order lock separately using findByIdForUpdate().
    @Query("""
            SELECT DISTINCT o
            FROM Order o
            LEFT JOIN FETCH o.orderItems
            WHERE o.orderId = :orderId
            """)
    Optional<Order> findByIdWithItemsForUpdate(@Param("orderId") Long orderId);
}
