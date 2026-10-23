package org.example.cafeteria_ordering_backend.config;

import org.example.cafeteria_ordering_backend.dto.requests.OrderRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.OrderResponseDTO;
import org.example.cafeteria_ordering_backend.entity.*;
import org.example.cafeteria_ordering_backend.repository.CategoryRepository;
import org.example.cafeteria_ordering_backend.repository.MenuItemRepository;
import org.example.cafeteria_ordering_backend.repository.OrderStatusRepository;
import org.example.cafeteria_ordering_backend.repository.RoleRepository;
import org.example.cafeteria_ordering_backend.repository.UserRepository;
import org.example.cafeteria_ordering_backend.service.interfaces.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// Starts the application on a random port and uses the test configuration.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jwt.secret=test-only-secret-key-for-cafeteria-integration-tests-2026",
                "jwt.expiration-ms=900000"
        }
)
@ActiveProfiles("test")
class CheckoutWebSocketIntegrationTest {

    // Stores the port assigned to the embedded server.
    @LocalServerPort
    private int port;

    // Provides access to the services and repositories needed by the test.
    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrderStatusRepository orderStatusRepository;

    // Clears authentication after each test to prevent security context leakage.
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkoutShouldSendNewOrderToKitchen() throws Exception {

        // Find the test customer or create one if it does not exist.
        User testUser = userRepository.findAll().stream()
                .filter(u -> "testcustomer@edu.com".equals(u.getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    Role customerRole = roleRepository.findByRoleName("CUSTOMER").orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setRoleName("CUSTOMER");
                        return roleRepository.save(newRole);
                    });

                    // Create and save the test customer.
                    User newUser = new User();
                    newUser.setCustomerCode("CUST-001");
                    newUser.setEmail("testcustomer@edu.com");
                    newUser.setPasswordHash("Password123!");
                    newUser.setFirstName("Test");
                    newUser.setLastName("Customer");
                    newUser.setRole(customerRole);
                    return userRepository.save(newUser);
                });

        // Confirm that the customer was saved correctly.
        assertNotNull(testUser.getUserId(), "Test user must have an ID generated");
        assertNotNull(testUser.getEmail(), "Test user must have an email");

        // Ensure all required order statuses exist in the test database.
        List.of("Submitted", "Accepted", "Preparing", "Ready", "Collected", "Cancelled").forEach(name -> {
            orderStatusRepository.findByStatusName(name).orElseGet(() -> {
                OrderStatus status = new OrderStatus();
                status.setStatusName(name);

               // Assign each status its expected siaply order.
                status.setSortOrder(
                        name.equals("Submitted") ? 1 :
                                name.equals("Accepted") ? 2 :
                                        name.equals("Preparing") ? 3 :
                                                name.equals("Ready") ? 4 :
                                                        name.equals("Collected") ? 5 : 6
                );

                // Mark Collected and Cancelled as final statuses.
                status.setIsFinal(name.equals("Collected") || name.equals("Cancelled"));
                return orderStatusRepository.save(status);
            });
        });

        // Retrieve the Submitted status and confirm it has a database ID.
        OrderStatus submittedStatus = orderStatusRepository.findByStatusName("Submitted").orElseThrow();
        assertNotNull(submittedStatus.getStatusId(), "Order status 'Submitted' must exist");

        // Find a category or create one for the test menu item.
        Category category = categoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            Category newCat = new Category();
            newCat.setCategoryName("Test Category");
            return categoryRepository.save(newCat);
        });

        // Find the test burger or create it if it does not exist.
        MenuItems menuItem = menuItemRepository.findAll().stream()
                .filter(m -> "Test Burger".equals(m.getName()))
                .findFirst()
                .orElseGet(() -> {
                    MenuItems newItem = new MenuItems();
                    newItem.setName("Test Burger");
                    newItem.setPrice(new BigDecimal("45.00"));
                    newItem.setCategory(category);
                    newItem.setStockQuantity(50);
                    newItem.setAvailable(true);
                    return menuItemRepository.save(newItem);
                });

        // Reset the menu item's details to known test values.
        menuItem.setName("Test Burger");
        menuItem.setPrice(new BigDecimal("45.00"));
        menuItem.setCategory(category);
        menuItem.setStockQuantity(50);
        menuItem.setAvailable(true);
        menuItem = menuItemRepository.save(menuItem);

        // Confirm that the menu item was saved successfully.
        assertNotNull(menuItem.getMenuItemId(), "Menu item must have an ID generated");

        // Authenticate the test customer for the checkout request.
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(testUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Prepare a queue to collect kitchen messages and a latch to track connection.
        BlockingQueue<String> kitchenMessages = new LinkedBlockingQueue<>();
        CountDownLatch connectedLatch = new CountDownLatch(1);

        // Create the WebSocket client and configure STOMP messaging.
        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);

        // Convert incoming byte messages into UTF-8 strings.
        stompClient.setMessageConverter(new MessageConverter() {
            @Override
            public Object fromMessage(Message<?> message, Class<?> targetClass) {
                Object payload = message.getPayload();
                if (targetClass == String.class && payload instanceof byte[]) {
                    return new String((byte[]) payload, StandardCharsets.UTF_8);
                }
                return null;
            }

            @Override
            public Message<?> toMessage(Object payload, MessageHeaders headers) {
                return null;
            }
        });

        // Build the WebSocket endpoint URL using the application's random port.
        String url = "ws://localhost:" + port + "/ws-orders";

        // Set the origin header to match the frontend development address.
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.add("Origin", "http://localhost:5173");

        StompHeaders connectHeaders = new StompHeaders();

        // Connect to the WebSocket endpoint and wait for the connection result.
        StompSession session = stompClient.connectAsync(
                url,
                handshakeHeaders,
                connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        System.out.println("TEST CONNECTED TO WEBSOCKET");
                        connectedLatch.countDown();
                    }

                    // Print any errors raised while handling STOMP messages.
                    @Override
                    public void handleException(
                            StompSession session,
                            org.springframework.messaging.simp.stomp.StompCommand command,
                            StompHeaders headers,
                            byte[] payload,
                            Throwable exception) {
                        exception.printStackTrace();
                    }

                    // Print transport errors that occur during the connection.
                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        exception.printStackTrace();
                    }
                }
        ).get(5, TimeUnit.SECONDS);

        // Confirm that the STOMP connection was established within five seconds.
        assertTrue(
                connectedLatch.await(5, TimeUnit.SECONDS),
                "WebSocket STOMP connection was not established"
        );

        // Subscribe to the topic used to broadcast active orders to the kitchen.
        session.subscribe("/topic/orders/active", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String receivedMessage = (String) payload;
                System.out.println("KITCHEN RECEIVED:");
                System.out.println(receivedMessage);
                kitchenMessages.add(receivedMessage);
            }
        });

        // Allow time for the subscription to register with the message broker.
        Thread.sleep(1000);

        // Build an order request using the IDs generated by the test database.
        OrderRequestDTO request = new OrderRequestDTO();
        request.setUserId(testUser.getUserId());

        OrderRequestDTO.OrderItemRequest item = new OrderRequestDTO.OrderItemRequest();
        item.setMenuItemId(menuItem.getMenuItemId());
        item.setQuantity(1);
        item.setSpecialInstructions("WebSocket checkout test");
        item.setModifierIds(List.of());

        request.setItems(List.of(item));

        // Submit the order through the checkout service.
        OrderResponseDTO response = orderService.checkout(request);

        // Confirm that checkout returned an order.
        assertNotNull(response, "Checkout response should not be null");
        System.out.println("CHECKOUT CREATED ORDER:");
        System.out.println(response.getCustomerOrderNumber());

        // Wait up to five seconds for the kitchen to receive the order message.
        String kitchenMessage = kitchenMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(kitchenMessage, "Kitchen did not receive new order through WebSocket");

        // Verify that the WebSocket message contains the correct order details.
        assertTrue(kitchenMessage.contains("\"orderId\":" + response.getOrderId()), "Kitchen received incorrect order ID");
        assertTrue(kitchenMessage.contains("\"customerOrderNumber\":\"" + response.getCustomerOrderNumber() + "\""), "Kitchen received incorrect order number");
        assertTrue(kitchenMessage.contains("\"status\":\"Submitted\""), "Kitchen received incorrect order status");

        System.out.println("CHECKOUT WEBSOCKET TEST PASSED");

        // Disconnect the client and stop the STOMP client.
        session.disconnect();
        stompClient.stop();
    }
}