# API Contract

## Status

Draft

## Traceability

- Requirements: [requirements.md](./requirements.md)
- Workflow: [state-machine.md](./state-machine.md)
- Standards: `/rules/api-standards.md`

**Base URL:** `https://{host}/api/v1`  
**Content-Type:** `application/json`  
**Errors:** `application/problem+json` (RFC 9457)

---

## Authentication

All endpoints require authentication unless noted.

```
Authorization: Bearer <access_token>
```

| Role | Scope |
| --- | --- |
| `REQUESTER` | Own tickets + comments |
| `AGENT`, `ADMIN` | All tickets |

---

## Resources

### Ticket representation (`TicketResponse`)

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | UUID string | yes | |
| `title` | string | yes | max 200 |
| `description` | string | yes | |
| `priority` | enum | yes | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `status` | enum | yes | See state machine |
| `requesterId` | UUID string | yes | |
| `assigneeId` | UUID string \| null | yes | |
| `version` | number | yes | Optimistic lock |
| `createdAt` | ISO-8601 UTC | yes | |
| `updatedAt` | ISO-8601 UTC | yes | |

### Comment representation (`CommentResponse`)

| Field | Type | Required |
| --- | --- | --- |
| `id` | UUID string | yes |
| `ticketId` | UUID string | yes |
| `authorId` | UUID string | yes |
| `body` | string | yes |
| `createdAt` | ISO-8601 UTC | yes |

### Pagination envelope

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

---

## Endpoints

### Create ticket

`POST /tickets`

**Roles:** `REQUESTER`, `AGENT`, `ADMIN`

**Request body (`CreateTicketRequest`):**

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `title` | string | yes | not blank, max 200 |
| `description` | string | yes | not blank |
| `priority` | enum | yes | |
| `assigneeId` | UUID | no | Agent/Admin only; ignored or `403` if Requester sends |

**Response:** `201 Created`  
**Headers:** `Location: /api/v1/tickets/{ticketId}`  
**Body:** `TicketResponse` with `status` `OPEN`

**Errors:**

| Status | code | When |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Bean validation failed |
| 401 | `UNAUTHORIZED` | Missing/invalid token |
| 403 | `FORBIDDEN` | Policy violation (e.g. assignee on create as Requester) |

---

### List tickets

`GET /tickets`

**Roles:** `AGENT`, `ADMIN` (all tickets); `REQUESTER` (implicit filter `requesterId = self`)

**Query parameters:**

| Param | Type | Required | Notes |
| --- | --- | --- | --- |
| `status` | enum | no | Repeatable; OR semantics (`status=OPEN&status=IN_PROGRESS`) |
| `q` | string | no | Keyword search title and description, case-insensitive |
| `page` | int | no | default `0`, min 0 |
| `size` | int | no | default `20`, max `100` |
| `sort` | string | no | default `createdAt,desc`; fields: `createdAt`, `updatedAt`, `priority`, `status` |

**Response:** `200 OK` — pagination envelope of `TicketResponse`

**Errors:** `400` for invalid enum or sort field

---

### Get ticket

`GET /tickets/{ticketId}`

**Response:** `200 OK` — `TicketResponse`

**Errors:**

| Status | code | When |
| --- | --- | --- |
| 404 | `TICKET_NOT_FOUND` | Unknown ID or not visible to Requester |

---

### Update ticket (fields only)

`PATCH /tickets/{ticketId}`

**Roles:** per resource ownership; Agents may update any non-terminal ticket.

**Request body (`UpdateTicketRequest`):** all fields optional; at least one required

| Field | Type | Notes |
| --- | --- | --- |
| `title` | string | max 200 |
| `description` | string | |
| `priority` | enum | |
| `assigneeId` | UUID \| null | clear assignee with `null` |

**Rules:**

- Does **not** change `status`. Status changes use `/transitions`.
- Rejected when `status` is `CLOSED` or `CANCELLED` → `409` `TICKET_TERMINAL`.
- Optional concurrency: header `If-Match: "{version}"` → mismatch `409` `TICKET_VERSION_CONFLICT`.

