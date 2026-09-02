package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.Payment;
import com.example.EV_finder_api.entity.PaymentStatus;

import java.math.BigDecimal;

public record PaymentResponse(
        String paymentId,
        String bookingId,
        BigDecimal amount,
        String paymentMethod,
        PaymentStatus status,
        String transactionRef
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getBooking().getId(), p.getAmount(),
                p.getPaymentMethod().name(), p.getStatus(), p.getTransactionRef());
    }
}
