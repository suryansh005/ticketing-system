package com.tothenew.ticket.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository
        extends JpaRepository<TicketEntity, UUID>, JpaSpecificationExecutor<TicketEntity> {}
