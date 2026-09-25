package com.tothenew.ticket.domain;

import java.time.Instant;
import java.util.UUID;

final class TicketFactory {

    private static final Instant DEFAULT_CREATED_AT = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant DEFAULT_UPDATED_AT = Instant.parse("2026-01-15T10:00:00Z");
    private static final UUID DEFAULT_REQUESTER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private TicketFactory() {}

    static Ticket open() {
        return withStatus(TicketStatus.OPEN);
    }

    static Ticket withStatus(TicketStatus status) {
        return new Ticket(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Test ticket",
                "Description",
                TicketPriority.MEDIUM,
                status,
                DEFAULT_REQUESTER,
                null,
                0L,
                DEFAULT_CREATED_AT,
                DEFAULT_UPDATED_AT);
    }
}
