package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.SlotResponse;
import com.example.EV_finder_api.entity.Booking;
import com.example.EV_finder_api.entity.BookingStatus;
import com.example.EV_finder_api.entity.Station;
import com.example.EV_finder_api.entity.StationService;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.BookingRepository;
import com.example.EV_finder_api.repository.StationServiceRepository;
import com.example.EV_finder_api.service.AvailabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final int SLOT_MINUTES = 60;

    private final StationServiceRepository stationServiceRepository;
    private final BookingRepository bookingRepository;

    public AvailabilityServiceImpl(StationServiceRepository stationServiceRepository,
                                   BookingRepository bookingRepository) {
        this.stationServiceRepository = stationServiceRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public List<SlotResponse> slotsFor(String serviceId, LocalDate date) {
        StationService service = stationServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
        Station station = service.getStation();
        if (service.getStatus() != com.example.EV_finder_api.entity.ServiceStatus.ACTIVE
                || station.getStatus() != com.example.EV_finder_api.entity.StationStatus.ACTIVE) {
            return List.of();
        }

        LocalTime open = station.getOpeningTime() != null ? station.getOpeningTime() : LocalTime.MIDNIGHT;
        LocalTime close = station.getClosingTime() != null ? station.getClosingTime() : LocalTime.of(23, 59);

        // Active bookings overlapping this date (day window)
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<Booking> active = bookingRepository.findByServiceIdAndStatusInAndStartTimeBetween(
                serviceId, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED), dayStart, dayEnd);

        List<SlotResponse> slots = new ArrayList<>();
        LocalDateTime cursor = date.atTime(open);
        LocalDateTime dayClose = date.atTime(close);
        while (cursor.plusMinutes(SLOT_MINUTES).isBefore(dayClose)
                || cursor.plusMinutes(SLOT_MINUTES).equals(dayClose)) {
            LocalDateTime slotStart = cursor;
            LocalDateTime slotEnd = cursor.plusMinutes(SLOT_MINUTES);
            long used = active.stream()
                    .filter(b -> b.getStartTime().isBefore(slotEnd) && b.getEndTime().isAfter(slotStart))
                    .count();
            int capacity = service.getAvailableSlots();
            int available = (int) Math.max(0, capacity - used);
            boolean future = slotStart.isAfter(LocalDateTime.now());
            slots.add(new SlotResponse(slotStart.toString(), slotEnd.toString(),
                    capacity, available, available > 0 && future));
            cursor = slotEnd;
        }
        return slots;
    }
}
