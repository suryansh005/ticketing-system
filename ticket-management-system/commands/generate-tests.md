# Command: Generate tests from specifications

Use this prompt after a spec is ready (or when implementing a spec slice). Tests must fail or pass against the spec, not against guessed Spring internals.

## Prompt

```
You generate tests for the ticket-management-system from specifications.

Stack: Java 21, Spring Boot (JUnit 5, Mockito, Spring Boot Test as needed), React/Next.js for UI tests only if the spec has UI acceptance criteria.

Read first:
- The feature spec under /spec (source of truth)
- rules/testing.md
- rules/api-standards.md
- rules/java-springboot.md
- Existing test patterns in the repo (match packages, assertion library, Testcontainers)

Rules:
- One test (or parameterized row) per acceptance criterion and per state-machine cell that the spec defines.
- Name tests from spec language and requirement IDs (e.g. TCK-12).
- Domain and state-machine tests: pure unit tests, no Spring context.
- HTTP mapping, validation, Problem Details: @WebMvcTest or equivalent against the real exception advice once it exists.
- Persistence, optimistic locking, replay: integration tests as in rules/testing.md.
- Mock only ports (repository, clock, publisher). Do not mock the aggregate or records.
- Do not invent endpoints, statuses, or transitions that are not in the spec.
- If the spec is incomplete, list missing cases and generate tests only for what is specified.
- Do not add dependencies unless the project already uses them.
- No secrets in fixtures.

Deliver:
1. Test plan table: criterion ID → test class#method → unit | slice | integration | UI
2. Source files matching project layout
3. Parameterized sources copied from spec transition tables (allow and deny)
4. Notes for tests that cannot be written yet (missing production ports)

If production code is missing, still write the tests against the intended public API described in the spec (package names from existing code if present).
```

## Agent checklist

- [ ] Allow and deny transitions covered
- [ ] Authz denial tests if spec defines actors
- [ ] Validation and 409 illegal transition tests for APIs
- [ ] No Thread.sleep
- [ ] Test plan included
