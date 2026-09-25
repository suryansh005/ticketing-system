### Phase 3 Review: Meaningful AI Mistake Identified

**Issue Detected:**
During the implementation of Phase 3 (Core REST APIs), the AI generated the state transition endpoint as `POST /{ticketId}/transitions`. 

**Why it is a mistake:**
While `POST` is sometimes used for RPC-style state changes, this directly violates the Spec-Driven Development (SDD) contract we established. Our `/spec/api-contract.md` and the Phase 3 prompt explicitly mandated using a partial update method: `PATCH /api/v1/tickets/{id}/status`. 

**Impact if not fixed:**
If the backend changes the endpoint structure and HTTP method without updating the specification, the frontend team (or any API consumer) will build against the wrong URL, causing integration failures. 

**Resolution:**
Rejected the AI's initial Controller suggestion and prompted Cursor to refactor the endpoint to strictly adhere to the `PATCH` specification defined in the API contract.

--------

### Phase 5 Review: Meaningful AI Limitation Identified

**Issue Detected:**
During the Phase 5 Frontend Setup, the AI generated the React API service to hit `http://localhost:8080`, but it completely failed to update the Spring Boot backend to allow Cross-Origin Resource Sharing (CORS). 

**Why it is a mistake:**
Without `@CrossOrigin` on the Spring Boot controller (or a proxy configured in the frontend bundler), the browser's Same-Origin Policy will block all requests from the React app (port 3000/5173) to the Spring Boot app (port 8080). The AI lacked the cross-boundary context to update the backend while generating the frontend.

**Resolution:**
Explicitly prompted the AI to update the backend Controller to allow CORS.

--------
### Phase 6 Review: Meaningful AI Mistake Identified (JPA Lifecycle & ID Generation)

**Issue Detected:**
Encountered a `StaleObjectStateException` when attempting to save a newly created ticket. 

**Why it is a mistake:**
The AI originally designed the system to generate the `UUID` in the domain layer (`TicketService`) before saving it to the database, but it incorrectly left the `@GeneratedValue(strategy = GenerationType.UUID)` annotation on the `TicketEntity`. 

Because the entity already had an ID assigned when passed to `ticketRepository.save()`, Spring Data JPA assumed the entity already existed in the database and called `merge()` instead of `persist()`. Since the record did not actually exist, the optimistic locking mechanism (`@Version`) failed, throwing the `StaleObjectStateException: Row was updated or deleted by another transaction`.

**Resolution:**
Prompted the AI with the error stack trace. The AI correctly diagnosed the `persist()` vs `merge()` issue and refactored the code to implement Spring Data's `Persistable<UUID>` interface. This allowed us to manually override the `isNew()` method using a transient flag, forcing JPA to execute an `INSERT` statement even when the ID is pre-populated by the domain layer.

--------
### Phase 3 & 5 Review: Meaningful AI Omission Identified (Dropped Context)

**Issue Detected:**
During the frontend integration phase, I discovered that the system lacked the ability to update standard ticket fields (title, description, priority, assignee) and add comments. 

**Why it is a mistake (Context Dropping):**
The AI suffered from "instruction ignoring" or "context dropping." Even though updating these specific fields and adding comments were explicitly listed in the Core Acceptance Criteria and the original requirements prompt, the AI only focused on the state machine transition (`PATCH /status`) during Phase 3. It completely failed to generate the general update API and the corresponding frontend UI.

**Impact if not fixed:**
The application would fail the core acceptance criteria, leaving users unable to edit tickets or collaborate via comments once a ticket was created.

**Resolution:**
I manually cross-referenced the generated code with the Acceptance Criteria, identified the gap, and issued targeted prompts to force the AI to:
1. Generate the missing `TicketUpdateRequest` DTO and `PUT /api/v1/tickets/{ticketId}` endpoint in the Spring Boot backend.
2. Update the React API service and `TicketDetail` components to include forms for inline editing of these omitted fields.

-------
### Phase 6 Review: Meaningful AI Domain Modeling Gap (Assignee Entity)

**Issue Detected:**
The system lacked a proper relational model for ticket assignees. The AI implemented the "assignee" field as a raw string/UUID on the ticket without a corresponding `User` entity to validate against.

**Why it is a mistake (Domain Modeling Gap):**
While the AI fulfilled the literal requirement to "Update... assignee", it failed to apply standard relational database and API design. An assignee must reference a valid, existing system user. Treating it as a free-text field or unverified UUID breaks data integrity and makes it impossible to build a proper frontend UI (like a user dropdown).

**Impact if not fixed:**
* **Backend:** Data integrity is compromised. Invalid, non-existent, or malformed IDs could be saved to tickets.
* **Frontend:** Terrible user experience. Users would be forced to manually type raw UUIDs or names into a text box instead of selecting an agent from a pre-populated dropdown.

**Resolution:**
I identified this architectural flaw and issued targeted prompts to correct the domain model:
1. Instructed the AI to create a `User` JPA entity, repository, and a `GET /api/v1/users` endpoint.
2. Added a database seeder to populate dummy users on backend startup.
3. Added backend validation in `TicketService` to ensure the `assigneeId` actually exists in the database before saving or updating a ticket.
4. Refactored the React frontend to fetch the user list on mount and replaced the raw text inputs with a `<select>` dropdown bounded to valid users.