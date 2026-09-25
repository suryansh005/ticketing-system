package com.tothenew.ticket.api.dto;

import com.tothenew.ticket.infrastructure.persistence.TicketCommentEntity;
import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id, UUID ticketId, UUID authorId, String body, Instant createdAt) {

    public static CommentResponse from(TicketCommentEntity entity) {
        return new CommentResponse(
                entity.getId(),
                entity.getTicketId(),
                entity.getAuthorId(),
                entity.getBody(),
                entity.getCreatedAt());
    }
}
