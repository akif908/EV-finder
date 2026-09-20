package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.BookingResponse;
import com.example.EV_finder_api.service.BookingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Operator dashboard: bookings at stations this operator owns. */
@RestController
@RequestMapping("/api/operator/bookings")
@PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
public class OperatorBookingController {

    private final BookingService bookingService;

    public OperatorBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<BookingResponse> stationBookings() {
        return bookingService.bookingsForMyStations();
    }
}
