# Support Ticket Management System

A full-stack support ticketing application for creating tickets, tracking lifecycle status, assigning agents, searching and filtering, and discussing issues via comments. The **backend** owns all business rules—especially ticket status changes—while the **React** UI consumes the REST API under `/api/v1`.

This repository was built using **Spec-Driven Development (SDD)**. Behavior is defined first in [`spec/`](./spec/) (requirements, API contract, data model, state machine, UI flows, and test strategy) and in [`rules/`](./rules/) (Java/Spring, API, and testing conventions). Application code implements those documents; when behavior changes, the spec is updated before or alongside the code. The domain [`TicketStateMachine`](./backend/src/main/java/com/tothenew/ticket/domain/TicketStateMachine.java) explicitly references [`spec/state-machine.md`](./spec/state-machine.md).

---

## Repository layout

| Path | Description |
| --- | --- |
| [`spec/`](./spec/) | Normative product and technical specifications (SDD source of truth) |
| [`rules/`](./rules/) | Engineering standards for API, Java/Spring Boot, and tests |
| [`backend/`](./backend/) | Spring Boot REST API (`ticket-api`) |
| [`frontend/`](./frontend/) | React + TypeScript SPA (Vite) |
| [`docs/`](./docs/) | Supplementary notes (e.g. SDD review history) |

---

## Tech stack

| Layer | Technologies |
| --- | --- |
| **Language (backend)** | Java 21 |
| **Backend framework** | Spring Boot 3.4.x (Web, Data JPA, Validation) |
| **Build (backend)** | Gradle 8.x (wrapper included) |
| **Database** | PostgreSQL ( `dev` profile ) or in-memory H2 ( `local` profile, default ) |
| **Frontend** | React 18, TypeScript, React Router |
| **Build (frontend)** | Vite 5, ESLint |
| **API style** | JSON REST, RFC 9457 Problem Details for errors |

---

## Ticket status state machine

New tickets start in **`OPEN`**. Terminal states **`CLOSED`** and **`CANCELLED`** accept no further lifecycle events. Status cannot be set via `PATCH`; clients call `PATCH /api/v1/tickets/{id}/status` with a **transition event**. Illegal pairs return HTTP **409** with code **`TICKET_ILLEGAL_TRANSITION`** (see [`TicketStateMachine`](./backend/src/main/java/com/tothenew/ticket/domain/TicketStateMachine.java)).

### Transition matrix (enforced in the backend)

Rows = current status, columns = event. A cell shows the **resulting status** after a successful transition, or **—** if the transition is rejected.

| From \\ Event | `START_PROGRESS` | `RESOLVE` | `CLOSE` | `CANCEL` |
| --- | --- | --- | --- | --- |
| `OPEN` | `IN_PROGRESS` | — | — | `CANCELLED` |
| `IN_PROGRESS` | — | `RESOLVED` | — | `CANCELLED` |
| `RESOLVED` | — | — | `CLOSED` | — |
| `CLOSED` | — | — | — | — |
| `CANCELLED` | — | — | — | — |

### Allowed lifecycle paths

| From | Event | To |
| --- | --- | --- |
| `OPEN` | `START_PROGRESS` | `IN_PROGRESS` |
| `OPEN` | `CANCEL` | `CANCELLED` |
| `IN_PROGRESS` | `RESOLVE` | `RESOLVED` |
| `IN_PROGRESS` | `CANCEL` | `CANCELLED` |
| `RESOLVED` | `CLOSE` | `CLOSED` |

Happy-path flow: `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`. Cancellation is allowed from `OPEN` or `IN_PROGRESS` only. Reopening (e.g. `CLOSED` → `OPEN`) is **not** supported in v1.

Full normative rules, guards, and invariants: [`spec/state-machine.md`](./spec/state-machine.md).

---

## Prerequisites

- **JDK 21** (matches the Gradle Java toolchain in [`backend/build.gradle`](./backend/build.gradle))
- **Node.js** 18+ and **npm** (for the frontend)
- **PostgreSQL** 14+ (only if you run the backend with the `dev` profile)

---

## Environment setup and secrets

Secrets and environment-specific values live in **`.env` files**, which are **gitignored**. Never commit real passwords or production URLs. Use the committed **`.env.example`** files as templates.

### Backend (`backend/.env`)

On startup, [`DotenvLoader`](./backend/src/main/java/com/tothenew/ticket/config/DotenvLoader.java) loads `backend/.env` (when you run from `backend/`) or `backend/.env` relative to the repo root, and injects keys into JVM system properties so Spring can resolve `${...}` placeholders. Existing OS environment variables take precedence over `.env`.

