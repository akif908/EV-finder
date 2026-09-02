package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.PaymentRequest;
import com.example.EV_finder_api.dto.PaymentResponse;
import com.example.EV_finder_api.entity.*;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.exception.ValidationException;
import com.example.EV_finder_api.repository.PaymentRepository;
import com.example.EV_finder_api.security.CurrentUserProvider;
import com.example.EV_finder_api.service.BookingService;
import com.example.EV_finder_api.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingServiceImpl bookingServiceImpl;
    private final CurrentUserProvider currentUserProvider;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              BookingServiceImpl bookingServiceImpl,
                              CurrentUserProvider currentUserProvider) {
        this.paymentRepository = paymentRepository;
        this.bookingServiceImpl = bookingServiceImpl;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public PaymentResponse pay(String bookingId, PaymentRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Booking booking = bookingServiceImpl.bookingForPayment(bookingId, user.getId());

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new ValidationException("Booking is already paid and confirmed");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ValidationException("Only pending bookings can be paid (status: "
                    + booking.getStatus() + ")");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId).orElseGet(() ->
                Payment.builder()
                        .booking(booking)
                        .amount(booking.getService().getPricePerUnit())
                        .paymentMethod(request.paymentMethod())
                        .status(PaymentStatus.PENDING)
                        .build());

        // ---- simulated gateway: no real money ----
        boolean success = !(Boolean.TRUE.equals(request.forceFailure()));
        payment.setTransactionRef("SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setPaymentMethod(request.paymentMethod());
        payment = paymentRepository.save(payment);

        booking.setStatus(success ? BookingStatus.CONFIRMED : BookingStatus.CANCELLED);
        return PaymentResponse.from(payment);
    }
}
