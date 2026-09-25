# Support Ticket Management System — Requirements

## Status

Draft

## Traceability

| Document | Purpose |
| --- | --- |
| [architecture.md](./architecture.md) | System structure and responsibilities |
| [data-model.md](./data-model.md) | Persistence and domain data |
| [api-contract.md](./api-contract.md) | HTTP API |
| [state-machine.md](./state-machine.md) | Ticket lifecycle |
| [ui-flow.md](./ui-flow.md) | Next.js user experience |
| [test-strategy.md](./test-strategy.md) | Verification approach |

**Stack:** Java 21, Spring Boot 3.x, PostgreSQL, React/Next.js (App Router).

**Normative rules:** `/rules/java-springboot.md`, `/rules/api-standards.md`, `/rules/testing.md`.

---

## Problem

Support teams need a single place to capture customer issues as tickets, assign work, track progress through a controlled lifecycle, discuss tickets via comments, and find tickets quickly by keyword and status.

## Actors and permissions

| Actor | Description | Capabilities |
| --- | --- | --- |
| **Requester** | Customer or internal user who opens tickets | Create ticket; view and comment on own tickets; cannot change status beyond create (initial `OPEN`) |
| **Agent** | Support staff | Full ticket CRUD fields; perform allowed status transitions; comment on any ticket; search and filter |
| **Admin** | System administrator | Same as Agent; manage users (out of scope for v1 API unless noted) |

Authentication is required for all ticket APIs. Authorization:

- Requesters may `GET` / `PATCH` (non-status fields only) / `POST` comments only on tickets where `requesterId` matches the authenticated user.
- Agents and Admins may access all tickets and apply status transitions per [state-machine.md](./state-machine.md).

Role names in JWT or session claims: `REQUESTER`, `AGENT`, `ADMIN`.

---

## Scope

### In scope (v1)

| ID | Requirement |
| --- | --- |
| TCK-01 | Create a ticket with title, description, priority, and optional assignee. New tickets start in status `OPEN`. |
| TCK-02 | List tickets with pagination, sort, filter by status, and keyword search across title and description. |
| TCK-03 | View a single ticket including current status, fields, and metadata (timestamps, version). |
| TCK-04 | Update ticket title, description, priority, and assignee. Status changes only through the status transition operation defined in the API contract. |
| TCK-05 | Enforce the ticket status state machine on the **backend**; illegal transitions return HTTP `409` with code `TICKET_ILLEGAL_TRANSITION`. |
| TCK-06 | Add comments to a ticket (author, body, timestamp). Comments are append-only in v1 (no edit/delete). |
| TCK-07 | Filter ticket list by one or more status values via query parameters. |
| TCK-08 | Search tickets by keyword (case-insensitive) in title and description. |
| TCK-09 | Next.js UI for list, detail, create, update, comment, status actions, search, and status filter. |
| TCK-10 | Store all instants in UTC; expose ISO-8601 in JSON. |

### Out of scope (v1)

- Email ingestion, webhooks, SLA timers, attachments, tags, custom fields.
- Comment edit/delete, ticket delete, soft-delete archive.
- Multi-tenant organizations, teams, and queue routing rules.
- Real-time notifications (WebSocket/SSE).
- Full user management UI (assignee picker may use a read-only user list endpoint stub or mock until user service exists).

---

## Domain summary

- **Ticket** aggregate: identity (UUID), title, description, priority, status, requester, optional assignee, optimistic version, audit timestamps.
- **Comment** entity: belongs to one ticket; ordered by creation time.
- **Priority** enum: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
- **Status** enum: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`.

Lifecycle rules are defined only in [state-machine.md](./state-machine.md). The UI and API must not allow transitions that the backend rejects.

---

## Non-functional requirements

| ID | Requirement |
| --- | --- |
| NFR-01 | API version prefix `/api/v1`. |
| NFR-02 | Error responses use RFC 9457 Problem Details per `/rules/api-standards.md`. |
| NFR-03 | List endpoints support `page`, `size`, `sort` with the standard pagination envelope. |
| NFR-04 | Ticket IDs are UUIDs (opaque). |
| NFR-05 | Structured logging with correlation ID; no passwords or tokens in logs. |
| NFR-06 | PostgreSQL is the system of record for tickets and comments. |

---

## Acceptance criteria

### Ticket CRUD and list

**TCK-01 — Create**

- **Given** an authenticated Agent or Requester with valid payload  
- **When** `POST /api/v1/tickets`  
- **Then** response is `201` with `status` `OPEN`, generated `id`, `requesterId` set to the caller (Requester) or as specified (Agent creating on behalf of someone), and `Location` header.

**TCK-02 — List with search and filter**

- **Given** tickets exist with varied statuses and text  
- **When** `GET /api/v1/tickets?status=OPEN&status=IN_PROGRESS&q=login&page=0&size=20`  
- **Then** response is `200` with pagination envelope; every item matches at least one requested status (OR semantics) and matches keyword search rules in [api-contract.md](./api-contract.md).

**TCK-03 — View**

- **Given** a ticket ID the caller may access  
- **When** `GET /api/v1/tickets/{ticketId}`  
- **Then** response is `200` with full ticket representation.

**TCK-04 — Update fields**

- **Given** a ticket not in a terminal status (`CLOSED`, `CANCELLED`)  
- **When** `PATCH /api/v1/tickets/{ticketId}` with title, description, priority, or assignee  
- **Then** response is `200` with updated fields and incremented version; `status` unchanged.

### State machine

**TCK-05 — Legal transition**

- **Given** a ticket in `OPEN`  
- **When** Agent posts `POST /api/v1/tickets/{ticketId}/transitions` with event `START_PROGRESS`  
- **Then** status becomes `IN_PROGRESS` and response is `200`.

**TCK-05b — Illegal transition**

- **Given** a ticket in `CLOSED`  
- **When** any transition event is requested  
- **Then** response is `409`, `code` is `TICKET_ILLEGAL_TRANSITION`, and persisted status is unchanged.

### Comments

**TCK-06 — Add comment**

- **Given** a ticket the caller may access  
- **When** `POST /api/v1/tickets/{ticketId}/comments` with non-empty body  
- **Then** response is `201` with comment resource; comment appears on subsequent list/get comments calls.

### Authorization

**TCK-AUTH-01**

- **Given** a Requester and a ticket owned by another user  
- **When** `GET /api/v1/tickets/{ticketId}`  
- **Then** response is `404` (or `403` if product policy prefers explicit forbidden; v1 uses `404` to avoid leaking existence).

---

## Open questions

| ID | Question | Default for implementation |
| --- | --- | --- |
| OQ-01 | User service for assignee display names | v1: API returns `assigneeId` UUID; UI shows truncated UUID until user profile API exists |
| OQ-02 | Requester-created tickets: can Requester set assignee? | No; assignee optional on create for Agent only |

---

## Definition of ready

- [ ] Status moved to Review or Approved
- [ ] [state-machine.md](./state-machine.md) reviewed against TCK-05
- [ ] [api-contract.md](./api-contract.md) reviewed against `/rules/api-standards.md`
- [ ] Review completed via `/commands/review-spec.md`
