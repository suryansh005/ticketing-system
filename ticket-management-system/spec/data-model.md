# Data Model

## Status

Draft

## Traceability

- Requirements: [requirements.md](./requirements.md)
- API shapes: [api-contract.md](./api-contract.md)
- Lifecycle: [state-machine.md](./state-machine.md)

---

## Overview

PostgreSQL stores two main tables: `ticket` and `ticket_comment`. Users are referenced by UUID (`requester_id`, `assignee_id`, `author_id`) without a local `user` table in v1.

All timestamps are `timestamptz` stored in UTC. API exposes them as ISO-8601 instant strings.

---

## Enumerations

### `ticket_status`

| Value | Terminal |
| --- | --- |
| `OPEN` | No |
| `IN_PROGRESS` | No |
| `RESOLVED` | No |
| `CLOSED` | Yes |
| `CANCELLED` | Yes |

### `ticket_priority`

`LOW`, `MEDIUM`, `HIGH`, `URGENT`

### Transition events (not stored as enum on ticket)

Persisted only in optional `ticket_status_history` if audit is enabled; v1 minimum is current `status` on `ticket` row. See [state-machine.md](./state-machine.md) for event names.

---

## Entity-relationship (logical)

```mermaid
erDiagram
  TICKET ||--o{ TICKET_COMMENT : has
  TICKET {
    uuid id PK
    varchar title
    text description
    varchar priority
    varchar status
    uuid requester_id
    uuid assignee_id nullable
    bigint version
    timestamptz created_at
    timestamptz updated_at
  }
  TICKET_COMMENT {
    uuid id PK
    uuid ticket_id FK
    uuid author_id
    text body
    timestamptz created_at
  }
```

---

## Table: `ticket`

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `id` | `UUID` | PK, default `gen_random_uuid()` | Opaque ID |
| `title` | `VARCHAR(200)` | NOT NULL | |
| `description` | `TEXT` | NOT NULL | |
| `priority` | `VARCHAR(20)` | NOT NULL | Check enum values |
| `status` | `VARCHAR(20)` | NOT NULL | Check enum values |
| `requester_id` | `UUID` | NOT NULL | |
| `assignee_id` | `UUID` | NULL | |
| `version` | `BIGINT` | NOT NULL, default 0 | JPA `@Version` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | Set on insert |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | Set on update |

**Check constraints (example):**

```sql
ALTER TABLE ticket ADD CONSTRAINT chk_ticket_status
  CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED','CANCELLED'));

ALTER TABLE ticket ADD CONSTRAINT chk_ticket_priority
  CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT'));
```

**Indexes:**

| Index | Columns | Purpose |
| --- | --- | --- |
| `idx_ticket_status` | `status` | Filter by status |
| `idx_ticket_created_at` | `created_at DESC` | Default sort |
| `idx_ticket_requester` | `requester_id` | Requester-scoped queries |
| `idx_ticket_assignee` | `assignee_id` | Agent workload (future) |
| `idx_ticket_search` | GIN or btree per chosen strategy | Keyword search on title/description |

**Search (v1):** composite filter `status IN (...)` AND (`title ILIKE '%' || :q || '%'` OR `description ILIKE '%' || :q || '%'`). Escape `%` and `_` in user input. Empty `q` omits text predicate.

---

## Table: `ticket_comment`

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `id` | `UUID` | PK | |
| `ticket_id` | `UUID` | NOT NULL, FK → `ticket(id)` ON DELETE RESTRICT | |
| `author_id` | `UUID` | NOT NULL | |
| `body` | `TEXT` | NOT NULL, length ≤ 10000 | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | Immutable |

**Indexes:**

| Index | Columns | Purpose |
| --- | --- | --- |
| `idx_comment_ticket_created` | `ticket_id`, `created_at ASC` | Thread ordering |

---

## Optional table: `ticket_status_history` (v1 optional, recommended for audit)

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `UUID` | PK |
| `ticket_id` | `UUID` | FK |
| `from_status` | `VARCHAR(20)` | |
| `to_status` | `VARCHAR(20)` | |
| `event` | `VARCHAR(50)` | e.g. `START_PROGRESS` |
| `actor_id` | `UUID` | |
| `created_at` | `TIMESTAMPTZ` | |

If omitted in v1, tests still require correct current `status` after transitions.

---

## Domain mapping notes

- JPA entity fields map to columns; application layer maps entity ↔ domain aggregate before invoking transition logic.
- `assignee_id` null means unassigned.
- Terminal statuses: updates to title/description/priority/assignee return `409` with `TICKET_TERMINAL` (see API contract).

---

## Retention

No automatic purge in v1. Backup and retention follow operations policy.

---

## Sample row (documentation only)

Ticket:

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "title": "Cannot reset password",
  "description": "Reset link returns 404.",
  "priority": "HIGH",
  "status": "OPEN",
  "requesterId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "assigneeId": null,
  "version": 0,
  "createdAt": "2026-09-24T10:00:00Z",
  "updatedAt": "2026-09-24T10:00:00Z"
}
```

Comment:

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "ticketId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "authorId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "body": "We are investigating.",
  "createdAt": "2026-09-24T11:00:00Z"
}
```
