package org.example.cafeteria_ordering_backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

// Not including @setter as errors should be immutable
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;

    // Constructor for  error response with the current timestamp and error details.
    public ErrorResponse (int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // Constructor for validation errors (auto-sets timestamp)
    public ErrorResponse (int status, String error, String message, String path, Map<String, String> validationErrors) {

        this(status, error, message, path);
        this.validationErrors = validationErrors;
    }
}