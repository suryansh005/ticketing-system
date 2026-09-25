package com.tothenew.ticket.infrastructure.persistence;

import com.tothenew.ticket.domain.TicketPriority;
import com.tothenew.ticket.domain.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

@Entity
@Table(
        name = "ticket",
        indexes = {
            @Index(name = "idx_ticket_status", columnList = "status"),
            @Index(name = "idx_ticket_created_at", columnList = "created_at"),
            @Index(name = "idx_ticket_requester", columnList = "requester_id"),
            @Index(name = "idx_ticket_assignee", columnList = "assignee_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TicketEntity implements Persistable<UUID> {

    @Transient
    @Builder.Default
    private boolean newRow = true;

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean isNew() {
        return newRow;
    }

    @PostPersist
    @PostLoad
    void markPersisted() {
        this.newRow = false;
    }
}
