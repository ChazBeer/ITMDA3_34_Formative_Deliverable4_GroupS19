package org.example.cafeteria_ordering_backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.cafeteria_ordering_backend.component.OperatingHoursValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cafeteria-status")
@RequiredArgsConstructor
public class CafeteriaStatusController {

    private final OperatingHoursValidator operatingHoursValidator;

    // Return the current cafeteria status, override state and operating hours.
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus () {

        return ResponseEntity.ok(Map.of(
                "isOpen", operatingHoursValidator.isOpen(),
                "isOverridden", operatingHoursValidator.isOverridden(),
                "overrideState", operatingHoursValidator.getOverrideState() != null
                        ? operatingHoursValidator.getOverrideState()
                        : "SCHEDULED",
                "openingTime", operatingHoursValidator.getOpeningTime().toString(),
                "closingTime", operatingHoursValidator.getClosingTime().toString()
        ));
    }

    // Allow administrators to open the cafeteria regardless of schedules hours.
    @PostMapping("/force-open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceOpen () {

        operatingHoursValidator.setForceOpen(true);
        return ResponseEntity.ok("Cafeteria status set to FORCE OPEN.");
    }

    // Allow administrators to close the cafeteria regardless of scheduled hours.
    @PostMapping("/force-close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceClose () {

        operatingHoursValidator.setForceOpen(false);
        return ResponseEntity.ok("Cafeteria status set to FORCE CLOSED.");
    }

    // Remove the manual override and return to the scheduled operating hours.
    @PostMapping("/reset-schedule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetSchedule () {

        operatingHoursValidator.resetToScheduledHours();
        return ResponseEntity.ok("Cafeteria status reset to normal schedule.");
    }
}