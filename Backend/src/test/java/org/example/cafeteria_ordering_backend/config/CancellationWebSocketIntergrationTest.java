package org.example.cafeteria_ordering_backend.config;

import org.example.cafeteria_ordering_backend.dto.requests.OrderRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.OrderResponseDTO;
import org.example.cafeteria_ordering_backend.entity.*;
import org.example.cafeteria_ordering_backend.repository.CategoryRepository;
import org.example.cafeteria_ordering_backend.repository.MenuItemRepository;
import org.example.cafeteria_ordering_backend.repository.OrderStatusRepository;
import org.example.cafeteria_ordering_backend.repository.RoleRepository;
import org.example.cafeteria_ordering_backend.repository.UserRepository;
import org.example.cafeteria_ordering_backend.security.CustomUserDetailsService;
import org.example.cafeteria_ordering_backend.security.JwtUtils;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                "SPRING_DATASOURCE_USERNAME=sa",
                "SPRING_DATASOURCE_PASSWORD=",
                "SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver",
                "jwt.secret=VGhpcylzYXZlcnlzZWN1cmVzdGVudHNlY3JldGtleWZvcmNhZmV0ZXJpYWhzaTUxMg==",
                "jwt.expiration-ms=900000"
        }
)
@ActiveProfiles("test")
class CancellationWebSocketIntegrationTest {

    // Stores the random port assigned to the test server.
    @LocalServerPort
    private int port;

    // Services and repositories used to create and manage test orders.
    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private OrderStatusRepository orderStatusRepository;

    // Clears the security context after each test.
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cancellationShouldSendUpdateToStudentAndKitchen() throws Exception {

        // Creates the CUSTOMER role if it does not already exist.
        Role customerRole = roleRepository.findByRoleName("CUSTOMER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setRoleName("CUSTOMER");
            return roleRepository.saveAndFlush(newRole);
        });

        // Creates the test customer if the account does not already exist.
        User testUser = userRepository.findByEmail("alice@campus.edu").orElseGet(() -> {
            User newUser = new User();
            newUser.setCustomerCode("CUST-001");
            newUser.setEmail("alice@campus.edu");
            newUser.setPasswordHash("Password123!");
            newUser.setFirstName("Alice");
            newUser.setLastName("Campus");
            newUser.setRole(customerRole);
            return userRepository.saveAndFlush(newUser);
        });

        // Ensures all order statuses required by the order workflow exist.
        List.of("Submitted", "Accepted", "Preparing", "Ready", "Collected", "Cancelled").forEach(name -> {
            orderStatusRepository.findByStatusName(name).orElseGet(() -> {
                OrderStatus status = new OrderStatus();
                status.setStatusName(name);
                status.setSortOrder(
                        name.equals("Submitted") ? 1 :
                                name.equals("Accepted") ? 2 :
                                        name.equals("Preparing") ? 3 :
                                                name.equals("Ready") ? 4 :
                                                        name.equals("Collected") ? 5 : 6
                );
                status.setIsFinal(name.equals("Collected") || name.equals("Cancelled"));
                return orderStatusRepository.saveAndFlush(status);
            });
        });

