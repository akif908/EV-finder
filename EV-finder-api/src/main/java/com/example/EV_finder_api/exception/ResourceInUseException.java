package com.example.EV_finder_api.exception;

/**
 * The request conflicts with the current state of a resource that other rows
 * still depend on — for example deleting a vehicle that bookings reference.
 * Maps to HTTP 409 Conflict, not 500.
 */
public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message) {
        super(message);
    }
}
