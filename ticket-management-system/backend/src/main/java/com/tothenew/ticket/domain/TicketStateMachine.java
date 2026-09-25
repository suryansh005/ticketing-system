package com.tothenew.ticket.domain;

import java.time.Clock;
import java.time.Instant;

/**
 * Authoritative ticket lifecycle policy per {@code spec/state-machine.md}.
 */
public class TicketStateMachine {

    private final Clock clock;

    public TicketStateMachine(Clock clock) {
        this.clock = clock;
    }

    /**
     * Applies {@code event} to {@code ticket}, updating status, version, and {@code updatedAt} on success.
     *
     * @throws InvalidStateTransitionException when the (current status, event) pair is not allowed
     */
    public void applyTransition(Ticket ticket, TransitionEvent event) {
        TicketStatus from = ticket.getStatus();
        TicketStatus to = resolveTargetStatus(from, event);
        if (to == null) {
            throw new InvalidStateTransitionException(from, event);
        }
        ticket.applySuccessfulTransition(to, Instant.now(clock));
    }

    /**
     * Resolves the target status for a transition without mutating the ticket.
     *
     * @return target status, or {@code null} if the transition is illegal
     */
    public TicketStatus resolveTargetStatus(TicketStatus from, TransitionEvent event) {
        return switch (from) {
            case OPEN -> switch (event) {
                case START_PROGRESS -> TicketStatus.IN_PROGRESS;
                case CANCEL -> TicketStatus.CANCELLED;
                default -> null;
            };
            case IN_PROGRESS -> switch (event) {
                case RESOLVE -> TicketStatus.RESOLVED;
                case CANCEL -> TicketStatus.CANCELLED;
                default -> null;
            };
            case RESOLVED -> switch (event) {
                case CLOSE -> TicketStatus.CLOSED;
                default -> null;
            };
            case CLOSED, CANCELLED -> null;
        };
    }
}
