package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.StationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StationReviewRepository extends JpaRepository<StationReview, String> {

    Optional<StationReview> findByStationIdAndUserId(String stationId, String userId);

    List<StationReview> findByStationIdOrderByCreatedAtDesc(String stationId);

    long countByStationId(String stationId);

    /**
     * One row per station: [stationId (String), avgRating (Double), reviewCount (Long)].
     * Used to enrich station list/detail responses.
     */
    @Query("SELECT r.station.id, AVG(r.rating), COUNT(r) FROM StationReview r GROUP BY r.station.id")
    List<Object[]> aggregateByStation();
}
