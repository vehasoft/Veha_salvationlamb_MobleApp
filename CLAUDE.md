# CLAUDE.md

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
