# Java 21 and Spring Boot Best Practices

Use these rules for all backend work. Prefer the smallest change that matches existing package layout and naming.

## Java 21 language features

- Target **Java 21** (LTS). Do not add preview features unless the build already enables them.
- Prefer **records** for immutable DTOs, value objects, and API request/response payloads.
- Use **sealed interfaces/classes** for closed domain hierarchies (ticket statuses, event types, error kinds). Pair with exhaustive `switch`.
- Use **pattern matching for switch** and **record patterns** instead of nested `instanceof` + casts.
- Use **text blocks** for SQL, JSON fixtures, and multi-line messages. Keep interpolation explicit; do not hide business data in concatenated strings.
- Prefer **virtual threads** for I/O-bound Spring MVC/WebFlux adapters only when the team has enabled them in the runtime (`spring.threads.virtual.enabled` or equivalent). Do not mix blocking JDBC on platform-thread pools without measuring.
- Use `SequencedCollection` / `List.getFirst()` / `getLast()` only when the type is already ordered; do not change collection semantics just to use new APIs.
- Keep `Optional` out of domain fields and entity mappings. Use it for return types of lookups that may miss.

### Prefer

```java
public sealed interface TicketEvent permits TicketCreated, TicketAssigned, TicketClosed {}

public record TicketCreated(UUID ticketId, String title) implements TicketEvent {}

public String describe(TicketEvent event) {
    return switch (event) {
        case TicketCreated(var id, var title) -> "Created %s (%s)".formatted(id, title);
        case TicketAssigned assigned -> "Assigned to %s".formatted(assigned.assigneeId());
        case TicketClosed closed -> "Closed at %s".formatted(closed.closedAt());
    };
}
```

### Avoid

- Mutable JavaBeans DTOs with getters/setters when a record is sufficient.
- `switch` on strings of status codes when a sealed type or enum exists.
- Catching `Exception` and returning `null`.

## Spring Boot application structure

- One bounded context per package under the application root (for example `ticket`, `user`, `workflow`). Do not dump everything into `controller` / `service` / `repository` at the root.
- Keep **controllers thin**: map HTTP ↔ application commands/queries. Put business rules in domain or application services.
- Inject collaborators through constructors. Never use field `@Autowired`.
- Configuration belongs in `@Configuration` classes or `application.yml`. Do not hardcode URLs, credentials, or feature flags.
- Use **constructor injection + `final` fields**. Prefer records for configuration properties with `@ConfigurationProperties`.
- Transactions: `@Transactional` on application services that mutate state, never on controllers or repositories unless a custom repository method needs it. Read-only queries use `@Transactional(readOnly = true)`.
- Persistence: JPA entities are not API models. Map entity ↔ domain/DTO at the adapter boundary.
- Validation: Bean Validation (`jakarta.validation`) on request records at the controller. Domain invariants stay in the domain model (not only in annotations).
- Observability: structured logs with correlation/request IDs. Never log tokens, passwords, or PII beyond what policy allows.
- Time: store and serialize **UTC** (`Instant`). Convert to zone only at the UI or reporting edge.

## REST in Spring MVC

- `@RestController` + `@RequestMapping` at type level with a versioned prefix (`/api/v1/...`).
- Use `@Valid` / `@Validated` on request bodies and query objects.
- Return `ResponseEntity` only when status or headers vary. Otherwise return the body and let `@ResponseStatus` or problem handling set status.
- Map domain exceptions to HTTP via `@ControllerAdvice` and the error payload in `rules/api-standards.md`.
- Idempotency: `PUT` and `DELETE` must be safe to retry. Create-with-client-key flows use `Idempotency-Key` when the spec requires it.
- Pagination: `page`, `size`, `sort` query params; return a consistent page envelope (see API standards).
- Do not expose persistence IDs as sequential integers if the spec requires opaque IDs; prefer UUID.

## Security defaults

- Authenticate every non-public endpoint. Authorize by role **and** resource ownership where tickets are user-scoped.
- CSRF: follow Spring Security defaults for cookie sessions; stateless JWT APIs disable CSRF only when using Bearer tokens.
- CORS: explicit allowed origins. Never `* ` with credentials.
- Mass assignment: never bind entities from `@RequestBody`. Allow-list DTO fields.
- SQL: parameterized queries / Spring Data only. No string-concatenated JPQL from user input.

## Next.js / BFF boundary

- Browser clients call the Next.js app or a documented BFF. Do not scatter Spring CSRF cookies across third-party origins without a spec.
- Keep Java services as JSON APIs. UI routing, RSC, and session cookies stay in Next.js unless the spec says otherwise.

## Checklist before merge

- [ ] Java 21 constructs used where they simplify (records, sealed types, pattern switch)
- [ ] No business logic in controllers
- [ ] Validation + problem responses per API standards
- [ ] Transactions and mapping boundaries respected
- [ ] No secrets in code or logs
