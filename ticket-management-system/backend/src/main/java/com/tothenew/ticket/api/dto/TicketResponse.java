package com.tothenew.ticket.api.dto;

import com.tothenew.ticket.domain.Ticket;
import com.tothenew.ticket.domain.TicketPriority;
import com.tothenew.ticket.domain.TicketStatus;
import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
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

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getRequesterId(),
                ticket.getAssigneeId(),
                ticket.getVersion(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
