package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.ReviewRequest;
import com.example.EV_finder_api.dto.ReviewResponse;
import com.example.EV_finder_api.entity.BookingStatus;
import com.example.EV_finder_api.entity.Station;
import com.example.EV_finder_api.entity.StationReview;
import com.example.EV_finder_api.entity.User;
import com.example.EV_finder_api.exception.ForbiddenException;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.BookingRepository;
import com.example.EV_finder_api.repository.StationRepository;
import com.example.EV_finder_api.repository.StationReviewRepository;
import com.example.EV_finder_api.security.CurrentUserProvider;
import com.example.EV_finder_api.service.StationReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StationReviewServiceImpl implements StationReviewService {

    private final StationReviewRepository reviewRepository;
    private final StationRepository stationRepository;
    private final BookingRepository bookingRepository;
    private final CurrentUserProvider currentUserProvider;

    public StationReviewServiceImpl(StationReviewRepository reviewRepository,
                                    StationRepository stationRepository,
                                    BookingRepository bookingRepository,
                                    CurrentUserProvider currentUserProvider) {
        this.reviewRepository = reviewRepository;
        this.stationRepository = stationRepository;
        this.bookingRepository = bookingRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public ReviewResponse upsert(String stationId, ReviewRequest request) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + stationId));
        User user = currentUserProvider.getCurrentUser();

        boolean hasCompletedBooking = bookingRepository.existsByUserIdAndStationIdAndStatus(
                user.getId(), stationId, BookingStatus.COMPLETED);
        if (!hasCompletedBooking) {
            throw new ForbiddenException("You can review a station only after a completed booking there");
        }

        StationReview review = reviewRepository.findByStationIdAndUserId(stationId, user.getId())
                .orElseGet(() -> StationReview.builder()
                        .station(station)
                        .user(user)
                        .build());
        review.setRating(request.rating());
        review.setComment(request.comment());
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> list(String stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new ResourceNotFoundException("Station not found: " + stationId);
        }
        return reviewRepository.findByStationIdOrderByCreatedAtDesc(stationId)
                .stream().map(ReviewResponse::from).toList();
    }
}
