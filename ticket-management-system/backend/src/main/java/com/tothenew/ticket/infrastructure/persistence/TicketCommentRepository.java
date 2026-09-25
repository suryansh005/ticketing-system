package com.tothenew.ticket.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketCommentEntity, UUID> {

    Page<TicketCommentEntity> findByTicketId(UUID ticketId, Pageable pageable);
}
