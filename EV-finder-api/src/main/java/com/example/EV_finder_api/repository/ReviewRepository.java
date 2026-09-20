package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findByStationIdOrderByCreatedAtDesc(String stationId);
    Optional<Review> findByBookingId(String bookingId);
    boolean existsByBookingId(String bookingId);
    long countByStationId(String stationId);
}
