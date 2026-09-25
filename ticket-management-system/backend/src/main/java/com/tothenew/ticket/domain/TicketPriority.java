package com.tothenew.ticket.domain;

/**
 * Persisted ticket priority. Values align with {@code ticket.priority} check constraint.
 */
public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}
