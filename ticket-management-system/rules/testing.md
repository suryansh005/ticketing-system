# Testing Standards (JUnit 5, Mockito, State Machines)

Tests prove the **specification**, not the implementation details of Spring or Hibernate.

## Layout and naming

| Layer | Location | Name |
| --- | --- | --- |
| Unit | `src/test/java/...` next to production packages | `ClassNameTest` |
| Slice / MVC | same | `ClassNameWebMvcTest` or feature name |
| Integration | `src/test/java/.../integration` | `FeatureIT` or `FeatureIntegrationTest` |
| Architecture | optional | `ArchitectureTest` |

- Method names: `should_expectedBehavior_when_condition` or `@DisplayName` with a full sentence from the spec.
- One logical assertion theme per test. Multiple `assertThat` calls are fine if they check one outcome.
- Do not share mutable static state between tests.

## JUnit 5

- Use JUnit 5 (`org.junit.jupiter`). Do not add JUnit 4.
- Prefer `@Nested` classes for given/when groups (especially state-machine suites).
- Use `@ParameterizedTest` + `@ValueSource` / `@MethodSource` / `@CsvSource` for table-driven spec cases.
- Use `@TestInstance(Lifecycle.PER_CLASS)` only when construction is expensive **and** tests stay isolated.
- Assertions: AssertJ (`assertThat`) for fluency; JUnit assertions for simple equality is acceptable if the module already uses them.
- Time: inject `Clock` in production; in tests use a fixed clock. Never `Thread.sleep` to wait for business time.

```java
@Nested
class WhenTicketIsOpen {
    @ParameterizedTest(name = "transition {0} is allowed")
    @ValueSource(strings = {"ASSIGN", "CLOSE"})
    void should_allow_legal_events(String event) { /* ... */ }
}
```

## Mockito

- Mock **boundaries** (repositories, clocks, publishers, HTTP clients). Do not mock the class under test.
- Prefer constructor injection so tests can `new Service(mockRepo, clock)`.
- `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks` is allowed; explicit constructors are clearer for domain tests.
- `verify` interactions only when the spec requires a side effect (event published, email sent). Prefer state assertions.
- Never mock value objects, records, or collections.
- `lenient()` is a smell; fix stubbing or split the test.
- For Spring Data, prefer fakes/in-memory fakes over mocking every derived query when behavior is collection-like.

## What to unit-test vs integration-test

**Unit (fast, no Spring context)**

- Domain services, aggregates, state-machine transitions, validators, mappers.
- Pure functions and policy rules from `/spec`.

**Slice**

- `@WebMvcTest` for HTTP mapping, validation annotations, and exception advice.
- `@DataJpaTest` only when custom queries or mappings are the subject.

**Integration (Spring context + Testcontainers or equivalent)**

- Persistence round-trips, security filters, full use-cases that span adapters.
- State-machine persistence: saved state after events, optimistic locking, replay.

Do not start the full application for a domain rule that a unit test can cover.

## State-machine tests

Ticket workflows are specified as states, events, guards, and actions. Tests must follow the spec tables, not ad-hoc examples.

### Unit: transition table

For each `(fromState, event)` in the spec:

- **Allowed**: resulting state, emitted domain events, side-effect ports invoked.
- **Forbidden**: type of rejection (domain exception / problem code), state unchanged.
- **Guards**: both true and false paths (assignee present, SLA exceeded, role).

Use a matrix or parameterized source derived from the spec. If the spec changes, tests must fail until updated.

```java
record TransitionCase(TicketStatus from, TicketEventType event, TicketStatus to) {}

static Stream<TransitionCase> allowed() {
    return Stream.of(
        new TransitionCase(OPEN, ASSIGN, IN_PROGRESS),
        new TransitionCase(IN_PROGRESS, RESOLVE, RESOLVED)
    );
}
```

### Invariants

After every successful transition assert:

- Status matches spec
- Version / updated timestamp advanced (if specified)
- Illegal fields unchanged
- Audit/history entry appended when the spec requires a log

After every rejected transition assert:

- Same status and version
- No extra history rows
- Error code matches `rules/api-standards.md` / spec

### Integration: persisted machines

- Load an aggregate, apply events through the application service, flush, clear persistence context, reload, assert state.
- Concurrent updates: two transactions on the same ticket; expect optimistic lock failure as specified.
- Restart/replay: given stored events, reconstituted machine equals last known state.
- Scheduler/SLA timers: use a fixed `Clock` and trigger the job API or command, not wall-clock waits.

### Spring State Machine (if used)

- Test the machine configuration in isolation with a listener that records transitions.
- Persist `StateMachineContext` only in integration tests.
- Do not assert on internal interceptor order unless the spec names it.

## Test data

- Builders or factory methods (`TicketFactory.open()`), not copied 40-field constructors.
- IDs: explicit UUIDs in tests that assert identity; random UUIDs otherwise.
- Do not use production secrets. Testcontainers credentials stay in test config.

## Coverage expectations

- New spec behavior: at least one test per acceptance criterion.
- Bugfix: a failing test that reproduces the bug, then the fix.
- Do not add tests that only exercise getters.

## Checklist

- [ ] Tests named from spec language
- [ ] State-machine matrix covers allow and deny
- [ ] Mocks only at boundaries
- [ ] Integration tests prove persistence and concurrency where specified
- [ ] No sleeps, no hidden order dependencies
