package org.example.cafeteria_ordering_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.cafeteria_ordering_backend.dto.requests.OrderRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.OrderResponseDTO;
import org.example.cafeteria_ordering_backend.service.interfaces.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.example.cafeteria_ordering_backend.dto.requests.OrderStatusUpdateRequestDTO;

import java.util.List;

// Handles order creation, retrieval, status updates and cancellation.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    // Allow authorised users to place an order.
    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STUDENT', 'ADMIN', 'STAFF')")
    public ResponseEntity<OrderResponseDTO> checkout (@Valid @RequestBody OrderRequestDTO request) {

        OrderResponseDTO response = orderService.checkout(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Retrieve all orders. Only administrators and staff can access this endpoint.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders () {

        List<OrderResponseDTO> response = orderService.getAllOrders();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Retrieve an order by its numeric ID.
    @GetMapping("/{id:^[0-9]+$}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'STUDENT')")
    public ResponseEntity<OrderResponseDTO> getOrderById (@PathVariable Long id) {

        OrderResponseDTO response = orderService.getOrderById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Retrieve orders associated with a specific user ID.
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'STUDENT')")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByUserId (@PathVariable Long userId) {

        List<OrderResponseDTO> response = orderService.getOrdersByUserId(userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Update an order's status. Only administrators and staff can access this endpoint.
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus (
            @PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequestDTO request) {

        OrderResponseDTO response = orderService.updateOrderStatus(id, request.getStatusName());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Cancel an order belonging to an authorised user.
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'STUDENT')")
    public ResponseEntity<Void> cancelOrder (@PathVariable Long id) {

        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }


    // Retrieve active orders for the kitchen or staff display.
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<OrderResponseDTO>> getActiveOrders () {

        List<OrderResponseDTO> response = orderService.getActiveOrders();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Retrieve the active orders associated with a specific user.
    @GetMapping("/user/{userId}/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'STUDENT')")
    public ResponseEntity<List<OrderResponseDTO>> getActiveOrdersByUserId (@PathVariable Long userId) {

        List<OrderResponseDTO> response = orderService.getActiveOrdersByUserId(userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}