**Response:** `200 OK` — `TicketResponse`

---

### Apply status transition

`POST /tickets/{ticketId}/transitions`

**Roles:** `AGENT`, `ADMIN` only

**Request body (`TransitionRequest`):**

| Field | Type | Required |
| --- | --- | --- |
| `event` | enum | yes |

**Event enum:** `START_PROGRESS`, `RESOLVE`, `CLOSE`, `CANCEL`

Mapping to target status is defined in [state-machine.md](./state-machine.md).

**Response:** `200 OK` — `TicketResponse` with new `status`

**Errors:**

| Status | code | When |
| --- | --- | --- |
| 409 | `TICKET_ILLEGAL_TRANSITION` | Illegal `(status, event)`; body includes `fromStatus`, `event` |
| 409 | `TICKET_VERSION_CONFLICT` | Optimistic lock failure |
| 404 | `TICKET_NOT_FOUND` | |

**Example problem (illegal transition):**

```json
{
  "type": "https://api.example.com/problems/ticket-illegal-transition",
  "title": "Illegal state transition",
  "status": 409,
  "detail": "Cannot apply CLOSE to ticket in status OPEN.",
  "instance": "/api/v1/tickets/3fa85f64-5717-4562-b3fc-2c963f66afa6/transitions",
  "code": "TICKET_ILLEGAL_TRANSITION",
  "fromStatus": "OPEN",
  "event": "CLOSE"
}
```

---

### List comments

`GET /tickets/{ticketId}/comments`

**Query:** `page`, `size` (default size 50), `sort=createdAt,asc`

**Response:** `200 OK` — pagination envelope of `CommentResponse`

---

### Add comment

`POST /tickets/{ticketId}/comments`

**Roles:** any role that can view the ticket

**Request body (`CreateCommentRequest`):**

| Field | Type | Required |
| --- | --- | --- |
| `body` | string | yes, not blank, max 10000 |

**Response:** `201 Created` — `CommentResponse`  
**Headers:** `Location: /api/v1/tickets/{ticketId}/comments/{commentId}`

**Errors:** `404` ticket not found; `400` validation

---

## Allowed transitions quick reference

| Current status | Allowed `event` values |
| --- | --- |
| `OPEN` | `START_PROGRESS`, `CANCEL` |
| `IN_PROGRESS` | `RESOLVE`, `CANCEL` |
| `RESOLVED` | `CLOSE` |
| `CLOSED` | *(none)* |
| `CANCELLED` | *(none)* |

Full matrix: [state-machine.md](./state-machine.md).

---

## Machine-readable error codes

| code | HTTP | Usage |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | Request validation |
| `UNAUTHORIZED` | 401 | Auth |
| `FORBIDDEN` | 403 | Authz |
| `TICKET_NOT_FOUND` | 404 | Ticket |
| `TICKET_ILLEGAL_TRANSITION` | 409 | State machine |
| `TICKET_TERMINAL` | 409 | PATCH on closed/cancelled |
| `TICKET_VERSION_CONFLICT` | 409 | Concurrent update |

Validation errors include `errors[]` with `{ "field", "message", "code" }`.

---

## Example flows

### Create and start progress

```http
POST /api/v1/tickets
Content-Type: application/json

{
  "title": "VPN drops hourly",
  "description": "Disconnects after 60 minutes.",
  "priority": "MEDIUM"
}
```

```http
POST /api/v1/tickets/{ticketId}/transitions
Content-Type: application/json

{
  "event": "START_PROGRESS"
}
```

### Search and filter

```http
GET /api/v1/tickets?q=vpn&status=OPEN&status=IN_PROGRESS&page=0&size=20&sort=createdAt,desc
```

---

## Compatibility

- Additive JSON fields on responses are allowed in future versions.
- Clients must ignore unknown fields.
- Breaking changes require `/api/v2`.
