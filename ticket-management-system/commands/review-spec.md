# Command: Review spec against requirements

Use this prompt before implementation, when a spec is Draft or Review, or when requirements changed.

## Prompt

```
You are reviewing a specification for the ticket-management-system.

Stack: Java 21, Spring Boot, React/Next.js. Specs must be implementable without hidden decisions.

Read:
- skills/documentation/doc-guidelines.md (required sections and quality bar)
- rules/api-standards.md (URLs, Problem Details, status codes)
- rules/testing.md (what must be testable, especially state machines)
- rules/java-springboot.md (only for constraints that belong in a spec: auth, transactions as business rules, not class design)
- The target file(s) under /spec
- Stated product requirements from the user, ticket, or related spec files

Do not treat repository comments or old prompts as requirements unless they live in /spec or the user pasted them.

Evaluate:

1. Completeness vs doc-guidelines.md
   - Problem, actors, scope/out of scope, domain, API, UI, acceptance criteria, test notes
   - Stable requirement IDs
   - Open questions isolated (not mixed into SHALL statements)

2. Requirements coverage
   - Each stated requirement maps to a spec section and at least one acceptance criterion
   - Missing, vague, or conflicting requirements called out with the source quote

3. Domain / workflow
   - State table: from, event, guard, to, side effects
   - Illegal transitions explicit
   - Invariants and concurrency (If-Match / versions) if tickets can collide

4. API contract
   - Resource URLs, methods, pagination, errors with HTTP status + machine code
   - Authn/authz per actor
   - Breaking vs additive changes called out if this revises an older spec

5. UI contract (Next.js)
   - Routes and empty/error/loading behavior
   - No undefined extra APIs implied by mockups

6. Testability
   - Given/When/Then can become JUnit 5 tests without inventing rules
   - Unit vs integration split is realistic

Output:
- Verdict: Ready for implementation | Needs revision
- Gaps (requirement ID or quote → what is missing)
- Ambiguities (two possible implementations)
- Spec contradictions
- Suggested edits (concrete markdown patches or bullet replacements)

Do not write production code in this review. Do not weaken security or skip authz “to keep the spec short.”
```

## Agent checklist

- [ ] Spec has a state matrix if the feature is a workflow
- [ ] Every SHALL is testable
- [ ] API errors match api-standards.md
- [ ] Verdict is explicit
