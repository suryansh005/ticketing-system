package com.tothenew.ticket.application;

import com.tothenew.ticket.api.dto.CommentResponse;
import com.tothenew.ticket.infrastructure.persistence.TicketCommentEntity;
import com.tothenew.ticket.infrastructure.persistence.TicketCommentRepository;
import com.tothenew.ticket.infrastructure.persistence.TicketRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final RequestContext requestContext;

    public CommentService(
            TicketRepository ticketRepository,
            TicketCommentRepository commentRepository,
            RequestContext requestContext) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.requestContext = requestContext;
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> list(UUID ticketId, Pageable pageable) {
        ensureTicketExists(ticketId);
        return commentRepository.findByTicketId(ticketId, pageable).map(CommentResponse::from);
    }

    @Transactional
    public CommentResponse add(UUID ticketId, String body) {
        ensureTicketExists(ticketId);
        TicketCommentEntity entity =
                TicketCommentEntity.builder()
                        .id(UUID.randomUUID())
                        .ticketId(ticketId)
                        .authorId(requestContext.currentUserId())
                        .body(body)
                        .build();
        return CommentResponse.from(commentRepository.save(entity));
    }

    private void ensureTicketExists(UUID ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }
    }
}
