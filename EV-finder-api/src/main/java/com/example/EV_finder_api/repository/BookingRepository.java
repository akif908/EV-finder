package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findByUserIdOrderByStartTimeDesc(String userId);

    /** All bookings at stations owned by the given operator. */
    @Query("SELECT b FROM Booking b WHERE b.station.operator.id = :operatorId ORDER BY b.startTime DESC")
    List<Booking> findByStationOperatorId(@Param("operatorId") String operatorId);

    /**
     * Double-booking prevention: counts active bookings whose [start,end)
     * window overlaps the requested window. Supported by index
     * idx_bookings_conflict (service_id, status, start_time, end_time).
     */
    @Query("""
           SELECT COUNT(b) FROM Booking b
           WHERE b.service.id = :serviceId
             AND b.status IN ('PENDING', 'CONFIRMED')
             AND b.startTime < :end
             AND b.endTime > :start
           """)
    long countActiveOverlapping(@Param("serviceId") String serviceId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    /**
     * Live occupancy: one row [serviceId, activeBookingsHappeningNow] for every
     * service that currently has at least one PENDING/CONFIRMED booking whose
     * window covers the given instant. Powers the station cards' availability
     * bar without N+1 queries.
     */
    @Query("""
           SELECT b.service.id, COUNT(b) FROM Booking b
           WHERE b.status IN ('PENDING', 'CONFIRMED')
             AND b.startTime < :now
             AND b.endTime > :now
           GROUP BY b.service.id
           """)
    List<Object[]> countActiveNowGrouped(@Param("now") LocalDateTime now);

    List<Booking> findByServiceIdAndStatusInAndStartTimeBetween(String serviceId,
                                                                List<com.example.EV_finder_api.entity.BookingStatus> statuses,
                                                                LocalDateTime start,
                                                                LocalDateTime end);
}
