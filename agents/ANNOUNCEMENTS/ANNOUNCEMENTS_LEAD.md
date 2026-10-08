# ANNOUNCEMENTS TEAM LEAD Agent

> Reports to: **PROJECT MANAGER** (`AGENTS.md`)
> Manages: the 1 module agent below. **Status: 1/1 READY.**
> Created by **T-026** (2026-10-08) — this code arrived in the 123 commits between
> `master` (v1.1) and `salvation_lamb_permissions_final_1` (v1.2.0) and had **no agent**.

---

## 1. Charter

The ANNOUNCEMENTS team owns the **admin broadcast channel**: a read-only, reverse-chronological
list of announcements published by administrators, reachable from the `MainActivity` header.

It is the smallest team in the app — **2 files, ~246 LOC** — and the only one that owns no
layout of its own beyond the host activity's: the list rows are **borrowed from the
NOTIFICATIONS team** (see §6, this is the team's defining constraint).

**Boundary:** the team owns the **list**; it does **not** own the announcement detail view —
tapping a row opens FEED's `ViewPostActivity` with `type="announcement"`. It also does not own
the entry button in `MainActivity` (APPSHELL) nor the `ANNOUNCEMENT` permission type (PLATFORM).

> **Migration note (RN):** this is the easiest screen in the app to port — one paged
> `FlatList`, one `GET`, no writes, no local state. Two things must be preserved: the
> **`results` + `count` envelope with its page-size-10 contract**, and the fact that the row
> renders `"<b>{author name}</b>  {title}"` as **HTML**, not plain text. The RN version should
> *not* reproduce the borrowed-ViewHolder coupling described in §6 — give it its own row
> component.

---

## 2. Modules owned

| Module agent | Screen / unit | Source files | LOC | Status |
|---|---|---|---|---|
| `ANNOUNCEMENT_LIST.md` | Announcements list | `activity/AnnouncementActivity.kt` (158), `adapter/AnnouncementAdapter.kt` (90), `res/layout/activity_announcement.xml` | 248 | READY |

---

## 3. Intra-team flows

```
MainActivity header "announcement" button           [APPSHELL]
   |
   |-- gate: Util.hasPermission(ANNOUNCEMENT, Read)
   |     |-- denied ------------------> NoPermissionActivity     [APPSHELL]
   |     `-- granted -----------------> AnnouncementActivity     [this team]
   |
   `-> AnnouncementActivity
          |-- GET api/v1/announcements?page&size=10  (paged on scroll)
          |-- empty  ------------------> "No Data found" empty state
          |-- logo tap ----------------> MainActivity            [APPSHELL]
          |-- close tap ---------------> finish()
          `-- row tap
                |-- gate: Util.hasPermission(ANNOUNCEMENT, Read)   <-- gated AGAIN
                |     |-- denied ------> NoPermissionActivity     [APPSHELL]
                |     `-- granted -----> ViewPostActivity         [FEED]
                |                          extras: type="announcement", postId
                `-- (401) -------------> LoginActivity            [AUTH]
```

**The same gate is evaluated twice** — once to enter the screen and once per row tap. Harmless,
but it means a permission revoked mid-session still shows the list while blocking every row
(`A-5`).

---

## 4. Shared state touched by this team

| Store / global | Keys / fields | Read | Written |
|---|---|---|---|
| DataStore `SalvationLamb` | `token` | every page fetch | — |
| DataStore `SalvationLamb` | `token`, `userId` | — | **deleted** in the (dead) `else` of the broken guard |
| `Util.permissionMap` | via `Util.hasPermission` | the row-tap gate | — |
| `Util.userId`, `Util.user` | — | **not read at all** | — |

This team writes **nothing** to global state in normal operation — it is purely a reader.

---

## 5. API surface used by this team

| Endpoint | Method | Used by |
|---|---|---|
| `api/v1/announcements?page={n}&size=10` | GET | `AnnouncementActivity.getAnnouncenents()` *(sic — the method name is misspelled in the source)* |
| `api/v1/announcements/{postId}` | GET | **declared in `RetrofitAPI.kt` but never called anywhere in the app** — the detail view goes through FEED's post endpoint instead (`A-6`) |

**Response envelope:** `{ "results": [...], "count": N }` — the standard array shape, shared with
FEED and PROFILE. Note the rows are parsed into **`Posts`**, not into the `Announcement` data
class that exists in `DataModels.kt` and is **entirely unused** (`A-4`).

---

## 6. Cross-team dependencies

| Needs | Owner team | Escalate to PM when |
|---|---|---|
| **`child_notification_list.xml`** | **NOTIFICATIONS** | **always — see the warning below** |
| **`NotificationListAdapter.ViewHolder`** | **NOTIFICATIONS** | **always — see the warning below** |
| `ViewPostActivity` + the `type`/`postId` extras | FEED | when the detail contract changes |
| The announcement button in `MainActivity` | APPSHELL | when the entry point or its gate changes |
| `NoPermissionActivity` | APPSHELL | when the denial destination changes |
| `LoginActivity` | AUTH | when the 401 contract changes |
| `Posts` model, `Util.hasPermission`, `RetrofitAPI`, `UserPreferences` | PLATFORM | always |

> ### ⚠ This team has no row of its own — it borrows NOTIFICATIONS'
>
> `AnnouncementAdapter` is declared:
>
> ```kotlin
> class AnnouncementAdapter(...) : RecyclerView.Adapter<NotificationListAdapter.ViewHolder>()
> ```
>
> * it inflates **`R.layout.child_notification_list`** (owned by NOTIFICATIONS), and
> * it returns **`NotificationListAdapter.ViewHolder`** from `onCreateViewHolder` — another
>   team's type is part of this team's public signature.
>
> It also defines a **private `ViewHolder` class of its own that is never used** — dead code that
> makes the coupling easy to miss on a casual read.
>
> **Consequences:** a NOTIFICATIONS change to the row's view ids breaks this team **at runtime,
> not at compile time**. The mirror of this warning lives in
> `agents/NOTIFICATIONS/NOTIFICATION_LISTS.md` §13. Neither team may change the shared layout or
> the `ViewHolder` field set without PM coordination and agreement from the other.

---

## 7. Delegation rules

- Everything in this team goes to `ANNOUNCEMENT_LIST.md` — there is only one module.
- A change to the **row layout or `ViewHolder`** is **never** module-local: PM + NOTIFICATIONS.
- A change to what the **detail view** renders is a **FEED** task (`ViewPostActivity`), not ours.
- Adding a *write* operation (create / edit / delete an announcement) would be a **new module**
  and a new permission (`ANNOUNCEMENT`/`Create`) — PM decision; today the client is read-only
  even though the permission model already defines `Create`, `Edit` and `Delete` for this type.

---

## 8. Definition of done (team-level)

- [ ] Code follows the conventions in `AGENTS.md` §5.
- [ ] `ANNOUNCEMENT_LIST.md` updated in the same change set (incl. its Change log).
- [ ] If the shared row was touched, **NOTIFICATIONS signed off** and their doc updated too.
- [ ] Cross-team impact reported to PM.
- [ ] `agents/TASKS.md` updated by PM with date & time.

---

## 9. Team-level known issues

| # | Issue | Module | Risk |
|---|---|---|---|
| A-1 | **The adapter borrows NOTIFICATIONS' layout *and* `ViewHolder` type** (§6); a row-id change there breaks this team at runtime, not compile time | ANNOUNCEMENT_LIST | **High** |
| A-2 | The always-true `\|\|` token guard (`CL-1`) — the session-lost `else` is dead code and a missing token is sent as `Bearer null` | ANNOUNCEMENT_LIST | **High** |
| A-3 | Paging is copied from the notification lists and carries the same three defects: the scroll listener is attached **inside `onResponse`**, `notifyItemRangeInserted` is called with the **post-insert** size, and the accumulating `postlist` is re-appended on every page | ANNOUNCEMENT_LIST | **High** |
| A-7 | Non-401 errors, `onFailure` and the offline path show the user **nothing** (`CL-7`); `onFailure`'s log tag even reads `HomeFragment.getMyDetails` | ANNOUNCEMENT_LIST | **High** |
| A-8 | HTTP 401 starts `LoginActivity` with **no `finish()` and no session clear**, so back returns to a dead screen and the next launch repeats the 401 | ANNOUNCEMENT_LIST | **High** |
| A-4 | Rows are parsed into **`Posts`**, while the purpose-built `Announcement` data class in `DataModels.kt` is **never used anywhere** | ANNOUNCEMENT_LIST / PLATFORM | Medium |
| A-6 | `GET api/v1/announcements/{postId}` is declared in `RetrofitAPI.kt` but **never called** — the detail view uses FEED's post endpoint instead | PLATFORM / NETWORK | Medium |
| A-5 | The `ANNOUNCEMENT`/`Read` gate is evaluated **twice** (entry + per row); a mid-session revocation leaves the list visible but every row blocked | ANNOUNCEMENT_LIST | Low |
| A-9 | `AnnouncementAdapter` takes a `LifecycleOwner` parameter it never uses, and creates a `UserPreferences` in `onCreateViewHolder` that it also never uses | ANNOUNCEMENT_LIST | Low |
| A-10 | The activity carries **8 unused imports** (`NotificationListAdapter`, `NotificationList`, `LifecycleOwner`, …) — residue of the copy-paste from the notification list | ANNOUNCEMENT_LIST | Cosmetic |
| A-11 | The fetch method is spelled **`getAnnouncenents()`** in the source | ANNOUNCEMENT_LIST | Cosmetic |

---

## 10. Change log

| Change | Detail |
|---|---|
| Created (T-026, 2026-10-08) | Team created to cover 2 previously unowned source files (~248 LOC) added by the v1.2.0 branch. Documented the read-only broadcast flow, the double `ANNOUNCEMENT`/`Read` gate, the `results` + `count` envelope, and 11 team-level issues. Headline finding: this team **owns no row of its own** — `AnnouncementAdapter` inflates NOTIFICATIONS' `child_notification_list.xml` *and* declares `RecyclerView.Adapter<NotificationListAdapter.ViewHolder>`, putting another team's type in its public signature, with a dead private `ViewHolder` left behind to obscure it (`A-1`). Also recorded that the purpose-built `Announcement` model and the `announcements/{postId}` endpoint both exist and are **both unused** (`A-4`, `A-6`). |

