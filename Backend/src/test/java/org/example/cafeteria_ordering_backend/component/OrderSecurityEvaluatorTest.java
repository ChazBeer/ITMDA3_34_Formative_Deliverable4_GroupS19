package org.example.cafeteria_ordering_backend.component;

import org.example.cafeteria_ordering_backend.entity.User;
import org.example.cafeteria_ordering_backend.exception.ResourceUnavailableException;
import org.example.cafeteria_ordering_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderSecurityEvaluatorTest {

    // Mock respository to simulate retrieving users from the database.
    @Mock
    private UserRepository userRepository;

    // Mocks the security context containing the current user's authentication.
    @Mock
    private SecurityContext securityContext;

    // Mocks the authentication details of the current user.
    @Mock
    private Authentication authentication;

    // Creates the evaluator and injects the mocked dependencies.
    @InjectMocks
    private OrderSecurityEvaluator securityEvaluator;

    // Initialises the mocks and sets the security context for each test.
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("getCurrentUser returns user when authenticated")
    void testGetCurrentUserSuccess() {
        // Simulates an authenticated user with a known email address.
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("student@eduvos.com");

        User mockUser = new User();
        mockUser.setUserId(100L);
        mockUser.setEmail("student@eduvos.com");

        // Returns the matching user when the repository searches by email.
        when(userRepository.findByEmail("student@eduvos.com")).thenReturn(Optional.of(mockUser));

        User result = securityEvaluator.getCurrentUser();

        // Verifies that the correct user is returned.
        assertNotNull(result);
        assertEquals(100L, result.getUserId());
    }

    @Test
    @DisplayName("isStaffOrAdmin returns true for ROLE_STAFF or ROLE_ADMIN")
    void testIsStaffOrAdminTrue() {
        // Assigns the staff role to the authenticated user.
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_STAFF")))
                .when(authentication).getAuthorities();

        // Verifies that staff members pass the role check.
        assertTrue(securityEvaluator.isStaffOrAdmin());
    }

    @Test
    @DisplayName("validateUserAccess allows customer accessing their own data")
    void testValidateUserAccessOwnDataSuccess() {
        // Simulates an authenticated customer accessing their own account.
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("student@eduvos.com");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .when(authentication).getAuthorities();

        User mockUser = new User();
        mockUser.setUserId(5L);

        // Returns the customer's account associated with their email.
        when(userRepository.findByEmail("student@eduvos.com")).thenReturn(Optional.of(mockUser));

        // Verifies the accessing their own user ID is permitted.
        assertDoesNotThrow(() -> securityEvaluator.validateUserAccess(5L));
    }

    @Test
    @DisplayName("validateUserAccess throws ResourceUnavailableException when customer accesses another user's data")
    void testValidateUserAccessOtherDataDenied() {
        // Simulates an authenticated customer attempting to access another account.
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("student@eduvos.com");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .when(authentication).getAuthorities();

        User mockUser = new User();
        mockUser.setUserId(5L);

        // Returns the current customer's account.
        when(userRepository.findByEmail("student@eduvos.com")).thenReturn(Optional.of(mockUser));

        // Verifies that access to another user's data is denied.
        assertThrows(ResourceUnavailableException.class, () -> securityEvaluator.validateUserAccess(99L));
    }
}