package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.IssueRequest;
import com.example.EV_finder_api.dto.IssueResponse;
import com.example.EV_finder_api.service.impl.IssueServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Issue / bug reporting — any signed-in user can file and track reports. */
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueServiceImpl issueService;

    public IssueController(IssueServiceImpl issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    public ResponseEntity<IssueResponse> create(@Valid @RequestBody IssueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.create(request));
    }

    /** The caller's own reports, so they can see progress. */
    @GetMapping("/my")
    public List<IssueResponse> mine() {
        return issueService.mine();
    }
}
