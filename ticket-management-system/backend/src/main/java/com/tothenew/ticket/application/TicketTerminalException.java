package com.tothenew.ticket.application;

import com.tothenew.ticket.domain.TicketStatus;
import java.util.UUID;

public class TicketTerminalException extends RuntimeException {

    private final UUID ticketId;
    private final TicketStatus status;

    public TicketTerminalException(UUID ticketId, TicketStatus status) {
        super("Ticket is terminal: " + ticketId + ", status=" + status);
        this.ticketId = ticketId;
        this.status = status;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public TicketStatus getStatus() {
        return status;
    }
}
