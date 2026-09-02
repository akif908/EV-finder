package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.PaymentRequest;
import com.example.EV_finder_api.dto.PaymentResponse;
import com.example.EV_finder_api.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Simulated payment: books and confirms on SUCCESS, cancels booking on FAILED. */
    @PostMapping("/{bookingId}")
    public PaymentResponse pay(@PathVariable String bookingId, @Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(bookingId, request);
    }
}
