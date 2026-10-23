package org.example.cafeteria_ordering_backend.component;

import org.example.cafeteria_ordering_backend.exception.ResourceUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperatingHoursValidatorTest {

    // Default cafeteria opening and closing time.
    private static final String DEFAULT_OPEN = "08:00";
    private static final String DEFAULT_CLOSE = "17:30";
    private static final ZoneId SAST_ZONE = ZoneId.of("Africa/Johannesburg");

    // Helper to create a fixed clock at a specific HH:mm (2026-10-09)
    private Clock fixedClockAt(String timeString) {
        String isoDateTime = "2026-10-09T" + timeString + ":00Z";
        return Clock.fixed(Instant.parse(isoDateTime), ZoneId.of("UTC"));
    }

    // Tests whether the cafeteria follows its scheduled operating hours.
    @Nested
    @DisplayName("Scheduled Hours Validation (08:00 - 17:30)")
    class ScheduledHoursTests {

        @Test
        @DisplayName("Should return true when time is within operating hours (e.g., 12:00)")
        void isOpen_WhenWithinOperatingHours_ReturnsTrue() {
            Clock clock = fixedClockAt("12:00");
            OperatingHoursValidator validator = new OperatingHoursValidator(DEFAULT_OPEN, DEFAULT_CLOSE, clock);
            assertThat(validator.isOpen()).isTrue();
        }

        @Test
        @DisplayName("Should return true exactly at opening time (08:00)")
        void isOpen_AtExactOpeningTime_ReturnsTrue() {
            Clock clock = fixedClockAt("08:00");
            OperatingHoursValidator validator = new OperatingHoursValidator(DEFAULT_OPEN, DEFAULT_CLOSE, clock);
            assertThat(validator.isOpen()).isTrue();
        }

        @Test
        @DisplayName("Should return false exactly at closing time (17:30)")
        void isOpen_AtExactClosingTime_ReturnsFalse() {
            Clock clock = fixedClockAt("17:30");
            OperatingHoursValidator validator = new OperatingHoursValidator(DEFAULT_OPEN, DEFAULT_CLOSE, clock);
            assertThat(validator.isOpen()).isFalse();
        }

        @Test
        @DisplayName("Should return false before opening hours (e.g., 07:59)")
        void isOpen_BeforeOpening_ReturnsFalse() {
            Clock clock = fixedClockAt("07:59");
            OperatingHoursValidator validator = new OperatingHoursValidator(DEFAULT_OPEN, DEFAULT_CLOSE, clock);
            assertThat(validator.isOpen()).isFalse();
        }

        @Test
        @DisplayName("Should return false after closing hours (e.g., 18:00)")
        void isOpen_AfterClosing_ReturnsFalse() {
            Clock clock = fixedClockAt("18:00");
            OperatingHoursValidator validator = new OperatingHoursValidator(DEFAULT_OPEN, DEFAULT_CLOSE, clock);
            assertThat(validator.isOpen()).isFalse();
        }

        @Test
        @DisplayName("validateOpenForOrders should throw ResourceUnavailableException when closed")
        void validateOpenForOrders_WhenClosed_ThrowsException() {
            Clock clock = fixedClockAt("20:00");
            OperatingHoursValidator validator = new OperatingHoursValidator(DEFAULT_OPEN, DEFAULT_CLOSE, clock);

            // Checks that orders are rejected when the cafeteria is closed.
            assertThatThrownBy(validator::validateOpenForOrders)
                    .isInstanceOf(ResourceUnavailableException.class)
                    .hasMessageContaining("Cannot place order: The cafeteria is closed");
        }
    }

    // Tests the administrator's ability to override operating hours.
    @Nested
    @DisplayName("Admin Override Operations")
    class AdminOverrideTests {

        @Test
        @DisplayName("Should return true when force open is active, even after hours (e.g., 22:00)")
        void isOpen_WhenForceOpen_ReturnsTrue() {
            Clock clock = fixedClockAt("22:00"); // Normally closed
            OperatingHoursValidator validator = new OperatingHoursValidator(DEFAULT_OPEN, DEFAULT_CLOSE, clock);

            // Forces the cafeteria to remain open outside scheduled hours.
            validator.setForceOpen(true);
            assertThat(validator.isOpen()).isTrue();
            assertThat(validator.isOverridden()).isTrue();
        }

        @Test
        @DisplayName("Should return false when force closed is active, even during operating hours (e.g., 12:00)")
        void isOpen_WhenForceClosed_ReturnsFalse() {
            Clock clock = fixedClockAt("12:00"); // Normally open
            OperatingHoursValidator validator = new OperatingHoursValidator(DEFAULT_OPEN, DEFAULT_CLOSE, clock);

            // Forces the cafeteria to close during scheduled hours.
            validator.setForceOpen(false);
            assertThat(validator.isOpen()).isFalse();
            assertThat(validator.isOverridden()).isTrue();
        }

        @Test
        @DisplayName("Should revert to normal schedule when reset")
        void resetToScheduledHours_RestoresDefaultBehavior() {
            Clock clock = fixedClockAt("22:00"); // Closed time
            OperatingHoursValidator validator = new OperatingHoursValidator(DEFAULT_OPEN, DEFAULT_CLOSE, clock);

            // Enables the override before restoring the normal schedule.
            validator.setForceOpen(true);
            assertThat(validator.isOpen()).isTrue();

            // Removes the override and checks the scheduled hours again.
            validator.resetToScheduledHours();
            assertThat(validator.isOverridden()).isFalse();
            assertThat(validator.isOpen()).isFalse(); // Returns to normal schedule check
        }
    }

    // Tests operating hours that continue past midnight.
    @Nested
    @DisplayName("Overnight Crossover Operating Hours (e.g., 22:00 - 04:00)")
    class OvernightHoursTests {

        @Test
        @DisplayName("Should return true late at night (23:00) during overnight shift")
        void isOpen_OvernightShift_LateNight_ReturnsTrue() {
            Clock clock = fixedClockAt("23:00");
            OperatingHoursValidator validator = new OperatingHoursValidator("22:00", "04:00", clock);
            assertThat(validator.isOpen()).isTrue();
        }

        @Test
        @DisplayName("Should return true early morning (02:00) during overnight shift")
        void isOpen_OvernightShift_EarlyMorning_ReturnsTrue() {
            Clock clock = fixedClockAt("02:00");
            OperatingHoursValidator validator = new OperatingHoursValidator("22:00", "04:00", clock);
            assertThat(validator.isOpen()).isTrue();
        }

        @Test
        @DisplayName("Should return false during daytime (12:00) during overnight shift schedule")
        void isOpen_OvernightShift_Daytime_ReturnsFalse() {
            Clock clock = fixedClockAt("12:00");
            OperatingHoursValidator validator = new OperatingHoursValidator("22:00", "04:00", clock);
            assertThat(validator.isOpen()).isFalse();
        }
    }
}