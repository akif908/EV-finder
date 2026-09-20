package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.AdminUserResponse;
import com.example.EV_finder_api.dto.BookingResponse;
import com.example.EV_finder_api.dto.PlatformOverview;
import com.example.EV_finder_api.entity.*;
import com.example.EV_finder_api.exception.ForbiddenException;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.*;
import com.example.EV_finder_api.security.CurrentUserProvider;
import com.example.EV_finder_api.service.AdminService;
import com.example.EV_finder_api.service.BookingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserProvider currentUserProvider;
    private final BookingService bookingService;

    public AdminServiceImpl(UserRepository userRepository,
                            StationRepository stationRepository,
                            BookingRepository bookingRepository,
                            PaymentRepository paymentRepository,
                            CurrentUserProvider currentUserProvider,
                            BookingService bookingService) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserProvider = currentUserProvider;
        this.bookingService = bookingService;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformOverview overview() {
        List<User> users = userRepository.findAll();
        List<Station> stations = stationRepository.findAll();
        double revenue = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .mapToDouble(p -> p.getAmount().doubleValue())
                .sum();
        return new PlatformOverview(
                users.size(),
                users.stream().filter(u -> u.getRole() == Role.OPERATOR).count(),
                stations.size(),
                stations.stream().filter(s -> s.getStatus() == StationStatus.ACTIVE).count(),
                bookingRepository.count(),
                revenue
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> allUsers(Role roleFilter) {
        return userRepository.findAll().stream()
                .filter(u -> roleFilter == null || u.getRole() == roleFilter)
                .map(u -> new AdminUserResponse(u.getId(), u.getName(), u.getEmail(),
                        u.getRole(), u.getCreatedAt()))
                .toList();
    }

    @Override
    public AdminUserResponse changeRole(String userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setRole(newRole);
        user = userRepository.save(user);
        return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getRole(), user.getCreatedAt());
    }

    @Override
    public void deleteUser(String userId) {
        User current = currentUserProvider.getCurrentUser();
        if (current.getId().equals(userId)) {
            throw new ForbiddenException("You cannot delete your own admin account");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (user.getRole() == Role.OPERATOR && stationRepository.existsByOperatorId(userId)) {
            throw new ForbiddenException(
                    "This operator still owns stations — remove or reassign them first");
        }
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> allBookings() {
        return bookingRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(b -> BookingResponse.from(b, amountOf(b)))
                .toList();
    }

    @Override
    public BookingResponse forceCancelBooking(String bookingId) {
        return bookingService.cancel(bookingId); // booking service allows admin cancellation
    }

    private java.math.BigDecimal amountOf(Booking b) {
        return paymentRepository.findByBookingId(b.getId())
                .map(Payment::getAmount)
                .orElse(b.getService().getPricePerUnit());
    }
}
