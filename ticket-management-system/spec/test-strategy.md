# Test Strategy

## Status

Draft

## Traceability

- Requirements: [requirements.md](./requirements.md)
- State machine: [state-machine.md](./state-machine.md)
- API: [api-contract.md](./api-contract.md)
- Standards: `/rules/testing.md`

Tests prove the **specification**. Naming follows `should_expectedBehavior_when_condition` or `@DisplayName` sentences drawn from requirement IDs.

---

## Test pyramid

| Layer | Scope | Tools | Runs in CI |
| --- | --- | --- | --- |
| Unit | Domain transitions, validators, mappers | JUnit 5, AssertJ, Mockito at boundaries | Every commit |
| Slice | Controllers, validation, exception advice | `@WebMvcTest` | Every commit |
| Integration | JPA, full use cases, security | Spring Boot Test + Testcontainers PostgreSQL | Every PR |
| Frontend | Critical flows | Playwright or React Testing Library (team choice) | PR / nightly |

Do not start the full application to test a transition that a pure domain test can cover.

---

## Requirement → test mapping

| Requirement | Unit | Slice | Integration | UI |
| --- | --- | --- | --- | --- |
| TCK-01 Create | Factory defaults `OPEN` | POST validation | POST persists row | UI-01 |
| TCK-02 List/search/filter | Query spec helpers | GET params binding | SQL filter + pagination | UI-02 |
| TCK-03 View | — | GET 404 mapping | GET by id | Detail load |
| TCK-04 PATCH fields | Terminal guard | PATCH validation | PATCH version bump | Edit form |
| TCK-05 Transitions | **Full matrix** | POST transitions | Persisted state | UI-03, UI-04 |
| TCK-06 Comments | Comment invariants | POST comment validation | FK + list order | UI-05 |
| TCK-AUTH-01 | — | Security rules | Requester isolation | — |

---

## State machine tests (mandatory)

### Unit: transition table

Implement parameterized tests from [state-machine.md](./state-machine.md) **Full transition matrix**.

**Allowed cases** (example source):

```java
record TransitionCase(TicketStatus from, TransitionEvent event, TicketStatus to) {}

static Stream<TransitionCase> allowedTransitions() {
    return Stream.of(
        new TransitionCase(OPEN, START_PROGRESS, IN_PROGRESS),
        new TransitionCase(OPEN, CANCEL, CANCELLED),
        new TransitionCase(IN_PROGRESS, RESOLVE, RESOLVED),
        new TransitionCase(IN_PROGRESS, CANCEL, CANCELLED),
        new TransitionCase(RESOLVED, CLOSE, CLOSED)
    );
}
```

For each allowed case assert:

- Resulting status equals `to`
- Version increment (if aggregate loaded with version)
- No exception

**Forbidden cases:** generate Cartesian product of all statuses × all events minus allowed set. For each:

- Domain throws (or returns `Either`/rejection type)
- Mapped to `409` / `TICKET_ILLEGAL_TRANSITION` at API layer

**Nested structure** (per `/rules/testing.md`):

```text
TicketStateMachineTest
  WhenStatusIsOpen
  WhenStatusIsInProgress
  WhenStatusIsResolved
  WhenStatusIsClosed
  WhenStatusIsCancelled
```

### Representative illegal examples (must be included)

| From | Event | Expected code |
| --- | --- | --- |
| `CLOSED` | `START_PROGRESS` | `TICKET_ILLEGAL_TRANSITION` |
| `OPEN` | `CLOSE` | `TICKET_ILLEGAL_TRANSITION` |
| `RESOLVED` | `CANCEL` | `TICKET_ILLEGAL_TRANSITION` |
| `IN_PROGRESS` | `START_PROGRESS` | `TICKET_ILLEGAL_TRANSITION` |

### Invariants (after every successful transition)

- Status matches spec
- `updated_at` advanced (use fixed `Clock`)
- History row count +1 when history table enabled

### Invariants (after rejection)

- Status and version unchanged
- No extra history rows

---

## API slice tests

`@WebMvcTest` for `TicketController` and `TransitionController` (or combined):

| Case | Assert |
| --- | --- |
| Invalid body | `400`, `VALIDATION_ERROR`, `errors[]` |
| Illegal transition | `409`, `TICKET_ILLEGAL_TRANSITION`, body `fromStatus` + `event` |
| PATCH with `status` field | `400` |
| PATCH on `CLOSED` | `409`, `TICKET_TERMINAL` |
| Not found | `404`, `TICKET_NOT_FOUND` |

Use `@MockBean` for application services; verify Problem Details shape matches `/rules/api-standards.md`.

---

## Integration tests

**Location:** `...integration` package, suffix `IT` or `IntegrationTest`.

| Scenario | Steps |
| --- | --- |
| Lifecycle happy path | Create → START_PROGRESS → RESOLVE → CLOSE; reload DB each step |
| Cancel from OPEN | Create → CANCEL |
| Cancel from IN_PROGRESS | Create → START_PROGRESS → CANCEL |
| Concurrent PATCH | Two threads, same ticket, different versions → one `409 TICKET_VERSION_CONFLICT` |
| Search | Seed titles/descriptions; `GET` with `q` returns expected subset |
| Status filter | Seed mixed statuses; multi `status` param |
| Requester scope | Requester A cannot read Requester B ticket (`404`) |
| Comment thread | Two comments; order by `created_at` asc |

**Infrastructure:** Testcontainers PostgreSQL; Flyway migrations applied before suite.

**Clock:** inject fixed `Clock` bean in test configuration for deterministic timestamps.

---

## Test data

- `TicketFactory.open(title)` — default priority, requester UUID
- Explicit UUIDs when asserting identity in JSON
- No production secrets; Testcontainers credentials in `application-test.yml`

---

## Frontend tests (minimum)

| ID | Test |
| --- | --- |
| UI-02 | Changing status chip updates request URL and refetches (mock `fetch`) |
| UI-03 | Agent sees “Start progress” only when status is `OPEN` (mock ticket) |
| UI-04 | Mock `409` on transition shows error message |

E2E (optional v1): single Playwright flow create → comment → resolve → close against docker-compose stack.

---

## Coverage expectations

- Every row in the state machine matrix has at least one automated test (allow or deny).
- Every machine-readable `code` in [api-contract.md](./api-contract.md) has at least one test producing it.
- Bug fixes: failing test first, then fix.

---

## CI gates

1. Unit + slice tests < 2 minutes
2. Integration tests with Testcontainers < 10 minutes
3. Lint/format (Checkstyle/Spotless, ESLint) per repo config
4. No `Thread.sleep` for business timing

---

## Generating tests from spec

Use `/commands/generate-tests.md` to produce skeleton classes from:

- [state-machine.md](./state-machine.md) matrix
- [requirements.md](./requirements.md) acceptance criteria (Given/When/Then)

Human review required before merging generated suites.

---

## Checklist before release

- [ ] Allowed + forbidden transition parameterized sources match spec matrix
- [ ] Integration test proves persistence after illegal transition does not change row
- [ ] API Problem Details include `code` and no stack traces
- [ ] TCK-02 search and filter covered at integration level
