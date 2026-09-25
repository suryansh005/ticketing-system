package com.tothenew.ticket.application;

import java.util.UUID;

public class InvalidAssigneeException extends RuntimeException {

    private final UUID assigneeId;

    public InvalidAssigneeException(UUID assigneeId) {
        super("Assignee not found: " + assigneeId);
        this.assigneeId = assigneeId;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }
}
