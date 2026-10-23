package org.example.cafeteria_ordering_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Returns HTTP 400 Bad Request when this exception is thrown.
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ResourceUnavailableException extends RuntimeException {

   // Creates the exception with a message explaining why the resource is unavailable.
    public ResourceUnavailableException (String message) {
        super(message);
    }
}