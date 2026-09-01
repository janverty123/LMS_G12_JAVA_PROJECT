# Domain Docs

Classify uses a single-context domain-documentation layout.

## Before exploring

- Read root `CONTEXT.md` when it exists.
- Read relevant decisions under `docs/adr/` when that directory exists.
- Continue silently when either is absent. Domain-modeling workflows create them only when terminology or architectural decisions are resolved.

## Vocabulary

Use terms defined in `CONTEXT.md` consistently in code, tests, issues, and plans. If a needed concept is missing, reconsider whether an existing term applies; otherwise record the gap for domain modeling.

## Decisions

Surface conflicts with an existing ADR explicitly, naming the ADR and explaining why the decision might need reconsideration. Do not silently override it.
