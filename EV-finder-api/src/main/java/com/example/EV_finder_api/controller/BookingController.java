package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.BookingRequest;
import com.example.EV_finder_api.dto.BookingResponse;
import com.example.EV_finder_api.entity.BookingStatus;
import com.example.EV_finder_api.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request));
    }

    @GetMapping("/my")
    public List<BookingResponse> myBookings(@RequestParam(required = false) BookingStatus status) {
        return status != null ? bookingService.myBookingsByStatus(status)
                : bookingService.myBookings();
    }

    @GetMapping("/{id}")
    public BookingResponse details(@PathVariable String id) {
        return bookingService.details(id);
    }

    @PutMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable String id) {
        return bookingService.cancel(id);
    }
}
