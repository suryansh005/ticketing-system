# UI Flow (Next.js)

## Status

Draft

## Traceability

- Requirements: [requirements.md](./requirements.md)
- API: [api-contract.md](./api-contract.md)
- State machine: [state-machine.md](./state-machine.md)

The UI implements the API contract; it does not define business rules beyond presentation and client-side validation for usability.

---

## Routes

| Route | Purpose | Access |
| --- | --- | --- |
| `/` | Redirect to `/tickets` or marketing stub | Authenticated |
| `/tickets` | Ticket list with search and status filter | All roles |
| `/tickets/new` | Create ticket form | Requester, Agent, Admin |
| `/tickets/[ticketId]` | Ticket detail, edit, comments, status actions | Per ticket authz |
| `/login` | Auth entry (integration-specific) | Public |

Use App Router conventions: `app/tickets/page.tsx`, `app/tickets/new/page.tsx`, `app/tickets/[ticketId]/page.tsx`.

---

## Global UX

- **Loading:** skeleton rows on list; skeleton card on detail while fetching.
- **Empty:** list shows “No tickets match your filters” with clear-filters action.
- **Error:** show Problem Details `title` and `detail`; `TICKET_ILLEGAL_TRANSITION` on status action shows inline toast.
- **Auth:** unauthenticated users redirect to `/login`; `401` from API triggers re-login prompt.
- **Time:** display timestamps in user locale; API values remain UTC.

---

## Ticket list (`/tickets`)

### Layout

- Page title “Tickets”
- Toolbar: search input (`q`), multi-select or chip filter for status (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`)
- Primary button “New ticket” → `/tickets/new`
- Table columns: Title (link), Status, Priority, Assignee, Updated, Created

### Behavior

1. On mount, `GET /api/v1/tickets?page=0&size=20&sort=createdAt,desc` with current filters in query string (shareable URL: `/tickets?status=OPEN&q=login`).
2. Debounce search input (~300ms) before refetch.
3. Status filter: repeat `status` query param per API OR semantics.
4. Pagination controls update `page` and refetch.
5. Requester sees only own tickets (backend enforced); no extra UI mode required.

### States

| State | UI |
| --- | --- |
| Loading | Table skeleton |
| Success, zero rows | Empty state message |
| 4xx/5xx | Error banner with retry |

---

## Create ticket (`/tickets/new`)

### Form fields

| Field | Control | Validation (client) |
| --- | --- | --- |
| Title | text input | required, max 200 |
| Description | textarea | required |
| Priority | select | enum values |
| Assignee | optional select | Agent/Admin only; hidden for Requester |

### Submit

1. `POST /api/v1/tickets`
2. On `201`, navigate to `/tickets/[ticketId]`
3. On `400`, show field errors from `errors[]`
4. On `403`, show forbidden message

---

## Ticket detail (`/tickets/[ticketId]`)

### Sections

1. **Header** — title, status badge, priority badge
2. **Metadata** — requester ID, assignee, created/updated times, version (optional, debug)
3. **Actions** — status buttons driven by [state-machine.md](./state-machine.md)
4. **Edit form** — title, description, priority, assignee (PATCH)
5. **Comments** — thread + add comment form

### Status actions (Agent/Admin only)

| Current status | Buttons | API call |
| --- | --- | --- |
| `OPEN` | Start progress, Cancel | `POST .../transitions` `START_PROGRESS` / `CANCEL` |
| `IN_PROGRESS` | Resolve, Cancel | `RESOLVE` / `CANCEL` |
| `RESOLVED` | Close | `CLOSE` |
| `CLOSED`, `CANCELLED` | *(disabled/hidden)* | — |

On `409 TICKET_ILLEGAL_TRANSITION`, show error and refresh ticket from server.

Requester: hide status action bar; read-only status badge.

### Field edit

1. User edits fields → Save
2. `PATCH /api/v1/tickets/{ticketId}` with changed fields only
3. On `409 TICKET_TERMINAL`, disable form and show message
4. Optional: send `If-Match` with last known `version`

### Comments

1. Load `GET /api/v1/tickets/{ticketId}/comments?sort=createdAt,asc`
2. Render chronological list (author ID, body, time)
3. Add comment: `POST .../comments` → append to list or refetch
4. Empty thread: “No comments yet”

---

## Component map (suggested)

| Component | Responsibility |
| --- | --- |
| `TicketTable` | List rendering, pagination |
| `TicketFilters` | `q` + status chips → URL search params |
| `TicketForm` | Create/edit shared fields |
| `StatusBadge` | Color by status enum |
| `StatusActions` | Transition buttons + API |
| `CommentList` / `CommentForm` | Thread UI |

---

## API client layer

- `lib/api/tickets.ts`: `listTickets`, `getTicket`, `createTicket`, `updateTicket`, `transitionTicket`, `listComments`, `addComment`
- Parse Problem Details on non-2xx; throw typed `ApiError` with `code`, `status`, `detail`
- Base URL from `NEXT_PUBLIC_API_BASE_URL`

---

## Accessibility

- Form labels associated with inputs
- Status badges include text, not color alone
- Action buttons use verb labels (“Start progress”, not “Submit”)
- Focus management after create (move to detail heading)

---

## Out of scope (UI v1)

- Kanban board by status
- Bulk status update
- Rich text comments
- Real-time comment refresh (manual refetch after post is sufficient)

---

## Acceptance mapping (UI)

| ID | Criterion |
| --- | --- |
| UI-01 | User can create a ticket from `/tickets/new` and land on detail |
| UI-02 | User can search and filter on `/tickets` with URL-synced params |
| UI-03 | Agent can apply each legal transition from detail and see updated badge |
| UI-04 | Illegal transition shows error without client crash |
| UI-05 | User can add a comment and see it in the thread |
