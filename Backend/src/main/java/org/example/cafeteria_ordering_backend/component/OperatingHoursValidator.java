package org.example.cafeteria_ordering_backend.component;

import lombok.Getter;
import org.example.cafeteria_ordering_backend.exception.ResourceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
@Getter
public class OperatingHoursValidator {

    // Configured opening and closing time of cafeteria.
    private final LocalTime openingTime;
    private final LocalTime closingTime;

    // Provide the current time for operating hours checks.
    private final Clock clock;

    // null = scheduled hours, true = forced open, false = forced closed.
    private Boolean overrideState = null;

    public OperatingHoursValidator (
            @Value("${app.cafeteria.opening-time:08:00}") String openingTime,
            @Value("${app.cafeteria.closing-time:17:30}") String closingTime,
            Clock clock) {

        // Convert the configured time strings to LocalTime objects.
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        this.openingTime = LocalTime.parse(openingTime, formatter);
        this.closingTime = LocalTime.parse(closingTime, formatter);
        this.clock = clock;
    }

    // Checks whether the cafeteria is currently open.
    public boolean isOpen () {

        // Use admin override if one has been set.
        if (overrideState != null) {
            return overrideState;
        }

        // Get the current time using the configured clock.
        LocalTime now = LocalTime.now(clock);

        // Check operating hours when opening and closing occur on the same day.
        if (openingTime.isBefore(closingTime)) {
            return !now.isBefore(openingTime) && now.isBefore(closingTime);
        }

        // Handle operating hours that continue past midnight.
        return !now.isBefore(openingTime) || now.isBefore(closingTime);
    }

    // Prevents orders from being places while the cafeteria is closed.
    public void validateOpenForOrders () {

        if (!isOpen()) {
            throw new ResourceUnavailableException(
                    String.format("Cannot place order: The cafeteria is closed. " +
                                    "Pre-orders are only accepted between %s and %s.",
                            openingTime, closingTime)
            );
        }
    }

    // Admin Controls

    // Forces the cafeteria open or closed, regardless of scheduled hours.
    public void setForceOpen (boolean forceOpen) {
        this.overrideState = forceOpen;
    }

    // Removes admin override and restores the scheduled operating hours.
    public void resetToScheduledHours () {
        this.overrideState = null;
    }

    // Checks whether an admin override is currently active
    public boolean isOverridden () {
        return this.overrideState != null;
    }
}