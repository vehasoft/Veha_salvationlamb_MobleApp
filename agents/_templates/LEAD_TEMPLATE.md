# <TEAM> TEAM LEAD Agent

> Reports to: **PROJECT MANAGER** (`AGENTS.md`)
> Manages: the module agents listed below.

---

## 1. Charter

One paragraph: what this team is responsible for in the product, and where its boundary ends.

## 2. Modules owned

| Module agent | Screen / unit | Source files | Status |
|---|---|---|---|
| `<MODULE>.md` | <screen> | <paths> | READY / PLANNED |

## 3. Intra-team flows

How the team's screens hand off to each other (navigation, extras, state).

## 4. Shared state touched by this team

| Store / global | Keys / fields | Read | Written |
|---|---|---|---|

## 5. API surface used by this team

| Endpoint | Method | Used by |
|---|---|---|

## 6. Cross-team dependencies

| Needs | Owner team | Escalate to PM when |
|---|---|---|

## 7. Delegation rules

- Which module agent handles which kind of request.
- What must be escalated to PM instead of handled locally.

## 8. Definition of done (team-level)

- [ ] Code follows the conventions in `AGENTS.md` §5.
- [ ] The module agent's `.md` is updated in the same change (incl. its Change log).
- [ ] Cross-team impact reported to PM.
- [ ] `agents/TASKS.md` updated by PM with date & time.

## 9. Team-level known issues

| # | Issue | Module | Risk |
|---|---|---|---|

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial team lead agent. |
