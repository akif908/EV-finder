package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findByUserIdOrderByStartTimeDesc(String userId);

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

    List<Booking> findByServiceIdAndStatusInAndStartTimeBetween(String serviceId,
                                                                List<com.example.EV_finder_api.entity.BookingStatus> statuses,
                                                                LocalDateTime start,
                                                                LocalDateTime end);
}
