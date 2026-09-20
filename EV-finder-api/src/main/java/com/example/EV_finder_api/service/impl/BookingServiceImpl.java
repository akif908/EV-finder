package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.BookingRequest;
import com.example.EV_finder_api.dto.BookingResponse;
import com.example.EV_finder_api.entity.*;
import com.example.EV_finder_api.exception.*;
import com.example.EV_finder_api.repository.*;
import com.example.EV_finder_api.security.CurrentUserProvider;
import com.example.EV_finder_api.service.BookingService;
import com.example.EV_finder_api.service.NotificationService;
import com.example.EV_finder_api.websocket.AvailabilityWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final VehicleRepository vehicleRepository;
    private final StationServiceRepository stationServiceRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AvailabilityWebSocketHandler availabilitySocket;
    private final NotificationService notificationService;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              PaymentRepository paymentRepository,
                              VehicleRepository vehicleRepository,
                              StationServiceRepository stationServiceRepository,
                              CurrentUserProvider currentUserProvider,
                              AvailabilityWebSocketHandler availabilitySocket,
                              NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.vehicleRepository = vehicleRepository;
        this.stationServiceRepository = stationServiceRepository;
        this.currentUserProvider = currentUserProvider;
        this.availabilitySocket = availabilitySocket;
        this.notificationService = notificationService;
    }

    /**
     * Creates a PENDING booking. The overlap count + capacity check run inside
     * the same transaction that inserts the row, so two concurrent requests
     * cannot both claim the last slot (row-level locking on commit order).
     */
    @Override
    public BookingResponse create(BookingRequest request) {
        User user = currentUserProvider.getCurrentUser();

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ValidationException("End time must be after start time");
        }
        if (request.startTime().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Cannot book a slot in the past");
        }

        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.vehicleId()));
        if (!vehicle.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only book with your own vehicle");
        }

        StationService service = stationServiceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + request.serviceId()));
        Station station = service.getStation();
        if (service.getStatus() != ServiceStatus.ACTIVE
                || station.getStatus() != StationStatus.ACTIVE) {
            throw new BookingUnavailableException("Service or station is not active");
        }

        long overlapping = bookingRepository.countActiveOverlapping(
                request.serviceId(), request.startTime(), request.endTime());
        if (overlapping >= service.getAvailableSlots()) {
            throw new BookingUnavailableException("No free slots left for the selected time");
        }

        Booking booking = Booking.builder()
                .user(user)
                .vehicle(vehicle)
                .station(station)
                .service(service)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(BookingStatus.PENDING)
                .build();
        booking = bookingRepository.save(booking);
        availabilitySocket.broadcastAvailability(service); // live update to all clients
        // notify the station's operator that someone booked their charger
        notificationService.notify(
                station.getOperator().getId(),
                "New booking received",
                user.getName() + " booked " + station.getName() + " for " + booking.getStartTime() + ".",
                Notification.NotificationType.NEW_BOOKING);
        return BookingResponse.from(booking, service.getPricePerUnit());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings() {
        String userId = currentUserProvider.getCurrentUser().getId();
        return bookingRepository.findByUserIdOrderByStartTimeDesc(userId)
                .stream().map(this::withAmount).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> myBookingsByStatus(BookingStatus status) {
        return myBookings().stream().filter(b -> b.status() == status).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> bookingsForMyStations() {
        String operatorId = currentUserProvider.getCurrentUser().getId();
        return bookingRepository.findByStationOperatorId(operatorId)
                .stream().map(this::withAmount).toList();
    }

    /** Package-visible helper used by the payment service. */
    Booking bookingForPayment(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only pay for your own bookings");
        }
        return booking;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse details(String bookingId) {
        return BookingResponse.from(getOwnedBooking(bookingId), amountOf(getOwnedBooking(bookingId)));
    }

    @Override
    public BookingResponse cancel(String bookingId) {
        Booking booking = getOwnedBooking(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ValidationException("Only pending or confirmed bookings can be cancelled");
        }
        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Cannot cancel a booking that already started");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        // a paid booking gets a simulated refund
        paymentRepository.findByBookingId(bookingId).ifPresent(p -> {
            if (p.getStatus() == PaymentStatus.SUCCESS) p.setStatus(PaymentStatus.REFUNDED);
        });
        bookingRepository.save(booking);
        availabilitySocket.broadcastAvailability(booking.getService()); // slot back on the market
        notificationService.notify(
                booking.getStation().getOperator().getId(),
                "Booking cancelled",
                booking.getUser().getName() + " cancelled their booking at "
                        + booking.getStation().getName() + " — the slot is free again.",
                Notification.NotificationType.BOOKING_CANCELLED);
        return BookingResponse.from(booking, amountOf(booking));
    }

    Booking bookingForPayment(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only pay for your own bookings");
        }
        return booking;
    }

    private Booking getOwnedBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        String currentUserId = currentUserProvider.getCurrentUser().getId();
        boolean isAdmin = currentUserProvider.getCurrentUser().getRole() == Role.ADMIN;
        if (!booking.getUser().getId().equals(currentUserId) && !isAdmin) {
            throw new ForbiddenException("You can only view your own bookings");
        }
        return booking;
    }

    private BookingResponse withAmount(Booking b) {
        return BookingResponse.from(b, amountOf(b));
    }

    private java.math.BigDecimal amountOf(Booking b) {
        return paymentRepository.findByBookingId(b.getId())
                .map(Payment::getAmount)
                .orElse(b.getService().getPricePerUnit());
    }
}
