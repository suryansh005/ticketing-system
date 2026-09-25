# API Standards

Apply to Spring Boot JSON APIs and to Next.js Route Handlers / BFF endpoints that expose HTTP to clients.

## URL design

- Prefix: `/api/v{n}/...`. Breaking changes increment `n`; additive fields may stay on the same version.
- Resources are **nouns**, plural: `/api/v1/tickets`, `/api/v1/users`.
- Identify with opaque IDs: `/api/v1/tickets/{ticketId}`.
- Sub-resources for genuine containment: `/api/v1/tickets/{ticketId}/comments`.
- Actions that are not CRUD become sub-resources or documented verbs only when no noun fits: `POST /api/v1/tickets/{ticketId}/assignment` rather than `/assignTicket`.
- Filtering, sorting, pagination are **query parameters**, not path segments: `GET /api/v1/tickets?status=OPEN&page=0&size=20&sort=createdAt,desc`.
- Do not nest more than two resource levels unless the spec requires it.
- File downloads: `GET .../attachments/{attachmentId}` with `Content-Disposition`.
- Next.js app routes (`/tickets/[id]`) are UI. Browser data fetching still uses the `/api` contract unless the spec defines RSC-only loaders.

### Methods

| Method | Meaning | Body | Idempotent |
| --- | --- | --- | --- |
| `GET` | Read | No | Yes |
| `HEAD` | Metadata | No | Yes |
| `POST` | Create or non-idempotent command | Yes | No (unless Idempotency-Key) |
| `PUT` | Replace resource | Yes | Yes |
| `PATCH` | Partial update (JSON Merge Patch or documented sparse DTO) | Yes | Yes if documented |
| `DELETE` | Remove | No | Yes |

Do not use `GET` for state changes.

## Request and response bodies

- JSON, `Content-Type: application/json`.
- Field names: `camelCase`.
- Timestamps: ISO-8601 UTC (`2026-09-24T10:57:00Z`).
- Identifiers: UUID strings.
- Enums: uppercase names matching the domain (`OPEN`, `IN_PROGRESS`).
- Do not wrap every success as `{ "success": true, "data": ... }` unless the spec already standardized that envelope. Prefer raw resources and RFC 9457 errors.

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

## Error payload

Use **RFC 9457 Problem Details** (`application/problem+json`) unless an existing client contract forbids it.

```json
{
  "type": "https://api.example.com/problems/ticket-illegal-transition",
  "title": "Illegal state transition",
  "status": 409,
  "detail": "Cannot close ticket 3fa85f64-5717-4562-b3fc-2c963f66afa6 from DRAFT.",
  "instance": "/api/v1/tickets/3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "code": "TICKET_ILLEGAL_TRANSITION",
  "fromStatus": "DRAFT",
  "event": "CLOSE"
}
```

Rules:

- `type`: stable URI per error kind (may be a documentation URL).
- `title`: short, not localized per user unless product requires i18n.
- `status`: same as the HTTP status line.
- `detail`: safe for the caller; no stack traces, SQL, or internal hostnames.
- `code`: machine-readable constant for clients (ticket app, Next.js).
- Validation errors: `400` with an `errors` array of `{ "field", "message", "code" }`.
- Correlation: include `traceId` when the platform provides one.

### Validation example

```json
{
  "type": "https://api.example.com/problems/validation",
  "title": "Constraint violation",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "errors": [
    { "field": "title", "message": "must not be blank", "code": "NOT_BLANK" }
  ]
}
```

## HTTP status codes

| Status | When |
| --- | --- |
| `200 OK` | Successful GET/PUT/PATCH/DELETE with a body, or DELETE with representation |
| `201 Created` | POST created a resource; `Location` header to the new URL |
| `204 No Content` | Successful DELETE or action with no body |
| `400 Bad Request` | Malformed JSON, failed validation, illegal query types |
| `401 Unauthorized` | Missing or invalid credentials |
| `403 Forbidden` | Authenticated but not allowed |
| `404 Not Found` | Resource ID does not exist (do not leak whether a hidden resource exists if spec says so) |
| `409 Conflict` | Version conflict, duplicate create, **illegal state-machine transition** |
| `412 Precondition Failed` | `If-Match` / ETag failed |
| `422 Unprocessable Entity` | Syntactically valid but semantically unusable **when distinct from 409**; prefer 409 for workflow conflicts |
| `429 Too Many Requests` | Rate limit |
| `500 Internal Server Error` | Unexpected failure; log server-side; generic detail to client |
| `503 Service Unavailable` | Dependency down |

Do not return `200` with an embedded error object.

## Headers

- `Location` on `201`.
- `ETag` / `If-Match` when the spec requires optimistic concurrency.
- `Idempotency-Key` on create/command POSTs when specified.
- `Cache-Control: no-store` for authenticated ticket data unless the spec allows caching.

## Compatibility

- Additive JSON fields are non-breaking. Removing/renaming fields or changing status codes is breaking.
- Next.js must treat unknown fields as ignore-forward compatible.

## Checklist

- [ ] Resource URLs, not RPC
- [ ] Correct method and status
- [ ] Problem Details on errors with stable `code`
- [ ] No stack traces in bodies
- [ ] Pagination and time formats consistent
