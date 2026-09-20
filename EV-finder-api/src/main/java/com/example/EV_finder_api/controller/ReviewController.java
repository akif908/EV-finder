package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.ReviewRequest;
import com.example.EV_finder_api.dto.StationReviewsResponse;
import com.example.EV_finder_api.entity.Booking;
import com.example.EV_finder_api.entity.BookingStatus;
import com.example.EV_finder_api.entity.Notification;
import com.example.EV_finder_api.entity.Review;
import com.example.EV_finder_api.exception.*;
import com.example.EV_finder_api.repository.BookingRepository;
import com.example.EV_finder_api.repository.ReviewRepository;
import com.example.EV_finder_api.security.CurrentUserProvider;
import com.example.EV_finder_api.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Reviews & ratings (context §19) — one review per booking. */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationService notificationService;

    public ReviewController(ReviewRepository reviewRepository,
                            BookingRepository bookingRepository,
                            CurrentUserProvider currentUserProvider,
                            NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.currentUserProvider = currentUserProvider;
        this.notificationService = notificationService;
    }

    /** Rate a station after the booked session has taken place. */
    @PostMapping
    @Transactional
    public ResponseEntity<StationReviewsResponse.Item> create(@Valid @RequestBody ReviewRequest request) {
        var user = currentUserProvider.getCurrentUser();
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + request.bookingId()));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only review your own bookings");
        }
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new DuplicateResourceException("You already reviewed this booking");
        }
        boolean used = booking.getStatus() == BookingStatus.COMPLETED
                || (booking.getStatus() == BookingStatus.CONFIRMED
                    && booking.getEndTime().isBefore(LocalDateTime.now()));
        if (!used) {
            throw new ValidationException("You can review after the booked time has passed");
        }

        Review review = reviewRepository.save(Review.builder()
                .booking(booking)
                .user(user)
                .station(booking.getStation())
                .rating(request.rating())
                .comment(request.comment())
                .build());

        // the operator hears about it (and about a low score especially)
        notificationService.notify(
                booking.getStation().getOperator().getId(),
                request.rating() >= 4 ? "New positive review" : "New review received",
                user.getName() + " rated " + booking.getStation().getName()
                        + " " + request.rating() + "/5"
                        + (request.comment() != null ? ": \"" + request.comment() + "\"" : "."),
                Notification.NotificationType.NEW_REVIEW);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StationReviewsResponse.Item.from(review));
    }

    /** Average + list for a station (any logged-in user). */
    @GetMapping("/station/{stationId}")
    @Transactional(readOnly = true)
    public StationReviewsResponse stationReviews(@PathVariable String stationId) {
        List<Review> reviews = reviewRepository.findByStationIdOrderByCreatedAtDesc(stationId);
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return new StationReviewsResponse(
                Math.round(avg * 10) / 10.0,
                reviews.size(),
                reviews.stream().map(StationReviewsResponse.Item::from).toList()
        );
    }
}