        // Retrieves an existing category or creates one for the test.
        Category category = categoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            Category newCat = new Category();
            newCat.setCategoryName("Test Category");
            return categoryRepository.saveAndFlush(newCat);
        });

        // Retrieves a test menu item or creates one if it does not exist.
        MenuItems menuItem = menuItemRepository.findById(1L).orElseGet(() -> {
            MenuItems newItem = new MenuItems();
            newItem.setName("Test Burger");
            newItem.setPrice(new BigDecimal("45.00"));
            newItem.setCategory(category);
            newItem.setStockQuantity(50);
            newItem.setAvailable(true);
            return menuItemRepository.saveAndFlush(newItem);
        });

        // Ensures the menu item is available and has sufficient stock.
        menuItem.setStockQuantity(50);
        menuItem.setAvailable(true);
        menuItemRepository.saveAndFlush(menuItem);

        // Loads the customer's security details and authenticates the user.
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(testUser.getEmail());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token for WebSocket authentication
        String token = jwtUtils.generateToken(userDetails.getUsername());

        // Builds an order request containing one menu item.
        OrderRequestDTO request = new OrderRequestDTO();
        request.setUserId(testUser.getUserId()); // Resolves correctly

        OrderRequestDTO.OrderItemRequest item = new OrderRequestDTO.OrderItemRequest();
        item.setMenuItemId(menuItem.getMenuItemId());
        item.setQuantity(1);
        item.setSpecialInstructions("WebSocket cancellation test");
        item.setModifierIds(List.of());

        request.setItems(List.of(item));

        // Creates queues to store messages received by the student and kitchen.
        BlockingQueue<String> studentMessages = new LinkedBlockingQueue<>();
        BlockingQueue<String> kitchenMessages = new LinkedBlockingQueue<>();

        // Signals when the WebSocket connection has been established.
        CountDownLatch connectedLatch = new CountDownLatch(1);

        // Creates the WebSocket client and STOMP messaging client.
        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);

        // Converts incoming WebSocket message payloads into strings.
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

        // Defines the WebSocket endpoint exposed by the test server.
        String url = "ws://localhost:" + port + "/ws-orders";

        // Add JWT token to STOMP CONNECT headers
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        // Connects to the WebSocket endpoint and handles connection events.
        StompSession session = stompClient.connectAsync(
                url,
                new WebSocketHttpHeaders(), // WebSocket transport headers
                connectHeaders,             // STOMP CONNECT frame headers
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {System.out.println("TEST CONNECTED TO WEBSOCKET");connectedLatch.countDown();}

                    @Override
                    public void handleException(StompSession session, org.springframework.messaging.simp.stomp.StompCommand command,
                                                StompHeaders headers, byte[] payload, Throwable exception) {exception.printStackTrace();}

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {exception.printStackTrace();}
                }
        ).get(5, TimeUnit.SECONDS);

        // Fails the test if the connection is not established within five seconds.
        assertTrue(connectedLatch.await(5, TimeUnit.SECONDS), "WebSocket STOMP connection was not established");

        // Subscribes to updates for the customer's own orders.
        session.subscribe("/topic/orders/" + testUser.getUserId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = (String) payload;
                System.out.println("STUDENT RECEIVED:\n" + message);
                studentMessages.add(message);
            }
        });

        // Subscribes to updates sent to the kitchen's active-order topic.
        session.subscribe("/topic/orders/active", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = (String) payload;
                System.out.println("KITCHEN RECEIVED:\n" + message);
                kitchenMessages.add(message);
            }
        });

        // Allows time for both subscriptions to become active.
        Thread.sleep(1000);

        // Creates the order and verifies that it was returned successfully.
        OrderResponseDTO createdOrder = orderService.checkout(request);
        assertNotNull(createdOrder, "Created order should not be null");

        Long orderId = createdOrder.getOrderId();
        System.out.println("CREATED ORDER ID: " + orderId);

        // Cancels the order to trigger WebSocket status updates.
        System.out.println("CANCELLING ORDER " + orderId);
        orderService.cancelOrder(orderId);

        // Waits for the student topic to receive the cancellation update.
        String studentCancellationMessage = pollForStatus(studentMessages, orderId, "Cancelled", 5000);
        assertNotNull(studentCancellationMessage, "Student did not receive Cancelled status for order " + orderId);

        // Waits for the kitchen topic to receive the cancellation update.
        String kitchenCancellationMessage = pollForStatus(kitchenMessages, orderId, "Cancelled", 5000);
        assertNotNull(kitchenCancellationMessage, "Kitchen did not receive Cancelled status for order " + orderId);

        System.out.println("CANCELLATION WEBSOCKET TEST PASSED");

        // Closes the WebSocket session and stops the client.
        session.disconnect();
        stompClient.stop();
    }

    // Waits for a message containing the expected order ID and status.
    private String pollForStatus(BlockingQueue<String> queue, Long orderId, String expectedStatus, long timeoutMillis)
            throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeoutMillis;
        String targetOrderId = "\"orderId\":" + orderId;
        String targetStatus = "\"status\":\"" + expectedStatus + "\"";

        // Checks incoming messages until a match is found or the timeout expires.
        while (System.currentTimeMillis() < endTime) {
            String message = queue.poll(200, TimeUnit.MILLISECONDS);
            if (message != null && message.contains(targetOrderId) && message.contains(targetStatus)) {
                return message;
            }
        }
        // Returns null if the expected message was not recieved in time.
        return null;
    }
}