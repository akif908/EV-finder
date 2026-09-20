package com.example.EV_finder_api.service;

import com.example.EV_finder_api.dto.AdminUserResponse;
import com.example.EV_finder_api.dto.BookingResponse;
import com.example.EV_finder_api.dto.PlatformOverview;
import com.example.EV_finder_api.entity.Role;

import java.util.List;

/** Admin: platform overview, user/role management, booking moderation. */
public interface AdminService {

    PlatformOverview overview();

    List<AdminUserResponse> allUsers(Role roleFilter);

    AdminUserResponse changeRole(String userId, Role newRole);

    void deleteUser(String userId);

    List<BookingResponse> allBookings();

    BookingResponse forceCancelBooking(String bookingId);
}
