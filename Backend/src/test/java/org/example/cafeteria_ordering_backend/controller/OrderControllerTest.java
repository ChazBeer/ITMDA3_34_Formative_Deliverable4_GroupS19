package org.example.cafeteria_ordering_backend.controller;

import org.example.cafeteria_ordering_backend.dto.requests.OrderRequestDTO;
import org.example.cafeteria_ordering_backend.dto.requests.OrderStatusUpdateRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.OrderResponseDTO;
import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.security.JwtAuthenticationFilter;
import org.example.cafeteria_ordering_backend.security.JwtUtils;
import org.example.cafeteria_ordering_backend.service.interfaces.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypasses custom security filters during slice testing
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private OrderRequestDTO validOrderRequest;
    private OrderResponseDTO sampleOrderResponse;

    @BeforeEach
    void setUp () {

        // Create order item with quantity, customization, and add-ons
        OrderRequestDTO.OrderItemRequest itemRequest = new OrderRequestDTO.OrderItemRequest(
                1L,
                2,
                "Extra Crispy",
                List.of(10L, 11L)
        );

        // Creating valid checkout request containing the prepared order item
        validOrderRequest = new OrderRequestDTO(1L, List.of(itemRequest));
        sampleOrderResponse = new OrderResponseDTO();
        sampleOrderResponse.setOrderId(100L);
        sampleOrderResponse.setStatus("PENDING");
        sampleOrderResponse.setTotalPrice(new BigDecimal("45.50"));
    }

    @Nested
    @DisplayName("POST /api/orders/checkout - Checkout Order")
    class CheckoutOrderTests {

        @Test
        @DisplayName("Should return 201 Created and checkout order successfully")
        @WithMockUser(username = "student@eduvos.com", roles = {"STUDENT"})
        void checkout_Success () throws Exception {

            // Set up the service to return the prepared response
            when(orderService.checkout(any(OrderRequestDTO.class))).thenReturn(sampleOrderResponse);

            // Submit the checkout req as authenticated user
            mockMvc.perform(post("/api/orders/checkout").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validOrderRequest)))
                    // Verify status and returned order info
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderId").value(100))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalPrice").value(45.50));

            // Ensure service was called only once
            verify(orderService, times(1)).checkout(any(OrderRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("GET /api/orders - Fetch All & Filter Orders")
    class GetOrderTests {

        @Test
        @DisplayName("Should return 200 OK with list of all orders")
        @WithMockUser(username = "staff@eduvos.com", roles = {"STAFF"})
        void getAllOrders_Success () throws Exception {

            // Configure service to return list containing one order
            when(orderService.getAllOrders()).thenReturn(List.of(sampleOrderResponse));

            // Get all orders and verify response
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].orderId").value(100));

            // Ensure service receives all orders only once
            verify(orderService, times(1)).getAllOrders();
        }

        @Test
        @DisplayName("Should return 200 OK and order details when valid numeric ID is passed")
        @WithMockUser(username = "student@eduvos.com")
        void getOrderById_Success () throws Exception {

            // Setup service to return requested order
            when(orderService.getOrderById(100L)).thenReturn(sampleOrderResponse);

            // Get order by id and check details
            mockMvc.perform(get("/api/orders/{id}", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(100))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            // Check service gets expected orderID only once
            verify(orderService, times(1)).getOrderById(100L);
        }

        @Test
        @DisplayName("Should return 404 Not Found when order ID does not exist")
        @WithMockUser(username = "student@eduvos.com")
        void getOrderById_NotFound () throws Exception {

            // Simulate missing order by setting up service to throw known exception
            when(orderService.getOrderById(999L))
                    .thenThrow(new ResourceNotFoundException("Order not found with ID: 999"));

            // Ensure exception is translated to HTTP 404 error
            mockMvc.perform(get("/api/orders/{id}", 999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 200 OK for user's order history")
        @WithMockUser(username = "student@eduvos.com")
        void getOrdersByUserId_Success () throws Exception {

            // Setup service to return users order history
            when(orderService.getOrdersByUserId(1L)).thenReturn(List.of(sampleOrderResponse));

            // Get order history and check response
            mockMvc.perform(get("/api/orders/user/{userId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].orderId").value(100));

            // Ensure service gets history for requested user ID
            verify(orderService, times(1)).getOrdersByUserId(1L);
        }

        @Test
        @DisplayName("Should return 200 OK for active cafeteria orders board")
        @WithMockUser(username = "staff@eduvos.com")
        void getActiveOrders_Success () throws Exception {

            // Setup service to return active orders
            when(orderService.getActiveOrders()).thenReturn(List.of(sampleOrderResponse));

            // Get active orders & check response size
            mockMvc.perform(get("/api/orders/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            // Check active order retrieval method is invoked once
                verify(orderService, times(1)).getActiveOrders();
        }

        @Test
        @DisplayName("Should return 200 OK for user's current active orders")
        @WithMockUser(username = "student@eduvos.com")
        void getActiveOrdersByUserId_Success () throws Exception {

            // Setup service to return users active orders
            when(orderService.getActiveOrdersByUserId(1L)).thenReturn(List.of(sampleOrderResponse));

            // Get current orders
            mockMvc.perform(get("/api/orders/user/{userId}/current", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            // Check service received the expected ID
            verify(orderService, times(1)).getActiveOrdersByUserId(1L);
        }
    }

    @Nested
    @DisplayName("PUT /api/orders/{id}/status - Update Order Status")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Should return 200 OK when staff updates order status using OrderStatusUpdateRequestDTO")
        @WithMockUser(username = "staff@eduvos.com", roles = {"STAFF"})
        void updateOrderStatus_Success () throws Exception {

            //Prepare request to change order status
            OrderStatusUpdateRequestDTO statusDTO = new OrderStatusUpdateRequestDTO("IN_PREPARATION");

            // Update response
            sampleOrderResponse.setStatus("IN_PREPARATION");
            when(orderService.updateOrderStatus(eq(100L), eq("IN_PREPARATION")))
                    .thenReturn(sampleOrderResponse);

            // Submit status update, verify returned status
            mockMvc.perform(put("/api/orders/{id}/status", 100L).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PREPARATION"));

            // Ensure service gets correct order IDand new status
            verify(orderService, times(1)).updateOrderStatus(100L, "IN_PREPARATION");
        }
    }
}