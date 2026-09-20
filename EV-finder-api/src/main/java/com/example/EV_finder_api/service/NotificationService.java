package com.example.EV_finder_api.service;

import com.example.EV_finder_api.entity.Notification;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.NotificationRepository;
import com.example.EV_finder_api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates in-app notifications (context §18) at the moments that matter.
 *
 * <p>Notifications are a side effect, never a business rule: a failure here
 * must not roll back the booking/payment that triggered it, so the insert runs
 * in its own transaction and any error is logged instead of rethrown.</p>
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(String userId, String title, String message, Notification.NotificationType type) {
        try {
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            notificationRepository.save(Notification.builder()
                    .user(user)
                    .title(title)
                    .message(message)
                    .type(type)
                    .build());
        } catch (Exception e) {
            // never let a notification break the core flow
            log.warn("Could not create notification for user {}: {}", userId, e.getMessage());
        }
    }
}
