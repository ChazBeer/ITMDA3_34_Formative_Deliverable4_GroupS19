package org.example.cafeteria_ordering_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cafeteria_ordering_backend.dto.requests.UserCreateRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserResponseDTO;
import org.example.cafeteria_ordering_backend.mapper.UserMapper;
import org.example.cafeteria_ordering_backend.security.CustomUserDetailsService;
import org.example.cafeteria_ordering_backend.security.JwtUtils;
import org.example.cafeteria_ordering_backend.security.TokenBlacklistService;
import org.example.cafeteria_ordering_backend.service.interfaces.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/users - Should return 201 CREATED for valid request")
    void createValidUserReturns201 () throws Exception {

        // Builds valid request with new users details
        UserCreateRequestDTO requestDTO = UserCreateRequestDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .passwordHash("securePass123")
                .build();

        // Defines expected response returned by service layer
        UserResponseDTO responseDTO = UserResponseDTO.builder()
                .userId(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .roleName("ROLE_STUDENT")
                .build();

        // Stub user creation to return predefined response for given role ID
        when(userService.createUser(any(UserCreateRequestDTO.class), eq(1L))).thenReturn(responseDTO);

        // Submit user creation request as authenticated admin
        mockMvc.perform(post("/api/users")
                .param("roleId", "1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper
                        .writeValueAsString(requestDTO)))
                // Verify creation success
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId")
                        .value(1))
                .andExpect(jsonPath("$.email")
                        .value("john@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/users - Should return 400 BAD REQUEST when email/fields are invalid")
    void createInvalidUserReturns400 () throws Exception {

        // Build request with empty first name, incorrect email and short pass
        UserCreateRequestDTO invalidDto = UserCreateRequestDTO
                .builder()
                .firstName("")
                .lastName("Doe")
                .email("not-an-email")
                .passwordHash("123").build();

        // Submit the invalid request as an authenticated admin
        mockMvc.perform(post("/api/users")
                .param("roleId", "1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                // Ensure that validation rejects the request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}