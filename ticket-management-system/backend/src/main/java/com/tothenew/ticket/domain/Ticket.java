package com.tothenew.ticket.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Ticket aggregate root. Lifecycle changes go through {@link TicketStateMachine} only.
 */
public class Ticket {

    private final UUID id;
    private final String title;
    private final String description;
    private final TicketPriority priority;
    private final UUID requesterId;
    private final UUID assigneeId;
    private TicketStatus status;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public Ticket(
            UUID id,
            String title,
            String description,
            TicketPriority priority,
            TicketStatus status,
            UUID requesterId,
            UUID assigneeId,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.requesterId = requesterId;
        this.assigneeId = assigneeId;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    void applySuccessfulTransition(TicketStatus newStatus, Instant updatedAt) {
        this.status = newStatus;
        this.version++;
        this.updatedAt = updatedAt;
    }
}
