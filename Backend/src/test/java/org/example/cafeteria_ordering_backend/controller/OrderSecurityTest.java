package org.example.cafeteria_ordering_backend.controller;

import org.example.cafeteria_ordering_backend.config.SecurityConfig;
import org.example.cafeteria_ordering_backend.dto.requests.OrderStatusUpdateRequestDTO;
import org.example.cafeteria_ordering_backend.security.CustomUserDetailsService;
import org.example.cafeteria_ordering_backend.security.JwtUtils;
import org.example.cafeteria_ordering_backend.security.TokenBlacklistService;
import org.example.cafeteria_ordering_backend.service.interfaces.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
public class OrderSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @Nested
    @DisplayName("Authentication Requirements")
    class UnauthenticatedAccessTests {

        @Test
        @DisplayName("Should return 401 Unauthorized when unauthenticated user attempts checkout")
        void checkout_Unauthenticated () throws Exception {

            // Submit checkout request without authentication
            mockMvc.perform(post("/api/orders/checkout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")).andExpect(status()
                    .isUnauthorized());

            // Ensure order service is not invoked for unauthenticated request
            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when unauthenticated user accesses order history")
        void getOrders_Unauthenticated () throws Exception {

            // Request order history without authentication
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isUnauthorized());

            // Ensure unauthed access does not work
            verifyNoInteractions(orderService);
        }
    }

    @Nested
    @DisplayName("Role-Based Authorization (403 Forbidden)")
    class RoleBasedAccessTests {

        @Test
        @DisplayName("Should return 403 forbidden when a cust attempts to update ord status")
        void updateStatus_ForbiddenForCustomer () throws Exception {

            // Prepare request to mark order as complete
            OrderStatusUpdateRequestDTO dto = new OrderStatusUpdateRequestDTO("COMPLETED");

            // Customer must be unable to update order status
            mockMvc.perform(put("/api/orders/100/status")
                    .with(user("customer@campus.edu").roles("CUSTOMER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());

            // Ensure business logic is not called
            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("Should allow ADMIN to update order status")
        void updateStatus_AllowForAdmin () throws Exception {

            // Prepare status update request
            OrderStatusUpdateRequestDTO dto = new OrderStatusUpdateRequestDTO("COMPLETED");

            // Submit updated as a valid user role
            mockMvc.perform(put("/api/orders/100/status")
                    .with(user("admin@campus.edu").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            // Ensure service receives order with new status
            verify(orderService).updateOrderStatus(100L, "COMPLETED");
        }


        @Test
        @DisplayName("Should return 403 Forbidden when a STUDENT attempts to update order status")
        void updateStatus_ForbiddenForStudent () throws Exception {

            // Prep status update request
            OrderStatusUpdateRequestDTO dto = new OrderStatusUpdateRequestDTO("COMPLETED");

            // Student may not pdate an order status
            mockMvc.perform(put("/api/orders/100/status")
                    .with(user("student@eduvos.com").roles("STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());

            // Ensure forbidden request does not interact with business logic
            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("Should allow STAFF role to update order status")
        void updateStatus_AllowedForStaff () throws Exception {

            // Prepare valid status update request
            OrderStatusUpdateRequestDTO dto = new OrderStatusUpdateRequestDTO("COMPLETED");

            // Submit update as authorized member
            mockMvc.perform(put("/api/orders/100/status")
                    .with(user("staff@eduvos.com").roles("STAFF"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            // Ensure authorized request reaches Order service
            verify(orderService).updateOrderStatus(100L, "COMPLETED");
        }
    }
}