1. Copy the template:

   ```bash
   cp backend/.env.example backend/.env
   ```

2. Set variables as needed:

   | Variable | Purpose |
   | --- | --- |
   | `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | PostgreSQL connection (required for `dev` profile) |
   | `CORS_ALLOWED_ORIGINS` | Comma-separated browser origins allowed to call `/api/**` (e.g. `http://localhost:5173`) |
   | `H2_USER`, `H2_PASSWORD` | Optional credentials for the in-memory H2 `local` profile |

3. Choose a Spring profile (see [Running the backend](#running-the-backend)):
   - **`local`** (default in [`application.yml`](./backend/src/main/resources/application.yml)): H2 in-memory DB, `ddl-auto: update`, H2 console at `/h2-console`.
   - **`dev`**: PostgreSQL with `ddl-auto: validate`; all `POSTGRES_*` variables must be set.

### Frontend (`frontend/.env`)

Vite loads variables prefixed with `VITE_`. The dev server **requires** `VITE_API_PROXY_TARGET` so `/api` requests are proxied to the backend (avoids CORS during local development).

1. Copy the template:

   ```bash
   cp frontend/.env.example frontend/.env
   ```

2. Typical local values (already in `.env.example`):

   | Variable | Purpose |
   | --- | --- |
   | `VITE_API_BASE_URL` | Leave **empty** in dev so the app calls same-origin `/api` (proxied by Vite). Set to the full API origin (no trailing slash) for production builds. |
   | `VITE_API_PROXY_TARGET` | Backend origin for the Vite proxy, e.g. `http://localhost:8080` |

---

## Running locally

Run the **backend** and **frontend** in separate terminals.

### Running the backend

From the `backend` directory:

```bash
cd backend
cp .env.example .env   # if you have not already
./gradlew bootRun
```

- Default profile: **`local`** (H2). API base: **http://localhost:8080/api/v1**
- To use PostgreSQL instead:

  ```bash
  export SPRING_PROFILES_ACTIVE=dev
  ./gradlew bootRun
  ```

  Ensure `backend/.env` contains valid `POSTGRES_*` values and the database exists.

On first startup with an empty database, [`DatabaseSeeder`](./backend/src/main/java/com/tothenew/ticket/config/DatabaseSeeder.java) inserts sample users for assignee pickers. The API uses a development [`RequestContext`](./backend/src/main/java/com/tothenew/ticket/application/DevRequestContext.java) (fixed requester UUID) until full authentication is wired per the spec.

**Verify the API** (example):

```bash
curl -s http://localhost:8080/api/v1/tickets | head
```

**Run backend tests:**

```bash
cd backend
./gradlew test
```

### Running the frontend

From the `frontend` directory:

```bash
cd frontend
cp .env.example .env   # if you have not already
npm install
npm run dev
```

- Dev server: **http://localhost:5173**
- The Vite config ([`vite.config.ts`](./frontend/vite.config.ts)) proxies **`/api`** to `VITE_API_PROXY_TARGET` (default `http://localhost:8080`). Start the backend **before** using the UI.

**Production-style frontend build:**

```bash
cd frontend
npm run build
npm run preview
```

Set `VITE_API_BASE_URL` to your deployed API origin when building for production.

---

## API overview

Detailed contract: [`spec/api-contract.md`](./spec/api-contract.md).

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/tickets` | Create ticket (initial status `OPEN`) |
| `GET` | `/api/v1/tickets` | List with pagination, optional `status` and `search` |
| `GET` | `/api/v1/tickets/{id}` | Get one ticket |
| `PATCH` | `/api/v1/tickets/{id}` | Update title, description, priority, assignee (not status) |
| `PATCH` | `/api/v1/tickets/{id}/status` | Apply a transition event (`START_PROGRESS`, `RESOLVE`, `CLOSE`, `CANCEL`) |
| `GET` / `POST` | `/api/v1/tickets/{id}/comments` | List or add comments |
| `GET` | `/api/v1/users` | List users (for assignee selection) |

---

## Specifications and further reading

- [Requirements](./spec/requirements.md) — actors, scope, and requirement IDs (e.g. TCK-05 state machine)
- [Architecture](./spec/architecture.md) — layers, boundaries, deployment view
- [Data model](./spec/data-model.md) — entities and persistence
- [UI flows](./spec/ui-flow.md) — frontend behavior aligned with the API
- [Test strategy](./spec/test-strategy.md) — unit vs integration testing

---

## License

Internal / project-specific — add license text here if applicable.
