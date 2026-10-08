# CLAUDE.md

> ## ⚠️ This Android app is DEPRECATED — the product is moving to React Native (2026-10-08)
>
> **Rebuilding the app?** Start at **[`MIGRATION_SPEC.md`](MIGRATION_SPEC.md)**.
> **Maintaining the Kotlin app?** Only live-user crashes are in scope. No refactors, no
> bug-fix sweeps.

**Read `AGENTS.md` first and follow it.**

`AGENTS.md` is the PROJECT MANAGER agent for this repository. It defines the project overview,
the conventions you must follow, the team/module agent hierarchy under `agents/`, the routing
table, and the delegation protocol.

Do not bypass it. When a request arrives:

1. Act as the PM defined in `AGENTS.md`.
2. Log the request in `agents/TASKS.md` with date & time (IST).
3. Route to the owning team lead (`agents/<TEAM>/<TEAM>_LEAD.md`), then to the module agent
   (`agents/<TEAM>/<MODULE>.md`).
4. Update the module agent's `.md` (including its Change log) in the same change as the code.

## Document map

| File | Purpose |
|---|---|
| `MIGRATION_SPEC.md` | **The React Native rebuild spec** — constraints, traps, 44 endpoints, build order |
| `AGENTS.md` | PM agent — project snapshot, org chart, routing, protocol |
| `agents/<TEAM>/<TEAM>_LEAD.md` | 10 team leads |
| `agents/<TEAM>/<MODULE>.md` | 44 module agents — per-screen detail |
| `agents/BUG_NOTES.md` | Consolidated defect register (851 entries) |
| `agents/TASKS.md` | Dated task board |

