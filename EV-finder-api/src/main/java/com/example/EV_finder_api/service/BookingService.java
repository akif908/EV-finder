package com.example.EV_finder_api.service;

import com.example.EV_finder_api.dto.BookingRequest;
import com.example.EV_finder_api.dto.BookingResponse;
import com.example.EV_finder_api.entity.BookingStatus;

import java.util.List;

/** Booking lifecycle. The backend is the source of truth for availability. */
public interface BookingService {

    BookingResponse create(BookingRequest request);

    List<BookingResponse> myBookings();

    List<BookingResponse> myBookingsByStatus(BookingStatus status);

    BookingResponse details(String bookingId);

    BookingResponse cancel(String bookingId);
}
