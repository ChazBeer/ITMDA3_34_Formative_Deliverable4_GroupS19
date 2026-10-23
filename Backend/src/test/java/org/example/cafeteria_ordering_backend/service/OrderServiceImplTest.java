package org.example.cafeteria_ordering_backend.service;

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
import org.example.cafeteria_ordering_backend.service.implementation.OrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Initializes Mockito
@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private OperatingHoursValidator operatingHoursValidator;

    @Mock
    private OrderSecurityEvaluator orderSecurityEvaluator;

    @Mock
    private OrderNumberGenerator orderNumberGenerator;

    @Mock
    private OrderItemCalculator orderItemCalculator;

    @Mock
    private OrderMapper orderMapper;

    // Creates the service instance and injects mocks
    @InjectMocks
    private OrderServiceImpl orderService;

    // Shared test entities used to prep scenario
    private User mockUser;
    private OrderStatus submittedStatus;
    private OrderStatus cancelledStatus;
    private OrderStatus completedStatus;
    private Order mockOrder;

    @BeforeEach
    void setUp () {

        // Prep customer for test order
        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setEmail("test@campus.edu");

        // Prep the order statuses for test
        submittedStatus = new OrderStatus();
        submittedStatus.setStatusName("Submitted");

        cancelledStatus = new OrderStatus();
        cancelledStatus.setStatusName("Cancelled");

        completedStatus = new OrderStatus();
        completedStatus.setStatusName("Completed");

        // Create order with initial submit status
        mockOrder = new Order();
        mockOrder.setOrderId(50L);
        mockOrder.setUser(mockUser);
        mockOrder.setOrderStatus(submittedStatus);
        mockOrder.setCustomerOrderNumber("ORD-20260330-ABCD");
        mockOrder.setTotalPrice(new BigDecimal("30.00"));
        mockOrder.setCreatedAt(LocalDateTime.now());
        mockOrder.setOrderItems(new ArrayList<>());

        // Config mapper to build responseDTO
        // Lenient stubbing allows tests not using mapper to use setup
        lenient().when(orderMapper.toResponseDTO(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);

            if (order == null) return null;

            OrderResponseDTO dto = new OrderResponseDTO();
            dto.setOrderId(order.getOrderId());
            dto.setCustomerOrderNumber(order.getCustomerOrderNumber());
            dto.setStatus(order.getOrderStatus() != null ? order.getOrderStatus().getStatusName() : null);
            dto.setTotalPrice(order.getTotalPrice());

            // Map each order item & its modifiers into response
            if (order.getOrderItems() != null) {
                List<OrderResponseDTO.OrderItemResponse> itemDTOs = order.getOrderItems().stream().map(item -> {
                    OrderResponseDTO.OrderItemResponse itemDTO = new OrderResponseDTO.OrderItemResponse();
                    itemDTO.setMenuItemName(item.getMenuItemNameAtPurchase());
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setPriceAtPurchase(item.getPriceAtPurchase());

                    // Include selected modifier names, or empty list
                    if (item.getOrderItemModifiers() != null) {
                        itemDTO.setSelectedModifiers(
                                item.getOrderItemModifiers().stream()
                                        .map(OrderItemModifier::getModifierNameAtPurchase)
                                        .toList()
                        );
                    } else {
                        itemDTO.setSelectedModifiers(List.of());
                    }
                    return itemDTO;
                }).toList();
                dto.setItems(itemDTOs);
            } else {
                // Return empty item list when order has no items
                dto.setItems(List.of());
            }

            return dto;
        });
    }

    /**
     * Clears Spring Security context after each test to stop authentication details from leaking into following tests.
     */
    @AfterEach
    void clearSecurityContext () {

        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Checkout Tests")
    class CheckoutTests {

        /**
         * Creates valid checkout request containing a menu item, its quantity, modifier and special instructions
         *
         */
        private OrderRequestDTO createValidRequestDTO () {

            OrderRequestDTO request = new OrderRequestDTO();
            request.setUserId(1L);

            OrderRequestDTO.OrderItemRequest itemRequest = new OrderRequestDTO.OrderItemRequest();
            itemRequest.setMenuItemId(10L);
            itemRequest.setQuantity(2);
            itemRequest.setSpecialInstructions("Extra hot");
            itemRequest.setModifierIds(List.of(100L));
            request.setItems(List.of(itemRequest));
            return request;
        }

        @Test
        @DisplayName("Should successfully process checkout and deduct stock")
        void checkout_Success () {

            // Configure operating hours, the authenticated user, generated order number, and initial order status
            doNothing().when(operatingHoursValidator).validateOpenForOrders();

            when(orderSecurityEvaluator.getCurrentUser()).thenReturn(mockUser);
            when(orderNumberGenerator.generate()).thenReturn("ORD-20260330-ABCD");

            OrderRequestDTO request = createValidRequestDTO();

            when(orderStatusRepository.findByStatusName("Submitted")).thenReturn(Optional.of(submittedStatus));

            // Prepare the menu item linked with order item.
            MenuItems mockMenuItem = new MenuItems();
            mockMenuItem.setMenuItemId(10L);

            // Prepare order item with its purchase-time details
            OrderItem mockOrderItem = new OrderItem();
            mockOrderItem.setMenuItem(mockMenuItem);
            mockOrderItem.setMenuItemNameAtPurchase("Espresso");
            mockOrderItem.setQuantity(2);
            mockOrderItem.setPriceAtPurchase(new BigDecimal("25.00"));
            mockOrderItem.setSpecialInstructions("Extra hot");

            // Attach the selected modifier and its purchase-time price
            OrderItemModifier mockOrderItemModifier = new OrderItemModifier();
            mockOrderItemModifier.setModifierNameAtPurchase("Extra Shot");
            mockOrderItemModifier.setPriceAtPurchase(new BigDecimal("5.00"));

            mockOrderItem.setOrderItemModifiers(List.of(mockOrderItemModifier));

            // Configure calc result and update order entity when serivce req order-item calc
            OrderItemCalculator.CalculationResult mockCalculationResult =
                    new OrderItemCalculator.CalculationResult(List.of(mockOrderItem), new BigDecimal("60.00"));

            when(orderItemCalculator.calculateAndBuildItems(eq(request.getItems()), any(Order.class)
            )).thenAnswer(invocation -> {
                Order order = invocation.getArgument(1);
                order.setTotalPrice(mockCalculationResult.totalPrice());
                order.setOrderItems(mockCalculationResult.orderItems());
                return mockCalculationResult;
            });

            // Simulate successful stock deduction of requested stock quantity
            when(menuItemRepository.deductStockAndAutoDisable(10L, 2)).thenReturn(1);

            // Simulate persistence and assign an ID to a newly created order
            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> {
                        Order saved = invocation.getArgument(0);

                        if (saved.getOrderId() == null) {
                            saved.setOrderId(50L);
                        }
                        return saved;
                    });

            // Execute checkout
            OrderResponseDTO response = orderService.checkout(request);

            // Verify the response
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(50L);
            assertThat(response.getCustomerOrderNumber()).isEqualTo("ORD-20260330-ABCD");
            assertThat(response.getStatus()).isEqualTo("Submitted");
            assertThat(response.getTotalPrice()).isEqualByComparingTo("60.00");
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
            assertThat(response.getItems().get(0).getSelectedModifiers()).contains("Extra Shot");

            // Verify checkout and stock deduction.
            verify(operatingHoursValidator).validateOpenForOrders();
            verify(orderItemCalculator).calculateAndBuildItems(eq(request.getItems()), any(Order.class));
            verify(menuItemRepository).deductStockAndAutoDisable(10L, 2);
            verify(orderRepository).save(any(Order.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/orders/active"), any(OrderResponseDTO.class));
        }


        @Test
        @DisplayName("Should throw ResourceNotFoundException when user does not exist")
        void checkout_UserNotFound_ThrowsException () {

            // allow operating hours validation while simulating missing user
            doNothing().when(operatingHoursValidator).validateOpenForOrders();

            when(orderSecurityEvaluator.getCurrentUser())
                    .thenThrow(new ResourceNotFoundException("Current user not found."));

            OrderRequestDTO request = createValidRequestDTO();

            // Checkout must fail when no current user is found
            assertThatThrownBy(() -> orderService.checkout(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Current user not found");

            // Verify that processing stops before item calc and stock deduction or order is saved
            verify(orderItemCalculator, never())
                    .calculateAndBuildItems(any(), any());

            verify(menuItemRepository, never())
                    .deductStockAndAutoDisable(anyLong(), anyInt());

            verify(orderRepository, never()).save(any(Order.class));
        }


        @Nested
        @DisplayName("Update Order Status Tests")
        class UpdateOrderStatusTests {

            @Test
            @DisplayName("Should successfully update status, create history log, and broadcast via WebSockets")
            void updateOrderStatus_Success () {

                // Prep order and requested new status
                Long orderId = 50L;

                OrderStatus readyStatus = new OrderStatus();
                readyStatus.setStatusName("Ready");

                when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(mockOrder));

                when(orderStatusRepository.findByStatusName("Ready")).thenReturn(Optional.of(readyStatus));

                when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

                // Update order status
                OrderResponseDTO response = orderService.updateOrderStatus(orderId, "Ready");

                // Confirm response shows new status
                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("Ready");

                // Check order and status history is saved
                verify(orderRepository, times(1)).save(mockOrder);

                verify(orderStatusHistoryRepository, times(1))
                        .save(any(OrderStatusHistory.class));

                verify(messagingTemplate, times(1))
                        .convertAndSend(eq("/topic/orders/1"), any(OrderResponseDTO.class));

                verify(messagingTemplate, times(1))
                        .convertAndSend(eq("/topic/orders/active"), any(OrderResponseDTO.class));
            }

            @Test
            @DisplayName("Should throw ResourceNotFoundException when requested status name does not exist in DB")
            void updateOrderStatus_InvalidStatus_ThrowsException () {

                // Gives existing user with non-existant status
                Long orderId = 50L;

                when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(mockOrder));

                when(orderStatusRepository.findByStatusName("UnknownStatus"))
                        .thenReturn(Optional.empty());

                // Verify that the service rejects incorrect status
                assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "UnknownStatus"))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("Invalid status name: 'UnknownStatus'");

                // Check no status history entry is created
                verify(orderStatusHistoryRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("Cancel Order Tests")
        class CancelOrderTests {

            @Test
            @DisplayName("Should allow cancellation when order status is Submitted and broadcast updates")
            void cancelOrder_SubmittedStatus_Success () {

                // Prep an order currently in submitted state
                Long orderId = 50L;

                mockOrder.setOrderStatus(submittedStatus);

                when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(mockOrder));

                // Simulate successful validation and retireval of owner
                doNothing().when(orderSecurityEvaluator).validateOrderOwnership(1L);
                when(orderSecurityEvaluator.getCurrentUser()).thenReturn(mockUser);

                when(orderStatusRepository.findByStatusName("Cancelled"))
                        .thenReturn(Optional.of(cancelledStatus));

                when(orderRepository.save(any(Order.class)))
                        .thenAnswer(inv -> inv.getArgument(0));

                // Cancel submitted order
                orderService.cancelOrder(orderId);

                // Confirm that order status changes to cancelled
                assertThat(mockOrder.getOrderStatus().getStatusName()).isEqualTo("Cancelled");

                // Ensure persistence of order and status history as well as broadcasts to customer
                // and active order subscribers
                verify(orderRepository, times(1)).save(mockOrder);

                verify(orderStatusHistoryRepository, times(1))
                        .save(any(OrderStatusHistory.class));

                verify(messagingTemplate, times(1))
                        .convertAndSend(eq("/topic/orders/1"), any(OrderResponseDTO.class));

                verify(messagingTemplate, times(1))
                        .convertAndSend(eq("/topic/orders/active"), any(OrderResponseDTO.class));
            }

            @Test
            @DisplayName("Should allow cancellation when order status is Accepted")
            void cancelOrder_AcceptedStatus_Success () {

                // Prepare order that is accepted status
                Long orderId = 50L;

                OrderStatus acceptedStatus = new OrderStatus();
                acceptedStatus.setStatusName("Accepted");

                mockOrder.setOrderStatus(acceptedStatus);

                when(orderRepository.findByIdWithItems(orderId))
                        .thenReturn(Optional.of(mockOrder));

                // Simulate successful owner validation and retireval
                doNothing().when(orderSecurityEvaluator).validateOrderOwnership(1L);
                when(orderSecurityEvaluator.getCurrentUser()).thenReturn(mockUser);

                when(orderStatusRepository.findByStatusName("Cancelled"))
                        .thenReturn(Optional.of(cancelledStatus));

                when(orderRepository.save(any(Order.class)))
                        .thenAnswer(inv -> inv.getArgument(0));

                // Cancel accepted order
                orderService.cancelOrder(orderId);

                // Confirm the order is marked cancelled
                assertThat(mockOrder.getOrderStatus().getStatusName()).isEqualTo("Cancelled");

                // Ensure persistence, status history recording and websocket broadcast
                verify(orderRepository, times(1)).save(mockOrder);

                verify(orderStatusHistoryRepository, times(1))
                        .save(any(OrderStatusHistory.class));

                verify(messagingTemplate, times(1))
                        .convertAndSend(eq("/topic/orders/1"), any(OrderResponseDTO.class));

                verify(messagingTemplate, times(1))
                        .convertAndSend(eq("/topic/orders/active"), any(OrderResponseDTO.class));
            }

            @Test
            @DisplayName("Should throw ResourceUnavailableException when order is beyond initial phases (e.g. Completed)")
            void cancelOrder_CompletedStatus_ThrowsException () {

                // Prep order in Completted state
                Long orderId = 50L;

                mockOrder.setOrderStatus(completedStatus);

                when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(mockOrder));

                // Simulate ownership validation and user retrieval
                doNothing().when(orderSecurityEvaluator).validateOrderOwnership(1L);
                when(orderSecurityEvaluator.getCurrentUser()).thenReturn(mockUser);

                // Completed orders shouldnt be cancellable
                assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                        .isInstanceOf(ResourceUnavailableException.class)
                        .hasMessageContaining("Cannot cancel order: This order is already 'Completed'");

                // Ensure rejected cancellation does not save changes, save to history, or broadcast update
                verify(orderRepository, never()).save(any());
                verify(orderStatusHistoryRepository, never()).save(any());
                verify(messagingTemplate, never())
                        .convertAndSend(anyString(), any(Object.class));
            }
        }

        @Nested
        @DisplayName("Query & Fetch Order Tests")
        class QueryOrderTests {

            @Test
            @DisplayName("Should fetch active orders for user excluding final statuses")
            void getActiveOrdersByUserId_Success () {

                // Authorize access to users order and return matching order
                Long userId = 1L;

                doNothing().when(orderSecurityEvaluator).validateUserAccess(userId);

                when(orderRepository.findByUserUserIdAndOrderStatusStatusNameNotInOrderByCreatedAtAsc(
                        eq(userId),
                        eq(List.of("Cancelled", "Collected"))
                )).thenReturn(List.of(mockOrder));

                // Confirm returned order
                List<OrderResponseDTO> results = orderService.getActiveOrdersByUserId(userId);

                assertThat(results).hasSize(1);
                assertThat(results.get(0).getOrderId()).isEqualTo(50L);
            }

            @Test
            @DisplayName("Should prevent a customer from querying another user's order history")
            void getOrdersByUserId_UserNotAllowed_ThrowsException () {

                // Simulate req for user whose information the current user has no auth for
                Long invalidUserId = 99L;

                doThrow(new ResourceUnavailableException("Access denied: You are not authorized to access this resource."))
                        .when(orderSecurityEvaluator).validateUserAccess(invalidUserId);

                // Verify that access controln exception occurs
                assertThatThrownBy(() -> orderService.getOrdersByUserId(invalidUserId))
                        .isInstanceOf(ResourceUnavailableException.class)
                        .hasMessageContaining("Access denied");
            }

            @Test
            @DisplayName("Should successfully fetch all orders when user is staff or admin")
            void getAllOrders_StaffOrAdmin_Success () {

                // Authorise current user as staff or admin
                when(orderSecurityEvaluator.isStaffOrAdmin()).thenReturn(true);
                when(orderRepository.findAll()).thenReturn(List.of(mockOrder));

                // Get all orders
                List<OrderResponseDTO> results = orderService.getAllOrders();

                // Ensure returned results have expected order
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getOrderId()).isEqualTo(50L);

                // Ensure service checks users privileges
                verify(orderSecurityEvaluator, times(1)).isStaffOrAdmin();
            }

            @Test
            @DisplayName("Should throw ResourceUnavailableException when non-staff attempts to fetch all orders")
            void getAllOrders_Unauthorized_ThrowsException () {

                // Deny staff/admin
                when(orderSecurityEvaluator.isStaffOrAdmin()).thenReturn(false);

                // Confirm unauthorized users cant retrieve all orders
                assertThatThrownBy(() -> orderService.getAllOrders())
                        .isInstanceOf(ResourceUnavailableException.class)
                        .hasMessageContaining("Only staff or admin");

                // Ensure repo is not queried
                verify(orderRepository, never()).findAll();
            }

            @Test
            @DisplayName("Should successfully fetch active orders when user is staff or admin")
            void getActiveOrders_StaffOrAdmin_Success () {

                // Authorize user and simulate active order query result
                when(orderSecurityEvaluator.isStaffOrAdmin()).thenReturn(true);
                when(orderRepository.findActiveOrdersWithItems(anyList()))
                        .thenReturn(List.of(mockOrder));

                // Get active orders avail to staff or admin
                List<OrderResponseDTO> results = orderService.getActiveOrders();

                // Check that active order results contain expected result
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getOrderId()).isEqualTo(50L);
                verify(orderSecurityEvaluator, times(1)).isStaffOrAdmin();
            }

            @Test
            @DisplayName("Should throw ResourceUnavailableException when non-staff attempts to fetch active orders")
            void getActiveOrders_Unauthorized_ThrowsException () {

                // Simulate user without staff or admin priviledges
                when(orderSecurityEvaluator.isStaffOrAdmin()).thenReturn(false);

                // Verify access to active order is denied
                assertThatThrownBy(() -> orderService.getActiveOrders())
                        .isInstanceOf(ResourceUnavailableException.class)
                        .hasMessageContaining("Only staff or admin");

                // Ensure order repo is not queried
                verify(orderRepository, never()).findByOrderStatusStatusNameNotInOrderByOrderIdAsc(any());
            }
        }

        @Nested
        @DisplayName("Expire Overdue Orders Tests")
        class ExpireOverdueOrdersTests {

            @Test
            @DisplayName("Should expire overdue orders and broadcast status updates")
            void expireOverdueOrders_Success () {

                // Simulate overdue submitted order and available cancelled status for automatic expiration
                when(orderRepository.findOverdueOrders(
                        eq(List.of("Submitted")),
                        any(LocalDateTime.class)
                )).thenReturn(List.of(mockOrder));

                when(orderStatusRepository.findByStatusName("Cancelled"))
                        .thenReturn(Optional.of(cancelledStatus));

                when(orderRepository.save(any(Order.class)))
                        .thenAnswer(inv -> inv.getArgument(0));

                // Enact overdue order expiration
                orderService.expireOverdueOrders();

                // Verify that the overdue order is cancelled
                assertThat(mockOrder.getOrderStatus().getStatusName()).isEqualTo("Cancelled");

                // ENsure saving of updated order, status history, as well as notification to relevant uusers
                verify(orderRepository, times(1)).save(mockOrder);

                verify(orderStatusHistoryRepository, times(1))
                        .save(any(OrderStatusHistory.class));

                verify(messagingTemplate).convertAndSend(eq("/topic/orders/1"), any(WebSocketMessageResponseDTO.class));

                verify(messagingTemplate, times(1))
                        .convertAndSend(eq("/topic/orders/active"), any(OrderResponseDTO.class));
            }

            @Test
            @DisplayName("Should do nothing when no overdue orders are found")
            void expireOverdueOrders_NoOverdueOrders_DoesNothing () {

                // Simulate query that returns no overdue orders
                when(orderRepository.findOverdueOrders(
                        eq(List.of("Submitted")),
                        any(LocalDateTime.class)
                )).thenReturn(List.of());

                // RUn expiration process with no matching orders
                orderService.expireOverdueOrders();

                // Verify no orders is saved and no websocket messages are sent
                verify(orderRepository, never()).save(any());
                verifyNoInteractions(messagingTemplate);
            }
        }
    }
}