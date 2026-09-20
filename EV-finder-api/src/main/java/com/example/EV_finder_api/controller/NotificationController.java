package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.NotificationResponse;
import com.example.EV_finder_api.entity.Notification;
import com.example.EV_finder_api.exception.ForbiddenException;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.NotificationRepository;
import com.example.EV_finder_api.security.CurrentUserProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** In-app notifications — shared by USER, OPERATOR and ADMIN. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final CurrentUserProvider currentUserProvider;

    public NotificationController(NotificationRepository notificationRepository,
                                  CurrentUserProvider currentUserProvider) {
        this.notificationRepository = notificationRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/my")
    public List<NotificationResponse> myNotifications() {
        String userId = currentUserProvider.getCurrentUser().getId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::from).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        String userId = currentUserProvider.getCurrentUser().getId();
        return Map.of("count", notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable String id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        if (!n.getUser().getId().equals(currentUserProvider.getCurrentUser().getId())) {
            throw new ForbiddenException("Not your notification");
        }
        n.setRead(true);
        return NotificationResponse.from(notificationRepository.save(n));
    }

    @PutMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        String userId = currentUserProvider.getCurrentUser().getId();
        List<Notification> unread = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream().filter(n -> !n.isRead()).toList();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return Map.of("marked", unread.size());
    }
}
