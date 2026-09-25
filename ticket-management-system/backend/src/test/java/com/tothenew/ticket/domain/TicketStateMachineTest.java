package com.tothenew.ticket.domain;

import static com.tothenew.ticket.domain.TicketStatus.CANCELLED;
import static com.tothenew.ticket.domain.TicketStatus.CLOSED;
import static com.tothenew.ticket.domain.TicketStatus.IN_PROGRESS;
import static com.tothenew.ticket.domain.TicketStatus.OPEN;
import static com.tothenew.ticket.domain.TicketStatus.RESOLVED;
import static com.tothenew.ticket.domain.TransitionEvent.CANCEL;
import static com.tothenew.ticket.domain.TransitionEvent.CLOSE;
import static com.tothenew.ticket.domain.TransitionEvent.RESOLVE;
import static com.tothenew.ticket.domain.TransitionEvent.START_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Ticket state machine")
class TicketStateMachineTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-06-01T12:00:00Z");

    private TicketStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new TicketStateMachine(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    record TransitionCase(TicketStatus from, TransitionEvent event, TicketStatus to) {}

    static Stream<TransitionCase> allowedTransitions() {
        return Stream.of(
                new TransitionCase(OPEN, START_PROGRESS, IN_PROGRESS),
                new TransitionCase(OPEN, CANCEL, CANCELLED),
                new TransitionCase(IN_PROGRESS, RESOLVE, RESOLVED),
                new TransitionCase(IN_PROGRESS, CANCEL, CANCELLED),
                new TransitionCase(RESOLVED, CLOSE, CLOSED));
    }

    static Stream<Arguments> forbiddenTransitions() {
        Set<String> allowedKeys =
                allowedTransitions()
                        .map(c -> key(c.from, c.event))
                        .collect(java.util.stream.Collectors.toSet());

        return Arrays.stream(TicketStatus.values())
                .flatMap(
                        from ->
                                Arrays.stream(TransitionEvent.values())
                                        .filter(event -> !allowedKeys.contains(key(from, event)))
                                        .map(event -> Arguments.of(from, event)));
    }

    private static String key(TicketStatus from, TransitionEvent event) {
        return from + ":" + event;
    }

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @MethodSource("allowedTransitions")
    @DisplayName("allowed transitions update status per spec matrix")
    void should_apply_allowed_transition(TransitionCase case_) {
        Ticket ticket = TicketFactory.withStatus(case_.from);
        long versionBefore = ticket.getVersion();
        Instant updatedAtBefore = ticket.getUpdatedAt();

        stateMachine.applyTransition(ticket, case_.event);

        assertThat(ticket.getStatus()).isEqualTo(case_.to);
        assertThat(ticket.getVersion()).isEqualTo(versionBefore + 1);
        assertThat(ticket.getUpdatedAt()).isAfter(updatedAtBefore);
        assertThat(ticket.getUpdatedAt()).isEqualTo(FIXED_NOW);
        assertThat(ticket.getTitle()).isEqualTo("Test ticket");
        assertThat(ticket.getDescription()).isEqualTo("Description");
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(ticket.getRequesterId())
                .isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    }

    @ParameterizedTest(name = "{0} + {1} is forbidden")
    @MethodSource("forbiddenTransitions")
    @DisplayName("forbidden transitions throw InvalidStateTransitionException")
    void should_reject_forbidden_transition(TicketStatus from, TransitionEvent event) {
        Ticket ticket = TicketFactory.withStatus(from);
        long versionBefore = ticket.getVersion();
        TicketStatus statusBefore = ticket.getStatus();

        assertThatThrownBy(() -> stateMachine.applyTransition(ticket, event))
                .isInstanceOf(InvalidStateTransitionException.class)
                .satisfies(
                        ex -> {
                            InvalidStateTransitionException illegal =
                                    (InvalidStateTransitionException) ex;
                            assertThat(illegal.getFromStatus()).isEqualTo(from);
                            assertThat(illegal.getEvent()).isEqualTo(event);
                        });

        assertThat(ticket.getStatus()).isEqualTo(statusBefore);
        assertThat(ticket.getVersion()).isEqualTo(versionBefore);
    }

    @Nested
    @DisplayName("representative illegal transitions from spec")
    class RepresentativeIllegalTransitions {

        @Test
        @DisplayName("CLOSED + START_PROGRESS cannot reopen ticket")
        void should_reject_start_progress_when_closed() {
            Ticket ticket = TicketFactory.withStatus(CLOSED);

            assertThatThrownBy(() -> stateMachine.applyTransition(ticket, START_PROGRESS))
                    .isInstanceOf(InvalidStateTransitionException.class);

            assertThat(ticket.getStatus()).isEqualTo(CLOSED);
        }

        @Test
        @DisplayName("OPEN + CLOSE cannot close without resolving")
        void should_reject_close_when_open() {
            Ticket ticket = TicketFactory.open();

            assertThatThrownBy(() -> stateMachine.applyTransition(ticket, CLOSE))
                    .isInstanceOf(InvalidStateTransitionException.class);

            assertThat(ticket.getStatus()).isEqualTo(OPEN);
        }

        @Test
        @DisplayName("RESOLVED + CANCEL is not allowed after resolve")
        void should_reject_cancel_when_resolved() {
            Ticket ticket = TicketFactory.withStatus(RESOLVED);

            assertThatThrownBy(() -> stateMachine.applyTransition(ticket, CANCEL))
                    .isInstanceOf(InvalidStateTransitionException.class);

            assertThat(ticket.getStatus()).isEqualTo(RESOLVED);
        }

        @Test
        @DisplayName("IN_PROGRESS + START_PROGRESS is not allowed when already in progress")
        void should_reject_start_progress_when_in_progress() {
            Ticket ticket = TicketFactory.withStatus(IN_PROGRESS);

            assertThatThrownBy(() -> stateMachine.applyTransition(ticket, START_PROGRESS))
                    .isInstanceOf(InvalidStateTransitionException.class);

            assertThat(ticket.getStatus()).isEqualTo(IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("lifecycle happy paths")
    class LifecycleHappyPaths {

        @Test
        @DisplayName("OPEN → IN_PROGRESS → RESOLVED → CLOSED")
        void should_complete_resolve_path() {
            Ticket ticket = TicketFactory.open();

            stateMachine.applyTransition(ticket, START_PROGRESS);
            assertThat(ticket.getStatus()).isEqualTo(IN_PROGRESS);

            stateMachine.applyTransition(ticket, RESOLVE);
            assertThat(ticket.getStatus()).isEqualTo(RESOLVED);

            stateMachine.applyTransition(ticket, CLOSE);
            assertThat(ticket.getStatus()).isEqualTo(CLOSED);
            assertThat(ticket.getVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("OPEN → CANCELLED")
        void should_cancel_from_open() {
            Ticket ticket = TicketFactory.open();

            stateMachine.applyTransition(ticket, CANCEL);

            assertThat(ticket.getStatus()).isEqualTo(CANCELLED);
        }

        @Test
        @DisplayName("IN_PROGRESS → CANCELLED")
        void should_cancel_from_in_progress() {
            Ticket ticket = TicketFactory.withStatus(IN_PROGRESS);

            stateMachine.applyTransition(ticket, CANCEL);

            assertThat(ticket.getStatus()).isEqualTo(CANCELLED);
        }
    }
}
