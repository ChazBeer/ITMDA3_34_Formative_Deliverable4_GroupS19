package org.example.cafeteria_ordering_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Returns HTTP 404 Not Found when this exception is thrown.
@ResponseStatus(HttpStatus.NOT_FOUND)


public class ResourceNotFoundException extends RuntimeException {

    // Creates the exception with a message describing the missing
    public ResourceNotFoundException (String message) {
        super(message);
    }
}