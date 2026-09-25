package com.tothenew.ticket.domain;

/**
 * Persisted ticket lifecycle status. Values align with {@code ticket.status} check constraint.
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }
}
