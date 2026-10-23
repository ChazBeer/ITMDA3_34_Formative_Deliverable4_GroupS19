package org.example.cafeteria_ordering_backend.component;

import org.example.cafeteria_ordering_backend.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;

@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int RANDOM_LEN = 4;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final OrderRepository orderRepository;

    public OrderNumberGenerator (Clock clock, OrderRepository orderRepository) {
        this.clock = clock;
        this.orderRepository = orderRepository;
    }

    /**
     * Creates a unique, URL-safe order number (e.g., ORD-10-09-X7K2).
     */
    public String generate () {

        String datePart = LocalDateTime.now(clock).format(DATE_FORMATTER);
        String customerOrderNum;
        int attempts = 0;

        // Generate a new number until it is unique or the attempt limit is reached.
        do {
            customerOrderNum = buildCandidateNum(datePart);
            attempts++;

            // Stop if a unique order number cannot be generated within the limit.
            if (attempts > MAX_ATTEMPTS) {
                throw new IllegalArgumentException(
                        "Failed to generate a unique order number after: " + MAX_ATTEMPTS + "attempts.");
            }
        } while(orderRepository.existsByCustomerOrderNumber(customerOrderNum));

        // Return the order number once it is confirmed as unused.
        return customerOrderNum;
    }

    // Builds an order number using the data and a random four-character code.
    private String buildCandidateNum (String datePart) {

        StringBuilder randomPart = new StringBuilder(RANDOM_LEN);

        // Generate each character from the available letters and digits.
        for (int i = 0; i < RANDOM_LEN; i++) {
            randomPart.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        // Combine the prefix, data, and random code into the final order number.
        return "ORD-" + datePart + "-" + randomPart;
    }
}