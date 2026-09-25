package com.tothenew.ticket.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tothenew.ticket.domain.TicketPriority;
import com.tothenew.ticket.domain.TicketStateMachine;
import com.tothenew.ticket.infrastructure.persistence.TicketPersistenceMapper;
import com.tothenew.ticket.infrastructure.persistence.TicketRepository;
import com.tothenew.ticket.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketServiceAssigneeTest {

    private static final UUID UNKNOWN_ASSIGNEE =
            UUID.fromString("99999999-9999-4999-8999-999999999999");

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketPersistenceMapper mapper;

    @Mock
    private TicketStateMachine stateMachine;

    @Mock
    private RequestContext requestContext;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-24T10:00:00Z"), ZoneOffset.UTC);
        ticketService =
                new TicketService(
                        ticketRepository,
                        userRepository,
                        mapper,
                        stateMachine,
                        requestContext,
                        clock);
    }

    @Test
    void createRejectsUnknownAssignee() {
        when(userRepository.existsById(UNKNOWN_ASSIGNEE)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                ticketService.create(
                                        "Title",
                                        "Description",
                                        TicketPriority.LOW,
                                        UNKNOWN_ASSIGNEE))
                .isInstanceOf(InvalidAssigneeException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void createAllowsNullAssignee() {
        when(requestContext.currentUserId())
                .thenReturn(UUID.fromString("00000000-0000-4000-8000-000000000001"));

        ticketService.create("Title", "Description", TicketPriority.LOW, null);

        verify(userRepository, never()).existsById(any());
    }
}
