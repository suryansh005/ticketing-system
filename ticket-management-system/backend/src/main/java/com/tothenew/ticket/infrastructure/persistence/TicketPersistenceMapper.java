package com.tothenew.ticket.infrastructure.persistence;

import com.tothenew.ticket.domain.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketPersistenceMapper {

    public Ticket toDomain(TicketEntity entity) {
        return new Ticket(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getRequesterId(),
                entity.getAssigneeId(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public TicketEntity toNewEntity(Ticket ticket) {
        return TicketEntity.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .requesterId(ticket.getRequesterId())
                .assigneeId(ticket.getAssigneeId())
                .build();
    }

    public void applyDomainToEntity(Ticket ticket, TicketEntity entity) {
        entity.setTitle(ticket.getTitle());
        entity.setDescription(ticket.getDescription());
        entity.setPriority(ticket.getPriority());
        entity.setAssigneeId(ticket.getAssigneeId());
        entity.setStatus(ticket.getStatus());
        entity.setVersion(ticket.getVersion());
        entity.setUpdatedAt(ticket.getUpdatedAt());
    }
}
