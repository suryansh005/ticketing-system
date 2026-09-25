package com.tothenew.ticket.domain;

/**
 * Thrown when a ticket transition is not allowed by the lifecycle state machine.
 */
public class InvalidStateTransitionException extends RuntimeException {

    private final TicketStatus fromStatus;
    private final TransitionEvent event;

    public InvalidStateTransitionException(TicketStatus fromStatus, TransitionEvent event) {
        super(
                String.format(
                        "Illegal transition: status=%s, event=%s", fromStatus, event));
        this.fromStatus = fromStatus;
        this.event = event;
    }

    public TicketStatus getFromStatus() {
        return fromStatus;
    }

    public TransitionEvent getEvent() {
        return event;
    }
}
