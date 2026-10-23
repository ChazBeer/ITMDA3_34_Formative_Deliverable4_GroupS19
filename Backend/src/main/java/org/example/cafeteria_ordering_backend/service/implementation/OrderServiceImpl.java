package org.example.cafeteria_ordering_backend.service.implementation;

import org.example.cafeteria_ordering_backend.component.OperatingHoursValidator;
import org.example.cafeteria_ordering_backend.component.OrderItemCalculator;
import org.example.cafeteria_ordering_backend.component.OrderNumberGenerator;
import org.example.cafeteria_ordering_backend.component.OrderSecurityEvaluator;
import org.example.cafeteria_ordering_backend.dto.requests.OrderRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.OrderResponseDTO;
import org.example.cafeteria_ordering_backend.dto.response.WebSocketMessageResponseDTO;
import org.example.cafeteria_ordering_backend.entity.*;
import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.exception.ResourceUnavailableException;
import org.example.cafeteria_ordering_backend.mapper.OrderMapper;
import org.example.cafeteria_ordering_backend.repository.*;
import org.example.cafeteria_ordering_backend.service.interfaces.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final MenuItemRepository menuItemRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OperatingHoursValidator operatingHoursValidator;
    private final OrderSecurityEvaluator orderSecurityEvaluator;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderItemCalculator orderItemCalculator;

    // Sets the time limit for submitted orders before they expire.
    @Value("${app.orders.expiration-timeout-minutes:15}")
    private int expirationTimeoutMinutes;

    // Injects the repositories and components required to manage orders.
    public OrderServiceImpl (
            OrderRepository orderRepository,
            OrderStatusRepository orderStatusRepository,
            UserRepository userRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            SimpMessagingTemplate messagingTemplate,
            OperatingHoursValidator operatingHoursValidator,
            OrderSecurityEvaluator orderSecurityEvaluator,
            OrderNumberGenerator orderNumberGenerator,
            OrderItemCalculator orderItemCalculator,
            OrderMapper orderMapper,
            MenuItemRepository menuItemRepository) {

        this.orderRepository = orderRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.userRepository = userRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.messagingTemplate = messagingTemplate;
        this.operatingHoursValidator = operatingHoursValidator;
        this.orderSecurityEvaluator = orderSecurityEvaluator;
        this.orderNumberGenerator = orderNumberGenerator;
        this.orderItemCalculator = orderItemCalculator;
        this.orderMapper = orderMapper;
        this.menuItemRepository = menuItemRepository;
    }

    // Retrieves a user's orders that have not reached a final status.
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getActiveOrdersByUserId (Long userId) {

        // Ensures the current user is authorised to access these orders.
        orderSecurityEvaluator.validateUserAccess(userId);

        List<String> finalStatuses = List.of("Cancelled", "Collected");

        return orderRepository.findByUserUserIdAndOrderStatusStatusNameNotInOrderByCreatedAtAsc(
                        userId,
                        finalStatuses
                ).stream().map(orderMapper::toResponseDTO).toList();
    }


    @Override
    @Transactional
    public OrderResponseDTO checkout (OrderRequestDTO request) {

        // Prevents orders from being placed outside operating hours.
        operatingHoursValidator.validateOpenForOrders();

        // Retrieves the currently authenticated user.
        User user = orderSecurityEvaluator.getCurrentUser();

        logger.info("Checkout attempt for user ID: {}", user.getUserId());

        // Retrieves the initial status assigned to newly submitted orders.
        OrderStatus submittedStatus = orderStatusRepository.findByStatusName("Submitted")
                .orElseThrow(() -> new RuntimeException(
                        "Initial 'Submitted' status configuration missing in database."
                ));

        // Creates the order and assigns its customer and initial status.
        Order order = new Order();
        order.setUser(user);
        order.setOrderStatus(submittedStatus);
        order.setCustomerOrderNumber(orderNumberGenerator.generate());

        // Validates the requested items and calculates the order total.
        OrderItemCalculator.CalculationResult result =
                orderItemCalculator.calculateAndBuildItems(request.getItems(), order);

        order.setOrderItems(result.orderItems());
        order.setTotalPrice(result.totalPrice());

        // Combines quantities when the same menu item appears on multiple order lines.
        Map<Long, Integer> quantitiesByMenuItem = order.getOrderItems()
                .stream()
                .collect(Collectors.groupingBy(
                        item -> item.getMenuItem().getMenuItemId(),
                        Collectors.summingInt(OrderItem::getQuantity)
                ));

        // Deducts stock only when sufficient stock is available.
        for (Map.Entry<Long, Integer> entry : quantitiesByMenuItem.entrySet()) {
            Long menuItemId = entry.getKey();
            Integer quantity = entry.getValue();

            int updatedRows = menuItemRepository.deductStockAndAutoDisable(
                    menuItemId,
                    quantity
            );

            if (updatedRows == 0) {throw new ResourceUnavailableException(
                        "Insufficient stock or menu item unavailable for menu item ID: "
                                + menuItemId
                );
            }
        }

        // Saves the order after stock has been successfully deducted.
        Order savedOrder = orderRepository.save(order);

        logger.info(
                "Order created successfully: {} for user ID: {}, total: {}",
                savedOrder.getCustomerOrderNumber(),
                user.getUserId(),
                savedOrder.getTotalPrice()
        );

        // Converts the saved order to a response DTO.
        OrderResponseDTO response = orderMapper.toResponseDTO(savedOrder);

        // Notifies the kitchen of the new order.
        logger.info(
                "Sending new order {} to kitchen topic /topic/orders/active",
                savedOrder.getCustomerOrderNumber()
        );

        messagingTemplate.convertAndSend("/topic/orders/active", response);

        return response;
    }

    // Get all orders
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders () {

        // Prevents customers from accessing the complete order list.
        if (!orderSecurityEvaluator.isStaffOrAdmin()) {
            throw new ResourceUnavailableException("Only staff or admin can access all orders.");
        }
        return orderRepository.findAll().stream().map(orderMapper::toResponseDTO).toList();
    }

    // Get Order by ID
    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById (Long id) {

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        // Ensures users can only access orders they are authorised to view.
        orderSecurityEvaluator.validateOrderOwnership(order.getUser().getUserId());

        return orderMapper.toResponseDTO(order);
    }

    // Get orders by user ID
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByUserId (Long userId) {

        // Checks that the current user may access the requested user's orders.
        orderSecurityEvaluator.validateUserAccess(userId);

        // Confirms that the requested user exists.
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        return orderRepository
                .findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(orderMapper::toResponseDTO)
                .toList();
    }

    // Changes an order's status and notifies the customer and kitchen.
    @Override
    @Transactional
    public OrderResponseDTO updateOrderStatus (Long orderId, String statusName) {

        // Use the existing cancellation process when an order is cancelled.
        if ("Cancelled".equalsIgnoreCase(statusName)) {
            cancelOrder(orderId);

            // Retrieve the canceled order for the response.
            Order cancelledOrder = orderRepository.findByIdWithItems(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Order not found with ID: " + orderId
                    ));

            return orderMapper.toResponseDTO(cancelledOrder);
        }

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        logger.info(
                "Updating order {} status from {} to {}",
                order.getCustomerOrderNumber(),
                order.getOrderStatus().getStatusName(),
                statusName
        );

        // Retrieves the requested status or rejects an invalid status name.
        OrderStatus newStatus = orderStatusRepository.findByStatusName(statusName)
                        .orElseThrow(() -> new ResourceNotFoundException("Invalid status name: '" + statusName
                                + "'. Make sure this status exists in the database."));

        order.setOrderStatus(newStatus);

        // Saves the updated order status.
        Order updatedOrder = orderRepository.save(order);

        logger.info("Order {} status successfully changed to {}",
                updatedOrder.getCustomerOrderNumber(),
                newStatus.getStatusName()
        );

        // Get currently authenticated user
        User currentUser = orderSecurityEvaluator.getCurrentUser();

        // Save status history
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(updatedOrder);
        history.setStatus(newStatus);
        history.setChangedByUser(currentUser);
        orderStatusHistoryRepository.save(history);

        // Re-fetch to guarantee fully loaded relationships for DTO mapping
        Order fullyLoadedOrder = orderRepository.findByIdWithItems(updatedOrder.getOrderId())
                .orElse(updatedOrder);

        // Convert to DTO
        OrderResponseDTO response = orderMapper.toResponseDTO(fullyLoadedOrder);

        // Customer Websocket
        logger.info(
                "Sending WebSocket update for order {} to topic /topic/orders/{}",
                fullyLoadedOrder.getCustomerOrderNumber(),
                fullyLoadedOrder.getUser().getUserId()
        );

        messagingTemplate.convertAndSend("/topic/orders/" + fullyLoadedOrder.getUser().getUserId(), response);

        // Kitchen Websocket
        logger.info(
                "Sending order {} status update to kitchen topic /topic/orders/active",
                fullyLoadedOrder.getCustomerOrderNumber()
        );

        messagingTemplate.convertAndSend("/topic/orders/active", response);

        return response;
    }

    // Get active orders (for staff dashboard)
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getActiveOrders () {

        // Restricts the kitchen order list to staff and administrators.
        if (!orderSecurityEvaluator.isStaffOrAdmin()) {
            throw new ResourceUnavailableException("Only staff or admin can access active kitchen orders.");
        }

        List<String> excludedStatuses = List.of("Completed", "Cancelled");

        return orderRepository
                .findActiveOrdersWithItems(excludedStatuses)
                .stream()
                .map(orderMapper::toResponseDTO)
                .toList();
    }

    // Cancel orders
    @Override
    @Transactional
    public void cancelOrder (Long orderId) {

        // Fetch Order
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // Security & Ownership Check (Replaces ~30 lines of manual auth logic)
        orderSecurityEvaluator.validateOrderOwnership(order.getUser().getUserId());

        User currentUser = orderSecurityEvaluator.getCurrentUser();

        // Status Validation
        String currentStatus = order.getOrderStatus().getStatusName();

        logger.info("Cancellation requested for order {} by {} with current status {}",
                order.getCustomerOrderNumber(), currentUser.getEmail(), currentStatus);

        // Only submitted or accepted orders can be cancelled by the customer.
        if (!currentStatus.equalsIgnoreCase("Submitted") && !currentStatus.equalsIgnoreCase("Accepted")) {
            throw new ResourceUnavailableException(
                    "Cannot cancel order: This order is already '" + currentStatus + "' and cannot be cancelled."
            );
        }

        OrderStatus cancelledStatus = orderStatusRepository.findByStatusName("Cancelled")
                .orElseThrow(() -> new RuntimeException("Configuration error: 'Cancelled' status entity missing in database."));

        // Restores stock before changing the order status.
        restoreStockForOrder(order);

        order.setOrderStatus(cancelledStatus);
        Order savedOrder = orderRepository.save(order);

        logger.info("Order {} cancelled successfully by {}", savedOrder.getCustomerOrderNumber(), currentUser.getEmail());

        // Save Status History
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(savedOrder);
        history.setStatus(cancelledStatus);
        history.setChangedByUser(currentUser);
        orderStatusHistoryRepository.save(history);

        // WebSocket Notifications
        OrderResponseDTO response = orderMapper.toResponseDTO(savedOrder);

        messagingTemplate.convertAndSend("/topic/orders/" + savedOrder.getUser().getUserId(), response);

        // Removes the canceled order from active kitchen work.
        logger.info("Sending cancelled order {} to kitchen topic /topic/orders/active",
                savedOrder.getCustomerOrderNumber());
        messagingTemplate.convertAndSend("/topic/orders/active", response);
    }

    // Automatically cancels submitted orders that have exceeded the time limit.
    @Scheduled(fixedRate = 300000)
    @Transactional
    @Override
    public void expireOverdueOrders () {

        logger.info("--- Running scheduled task: Checking for overdue orders ---");

        // Calculates the cutoff time for orders that have expired.
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(expirationTimeoutMinutes);

        // Retrieves submitted orders older than the configured time limit.
        List<Order> overdueOrders = orderRepository.findOverdueOrders(List.of("Submitted"), cutoffTime);

        // Skips processing when no overdue orders are found.
        if (overdueOrders.isEmpty()) {return;}

        logger.info("Found {} overdue order(s) to expire.", overdueOrders.size());

        // Retrieves the status used to mark expired orders as cancelled.
        OrderStatus expiredStatus = orderStatusRepository.findByStatusName("Cancelled")
                .orElseThrow(() -> new ResourceNotFoundException("OrderStatus Cancelled not found"));

        // Cancels each overdue order and records its status change.
        for (Order order : overdueOrders) {
            // Restores stock before changing the order status.
            restoreStockForOrder(order);

            order.setOrderStatus(expiredStatus);
            Order savedOrder = orderRepository.save(order);

            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(savedOrder);
            history.setStatus(expiredStatus);
            orderStatusHistoryRepository.save(history);

            // Reloads the order and its items before creating the response.
            Order fullyLoadedOrder = orderRepository.findByIdWithItems(savedOrder.getOrderId())
                    .orElse(savedOrder);

            OrderResponseDTO responseDTO = orderMapper.toResponseDTO(fullyLoadedOrder);

            // Wraps response in WebSocketMessageResponseDTO
            WebSocketMessageResponseDTO<OrderResponseDTO> customerNotification = new WebSocketMessageResponseDTO<>(
                    "ORDER_TIMEOUT",
                    "Your order (" + fullyLoadedOrder.getCustomerOrderNumber() +
                            ") was automatically cancelled because the cafeteria did not accept within 15 minutes.",
                    responseDTO
            );

            // Sends rich timeout notification to specified user
            messagingTemplate.convertAndSend(
                    "/topic/orders/" + fullyLoadedOrder.getUser().getUserId(),
                    customerNotification
            );

            // Send standard response to the kitchen dashboard so it removes the expired order
            messagingTemplate.convertAndSend("/topic/orders/active", responseDTO);

            logger.info("Successfully expired and cancelled order: {}", savedOrder.getCustomerOrderNumber());
        }
    }

    // Restores stock for all menu items in an order.
    private void restoreStockForOrder (Order order) {

        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }

        // Combines quantities when the same menu item appears on multiple lines.
        Map<Long, Integer> quantitiesByMenuItem = order.getOrderItems()
                .stream()
                .collect(Collectors.groupingBy(
                        item -> item.getMenuItem().getMenuItemId(),
                        Collectors.summingInt(OrderItem::getQuantity)
                ));

        // Restores the quantity for each menu item.
        for (Map.Entry<Long, Integer> entry : quantitiesByMenuItem.entrySet()) {

            Long menuItemId = entry.getKey();
            Integer quantity = entry.getValue();

            int updatedRows = menuItemRepository.restoreStock(
                    menuItemId,
                    quantity
            );

            if (updatedRows == 0) {
                throw new ResourceNotFoundException(
                        "Could not restore stock for menu item ID: " + menuItemId
                );
            }
        }
    }
}