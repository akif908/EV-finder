package com.example.EV_finder_api.service;

import com.example.EV_finder_api.dto.PaymentRequest;
import com.example.EV_finder_api.dto.PaymentResponse;

public interface PaymentService {

    /**
     * Simulated payment for a PENDING booking. Success confirms the booking;
     * (deliberate) failure marks payment FAILED and cancels the booking,
     * releasing the slot. Controlled testing: a card number ending in "0000"
     * fails, everything else succeeds.
     */
    PaymentResponse pay(String bookingId, PaymentRequest request);
}
