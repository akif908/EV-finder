package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.AdminUserResponse;
import com.example.EV_finder_api.dto.BookingResponse;
import com.example.EV_finder_api.dto.PlatformOverview;
import com.example.EV_finder_api.entity.Role;
import com.example.EV_finder_api.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin platform management. Every endpoint requires the ADMIN role. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public PlatformOverview overview() {
        return adminService.overview();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(@RequestParam(required = false) Role role) {
        return adminService.allUsers(role);
    }

    @PutMapping("/users/{id}/role")
    public AdminUserResponse changeRole(@PathVariable String id, @RequestParam Role role) {
        return adminService.changeRole(id, role);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bookings")
    public List<BookingResponse> bookings() {
        return adminService.allBookings();
    }

    @PutMapping("/bookings/{id}/cancel")
    public BookingResponse forceCancel(@PathVariable String id) {
        return adminService.forceCancelBooking(id);
    }
}
