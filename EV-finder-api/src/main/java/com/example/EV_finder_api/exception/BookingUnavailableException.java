package com.example.EV_finder_api.exception;

/** Raised when the requested slot has no free capacity — HTTP 409. */
public class BookingUnavailableException extends RuntimeException {
    public BookingUnavailableException(String message) {
        super(message);
    }
}
