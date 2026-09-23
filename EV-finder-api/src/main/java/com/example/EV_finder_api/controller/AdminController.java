package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.AdminUserResponse;
import com.example.EV_finder_api.dto.BookingResponse;
import com.example.EV_finder_api.dto.IssueResponse;
import com.example.EV_finder_api.dto.IssueUpdateRequest;
import com.example.EV_finder_api.dto.PlatformOverview;
import com.example.EV_finder_api.entity.Role;
import com.example.EV_finder_api.service.AdminService;
import com.example.EV_finder_api.service.impl.IssueServiceImpl;
import jakarta.validation.Valid;
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
    private final IssueServiceImpl issueService;

    public AdminController(AdminService adminService, IssueServiceImpl issueService) {
        this.adminService = adminService;
        this.issueService = issueService;
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

    // ---- user-filed issue reports ----

    @GetMapping("/issues")
    public List<IssueResponse> issues() {
        return issueService.all();
    }

    @PutMapping("/issues/{id}")
    public IssueResponse updateIssue(@PathVariable String id,
                                     @Valid @RequestBody IssueUpdateRequest request) {
        return issueService.updateStatus(id, request);
    }
}
