# Command: Review code against project rules

Use this prompt when the user asks to review a diff, PR, or local changes. Follow `/rules` and the relevant `/spec` files. Do not praise style that violates those documents.

## Prompt

```
You are reviewing code for the ticket-management-system.

Stack: Java 21, Spring Boot, React/Next.js. Workflow is spec-driven.

Before commenting, read:
- rules/java-springboot.md
- rules/testing.md
- rules/api-standards.md
- skills/documentation/doc-guidelines.md
- The feature spec(s) under /spec that this change claims to implement

Review only the actual diff (plus necessary surrounding context). Ignore unrelated files.

Produce findings in this order:

1. Spec alignment
   - Missing, extra, or contradictory behavior vs /spec
   - State-machine transitions, guards, and error codes vs spec tables
   - API paths, methods, status codes, and Problem Details vs rules/api-standards.md

2. Java / Spring Boot (rules/java-springboot.md)
   - Records, sealed types, thin controllers, transactions, mapping boundaries
   - Validation, security (authn/authz, no mass assignment), no secrets in logs

3. Tests (rules/testing.md)
   - Acceptance criteria covered
   - State-machine allow/deny matrix
   - Mocks only at boundaries; no sleep-based tests

4. Next.js / UI (if present)
   - Uses the documented API contract; handles 401/403/404/409/problem+json
   - No business rules duplicated that belong on the server unless the spec says so

5. Security and quality
   - Injection, CSRF/CORS, IDOR on ticket IDs
   - Breaking API changes without version bump

Severity:
- Blocker: incorrect behavior, security issue, spec contradiction, missing tests for new rules
- Major: maintainability or standard violation that should be fixed before merge
- Minor: nits

For each finding: file, location, severity, what is wrong, what “good” looks like (cite the rule or spec ID).

If there are no blockers, say so. Do not invent issues. Do not request secrets.
```

## Agent checklist

- [ ] Opened the matching spec
- [ ] Compared HTTP and domain behavior to rules
- [ ] Listed blockers first
- [ ] Suggested the smallest fix, not a rewrite
