package com.example.EV_finder_api.exception;

/** Validation failures that aren't covered by bean validation — HTTP 400. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
