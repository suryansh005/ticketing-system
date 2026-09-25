# Spec-First Documentation Guidelines

Write the specification **before** implementation. Code implements `/spec`; it does not invent behavior.

## When this skill applies

Use this skill when adding features, changing APIs, altering ticket workflows, or writing anything under `/spec` or `/docs`.

## Workflow

1. **Capture intent** in `/spec` (problem, actors, scope, out of scope).
2. **Define the contract** (API, events, UI, data) so Java and Next.js can proceed independently.
3. **Define acceptance criteria** that are testable (see `rules/testing.md`).
4. **Review the spec** with `/commands/review-spec.md` before coding.
5. **Implement** against the spec; if code disagrees, **update the spec first** (or fix the code).
6. **Generate or update tests** from the spec (`/commands/generate-tests.md`).
7. **Review code** against rules (`/commands/review-code.md`).

Do not start Spring controllers, entities, or Next.js pages until the spec for that slice is reviewed.

## Spec location and naming

- Directory: `/spec`
- One feature (or one bounded change) per file: `spec/tickets-lifecycle.md`
- Optional split: `spec/<feature>/overview.md`, `api.md`, `states.md`, `ui.md`
- Language: English, present tense (“The system assigns…”).
- IDs: stable requirement IDs (`TCK-12`) so tests and reviews can cite them.

## Required sections (feature spec)

```markdown
# <Feature name>

## Status
Draft | Review | Approved

## Traceability
- Requirement IDs:
- Related specs:
- Prompt history: docs/prompt-history.md (if used)

## Problem
What user/job is unblocked.

## Actors and permissions
Who can do what.

## Scope / Out of scope

## Domain
- Aggregates and invariants
- State machine: states, events, guards, actions (table)
- Data retained and retention rules (no secrets in examples)

## API
- Endpoints (method, path, request, response)
- Error codes and HTTP statuses (align with rules/api-standards.md)
- Idempotency and concurrency

## UI (Next.js)
- Routes and states (loading, empty, error)
- What the UI must not assume beyond the API contract

## Acceptance criteria
Given / When / Then, one criterion per ID.

## Test notes
Which cases are unit vs integration vs UI.
```

## State-machine tables

Every workflow feature includes a table:

| From | Event | Guard | To | Side effects |
| --- | --- | --- | --- | --- |
| OPEN | ASSIGN | assignee exists | IN_PROGRESS | TicketAssigned event |

Plus an explicit **illegal transitions** list or a full matrix so tests can be generated without guessing.

## API fragments in specs

- Use the URL and Problem Details rules from `rules/api-standards.md`.
- Example JSON must be valid and use `camelCase`.
- Mark fields required vs optional.

## Quality bar

- **Unambiguous**: a second engineer could implement without asking the author.
- **Testable**: every SHALL has an acceptance criterion.
- **Bounded**: out of scope is written down.
- **Secure**: authz rules stated; no credentials in examples.
- **Stack-aware**: Java 21/Spring Boot vs Next.js responsibilities are explicit (who owns session, who owns domain rules).

## What not to put in a spec

- Class names, package trees, or library versions (unless they are constraints).
- Copy-pasted framework tutorials.
- Open questions mixed into requirements; use an `Open questions` section until resolved, then delete or move to ADR.

## Definition of ready (to implement)

- [ ] Status is Review or Approved
- [ ] Actors, API, and state machine (if any) complete
- [ ] Acceptance criteria cover happy path, authz denial, validation, and illegal transitions
- [ ] Errors named with `code` + HTTP status
- [ ] Spec reviewed via `/commands/review-spec.md`
