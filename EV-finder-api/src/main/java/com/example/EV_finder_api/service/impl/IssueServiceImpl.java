package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.IssueRequest;
import com.example.EV_finder_api.dto.IssueResponse;
import com.example.EV_finder_api.dto.IssueUpdateRequest;
import com.example.EV_finder_api.entity.*;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.exception.ValidationException;
import com.example.EV_finder_api.repository.IssueRepository;
import com.example.EV_finder_api.repository.StationRepository;
import com.example.EV_finder_api.repository.UserRepository;
import com.example.EV_finder_api.security.CurrentUserProvider;
import com.example.EV_finder_api.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bug / issue reporting. Users file reports from the app and every ADMIN is
 * notified; admins triage them and the reporter is notified back with the
 * resolution note.
 */
@Service
@Transactional
public class IssueServiceImpl {

    private final IssueRepository issueRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationService notificationService;

    public IssueServiceImpl(IssueRepository issueRepository,
                            StationRepository stationRepository,
                            UserRepository userRepository,
                            CurrentUserProvider currentUserProvider,
                            NotificationService notificationService) {
        this.issueRepository = issueRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.notificationService = notificationService;
    }

    /** Files a report and alerts every admin. */
    public IssueResponse create(IssueRequest request) {
        User reporter = currentUserProvider.getCurrentUser();
        Station station = null;
        if (request.stationId() != null && !request.stationId().isBlank()) {
            station = stationRepository.findById(request.stationId()).orElse(null);
        }

        Issue issue = Issue.builder()
                .user(reporter)
                .station(station)
                .category(request.category())
                .subject(request.subject())
                .description(request.description())
                .status(IssueStatus.OPEN)
                .build();
        issueRepository.save(issue);

        String where = station != null ? " (" + station.getName() + ")" : "";
        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            notificationService.notify(
                    admin.getId(),
                    "New issue reported",
                    reporter.getName() + " reported \"" + request.subject() + "\"" + where + ".",
                    Notification.NotificationType.NEW_ISSUE);
        }
        return IssueResponse.from(issue);
    }

    /** The reporter's own reports. */
    @Transactional(readOnly = true)
    public List<IssueResponse> mine() {
        return issueRepository.findByUserIdOrderByCreatedAtDesc(
                currentUserProvider.getCurrentUser().getId())
                .stream().map(IssueResponse::from).toList();
    }

    /** Admin inbox: every report, newest first. */
    @Transactional(readOnly = true)
    public List<IssueResponse> all() {
        return issueRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(IssueResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long openCount() {
        return issueRepository.countByStatus(IssueStatus.OPEN);
    }

    /** Admin triage: move the report along and tell the reporter. */
    public IssueResponse updateStatus(String issueId, IssueUpdateRequest request) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

        IssueStatus status;
        try {
            status = IssueStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException("Unknown issue status: " + request.status());
        }

        issue.setStatus(status);
        if (request.resolutionNote() != null && !request.resolutionNote().isBlank()) {
            issue.setResolutionNote(request.resolutionNote());
        }
        if (status == IssueStatus.RESOLVED || status == IssueStatus.REJECTED) {
            issue.setResolvedAt(LocalDateTime.now());
        }
        issueRepository.save(issue);

        // Close the loop with the person who reported it.
        String note = issue.getResolutionNote() != null
                ? " — " + issue.getResolutionNote() : "";
        notificationService.notify(
                issue.getUser().getId(),
                "Issue " + status.name().toLowerCase().replace('_', ' '),
                "Your report \"" + issue.getSubject() + "\" is now " + status.name() + note,
                status == IssueStatus.REJECTED
                        ? Notification.NotificationType.GENERAL
                        : Notification.NotificationType.ISSUE_RESOLVED);

        return IssueResponse.from(issue);
    }
}
