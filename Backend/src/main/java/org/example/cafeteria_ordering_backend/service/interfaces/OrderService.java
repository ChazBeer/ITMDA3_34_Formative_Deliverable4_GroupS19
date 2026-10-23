package org.example.cafeteria_ordering_backend.service.interfaces;

import org.example.cafeteria_ordering_backend.dto.requests.OrderRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.OrderResponseDTO;
import java.util.List;

public interface OrderService {

    // Processes checkout and creates a new order.
    OrderResponseDTO checkout (OrderRequestDTO request);

    // Retrieves all orders.
    List<OrderResponseDTO> getAllOrders ();

    // Retrieves an order by its ID.
    OrderResponseDTO getOrderById (Long id);

    // Retrieves all orders belonging to a specific user.
    List<OrderResponseDTO> getOrdersByUserId (Long userId);

    // Updates the status of an order.
    OrderResponseDTO updateOrderStatus (Long orderId, String statusName);

    // Expires orders that have exceeded their allowed collection time.
    void expireOverdueOrders ();

    // Retrieves all currently active orders.
    List<OrderResponseDTO> getActiveOrders ();

    // Cancels an order.
    void cancelOrder (Long orderId);

    // Retrieves active orders belonging to a specific user.
    List<OrderResponseDTO> getActiveOrdersByUserId (Long userId);
}