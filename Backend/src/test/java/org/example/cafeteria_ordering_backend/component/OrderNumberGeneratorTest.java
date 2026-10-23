package org.example.cafeteria_ordering_backend.component;

import org.example.cafeteria_ordering_backend.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderNumberGeneratorTest {

    // Mock repository to simulate checking existing order numbers.
    @Mock
    private OrderRepository orderRepository;

    // Generator being tested.
    private OrderNumberGenerator generator;

    // Sets up a fixed clock to make order number generation predictable.
    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-10-09T10:15:30.00Z"),
                ZoneId.of("Africa/Johannesburg")
        );
        generator = new OrderNumberGenerator(fixedClock, orderRepository);
    }

    @Test
    @DisplayName("Generates unique order number on first attempt when no collision exists")
    void testGenerateOrderNumberSuccess() {

        // Simulates that the generated order number does not already exist.
        when(orderRepository.existsByCustomerOrderNumber(anyString())).thenReturn(false);

        String orderNumber = generator.generate();

        // Checks that an order number is generated in the expected format.
        assertNotNull(orderNumber);
        assertTrue(orderNumber.matches("^ORD-\\d{2}-\\d{2}-[A-Z0-9]{4}$"));

        // Verifies that the repository is checked once for duplicate numbers.
        verify(orderRepository, times(1)).existsByCustomerOrderNumber(orderNumber);
    }

    @Test
    @DisplayName("Retries generation when a collision occurs")
    void testCollisionRetry() {
        // Simulates a duplicate on the first attempt and a unique number on the second.
        when(orderRepository.existsByCustomerOrderNumber(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        String orderNumber = generator.generate();

        // Confirms that a number is generated after the collision is resolved.
        assertNotNull(orderNumber);

        // Verifies that the repository is checked twice.
        verify(orderRepository, times(2)).existsByCustomerOrderNumber(anyString());
    }
}