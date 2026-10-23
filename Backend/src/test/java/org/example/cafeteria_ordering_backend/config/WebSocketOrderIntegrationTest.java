package org.example.cafeteria_ordering_backend.config;

import org.example.cafeteria_ordering_backend.dto.requests.OrderRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.OrderResponseDTO;
import org.example.cafeteria_ordering_backend.entity.*;
import org.example.cafeteria_ordering_backend.repository.*;
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
class WebSocketOrderIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OrderService orderService;

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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void orderStatusUpdateShouldBeSentToStudentAndKitchen() throws Exception {

        // 1. Ensure required order statuses exist in H2
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

        // 2. Create or retrieve the CUSTOMER role
        Role customerRole = roleRepository.findByRoleName("CUSTOMER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setRoleName("CUSTOMER");
            return roleRepository.saveAndFlush(newRole);
        });

        // 3. Create or retrieve the test user with a unique email and customer code for this test
        User testUser = userRepository.findByEmail("order-test@edu.com").orElseGet(() -> {
            User newUser = new User();
            newUser.setCustomerCode("CUST-ORD-01");
            newUser.setEmail("order-test@edu.com");
            newUser.setPasswordHash("Password123!");
            newUser.setFirstName("Order");
            newUser.setLastName("TestUser");
            newUser.setRole(customerRole);
            return userRepository.saveAndFlush(newUser);
        });

        assertNotNull(testUser.getUserId(), "Test user must have an ID generated");

        // 4. Create or retrieve a test category and menu item
        Category category = categoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            Category newCat = new Category();
            newCat.setCategoryName("Test Category");
            return categoryRepository.saveAndFlush(newCat);
        });

        MenuItems menuItem = menuItemRepository.findAll().stream().findFirst().orElseGet(() -> {
            MenuItems newItem = new MenuItems();
            newItem.setName("Test Burger");
            newItem.setPrice(new BigDecimal("45.00"));
            newItem.setCategory(category);
            newItem.setStockQuantity(50);
            newItem.setAvailable(true);
            return menuItemRepository.saveAndFlush(newItem);
        });

        // 5. Authenticate as the customer
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUser.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        BlockingQueue<String> studentMessages = new LinkedBlockingQueue<>();
        BlockingQueue<String> kitchenMessages = new LinkedBlockingQueue<>();
        CountDownLatch connectedLatch = new CountDownLatch(1);

        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);

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

        String url = "ws://localhost:" + port + "/ws-orders";

        StompSession session = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        System.out.println("TEST CONNECTED TO WEBSOCKET");
                        connectedLatch.countDown();
                    }

                    @Override
                    public void handleException(StompSession session, org.springframework.messaging.simp.stomp.StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                        exception.printStackTrace();
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        exception.printStackTrace();
                    }
                })
                .get(5, TimeUnit.SECONDS);

        assertTrue(connectedLatch.await(5, TimeUnit.SECONDS), "WebSocket STOMP connection was not established");

        // Subscribe using the dynamic user ID rather than hardcoded 1
        session.subscribe("/topic/orders/" + testUser.getUserId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return String.class; }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String receivedMessage = (String) payload;
                System.out.println("STUDENT RECEIVED:\n" + receivedMessage);
                studentMessages.add(receivedMessage);
            }
        });

        session.subscribe("/topic/orders/active", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return String.class; }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String receivedMessage = (String) payload;
                System.out.println("KITCHEN RECEIVED:\n" + receivedMessage);
                kitchenMessages.add(receivedMessage);
            }
        });

        Thread.sleep(1000);

        OrderRequestDTO request = new OrderRequestDTO();
        request.setUserId(testUser.getUserId());

        OrderRequestDTO.OrderItemRequest item = new OrderRequestDTO.OrderItemRequest();
        item.setMenuItemId(menuItem.getMenuItemId());
        item.setQuantity(1);
        item.setSpecialInstructions("Status update test");
        item.setModifierIds(List.of());

        request.setItems(List.of(item));

        OrderResponseDTO createdOrder = orderService.checkout(request);
        assertNotNull(createdOrder, "Created order should not be null");

        Long orderId = createdOrder.getOrderId();
        String expectedOrderNumber = createdOrder.getCustomerOrderNumber();

        orderService.updateOrderStatus(orderId, "Accepted");

        String studentMessage = pollForStatus(studentMessages, orderId, "Accepted");
        String kitchenMessage = pollForStatus(kitchenMessages, orderId, "Accepted");

        assertNotNull(studentMessage, "Student did not receive WebSocket message for Accepted status");
        assertNotNull(kitchenMessage, "Kitchen did not receive WebSocket message for Accepted status");

        assertTrue(studentMessage.contains("\"orderId\":" + orderId), "Student message has incorrect order ID");
        assertTrue(studentMessage.contains("\"customerOrderNumber\":\"" + expectedOrderNumber + "\""), "Student message has incorrect order number");
        assertTrue(studentMessage.contains("\"status\":\"Accepted\""), "Student message has incorrect status");

        assertTrue(kitchenMessage.contains("\"orderId\":" + orderId), "Kitchen message has incorrect order ID");
        assertTrue(kitchenMessage.contains("\"customerOrderNumber\":\"" + expectedOrderNumber + "\""), "Kitchen message has incorrect order number");
        assertTrue(kitchenMessage.contains("\"status\":\"Accepted\""), "Kitchen message has incorrect status");

        System.out.println("WEBSOCKET INTEGRATION TEST PASSED");

        session.disconnect();
        stompClient.stop();
    }

    private String pollForStatus(BlockingQueue<String> queue, Long orderId, String status)
            throws InterruptedException {
        long endTime = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < endTime) {
            String message = queue.poll(500, TimeUnit.MILLISECONDS);
            if (message != null && message.contains("\"orderId\":" + orderId) && message.contains("\"status\":\"" + status + "\"")) {
                return message;
            }
        }
        return null;
    }
}