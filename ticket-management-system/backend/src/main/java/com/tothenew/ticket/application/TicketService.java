package com.tothenew.ticket.application;

import com.tothenew.ticket.domain.Ticket;
import com.tothenew.ticket.domain.TicketPriority;
import com.tothenew.ticket.domain.TicketStateMachine;
import com.tothenew.ticket.domain.TicketStatus;
import com.tothenew.ticket.domain.TransitionEvent;
import com.tothenew.ticket.infrastructure.persistence.TicketEntity;
import com.tothenew.ticket.infrastructure.persistence.TicketPersistenceMapper;
import com.tothenew.ticket.infrastructure.persistence.TicketRepository;
import com.tothenew.ticket.infrastructure.persistence.TicketSpecification;
import com.tothenew.ticket.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketPersistenceMapper mapper;
    private final TicketStateMachine stateMachine;
    private final RequestContext requestContext;
    private final Clock clock;

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketPersistenceMapper mapper,
            TicketStateMachine stateMachine,
            RequestContext requestContext,
            Clock clock) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.stateMachine = stateMachine;
        this.requestContext = requestContext;
        this.clock = clock;
    }

    @Transactional
    public Ticket create(String title, String description, TicketPriority priority, UUID assigneeId) {
        validateAssignee(assigneeId);
        Instant now = Instant.now(clock);
        UUID id = UUID.randomUUID();
        UUID requesterId = requestContext.currentUserId();
        Ticket ticket = new Ticket(
                id,
                title,
                description,
                priority,
                TicketStatus.OPEN,
                requesterId,
                assigneeId,
                0L,
                now,
                now);
        TicketEntity saved = ticketRepository.save(mapper.toNewEntity(ticket));
        return mapper.toDomain(saved);
    }

    @Transactional(readOnly = true)
    public Ticket getById(UUID ticketId) {
        return ticketRepository
                .findById(ticketId)
                .map(mapper::toDomain)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    @Transactional(readOnly = true)
    public Page<Ticket> list(Pageable pageable, TicketStatus status, String search) {
        Specification<TicketEntity> spec =
                Specification.where(TicketSpecification.hasStatus(status))
                        .and(TicketSpecification.matchesSearch(search));
        return ticketRepository.findAll(spec, pageable).map(mapper::toDomain);
    }

    @Transactional
    public Ticket applyTransition(UUID ticketId, TransitionEvent event) {
        TicketEntity entity =
                ticketRepository
                        .findById(ticketId)
                        .orElseThrow(() -> new TicketNotFoundException(ticketId));
        Ticket ticket = mapper.toDomain(entity);
        stateMachine.applyTransition(ticket, event);
        mapper.applyDomainToEntity(ticket, entity);
        return mapper.toDomain(entity);
    }

    @Transactional
    public Ticket update(
            UUID ticketId,
            String title,
            String description,
            TicketPriority priority,
            UUID assigneeId) {
        TicketEntity entity =
                ticketRepository
                        .findById(ticketId)
                        .orElseThrow(() -> new TicketNotFoundException(ticketId));
        Ticket existing = mapper.toDomain(entity);
        if (existing.getStatus().isTerminal()) {
            throw new TicketTerminalException(ticketId, existing.getStatus());
        }
        validateAssignee(assigneeId);
        Instant now = Instant.now(clock);
        Ticket updated =
                new Ticket(
                        existing.getId(),
                        title,
                        description,
                        priority,
                        existing.getStatus(),
                        existing.getRequesterId(),
                        assigneeId,
                        existing.getVersion() + 1,
                        existing.getCreatedAt(),
                        now);
        mapper.applyDomainToEntity(updated, entity);
        return mapper.toDomain(entity);
    }

    private void validateAssignee(UUID assigneeId) {
        if (assigneeId != null && !userRepository.existsById(assigneeId)) {
            throw new InvalidAssigneeException(assigneeId);
        }
    }
}
