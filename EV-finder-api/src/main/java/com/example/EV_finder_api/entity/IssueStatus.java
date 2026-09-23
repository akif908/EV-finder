package com.example.EV_finder_api.entity;

/** Lifecycle of a user-filed issue report. */
public enum IssueStatus {
    /** Filed, not yet looked at. */
    OPEN,
    /** An admin is working on it. */
    IN_PROGRESS,
    /** Fixed / answered. */
    RESOLVED,
    /** Closed without action (duplicate, not reproducible, ...). */
    REJECTED
}
