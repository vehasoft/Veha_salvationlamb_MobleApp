# NOTIFICATIONS TEAM LEAD Agent

> Reports to: **PROJECT MANAGER** (`AGENTS.md`)
> Manages: the 3 module agents below. **Status: 3/3 READY.**
> Created by **T-026** (2026-10-08) — this code arrived in the 123 commits between
> `master` (v1.1) and `salvation_lamb_permissions_final_1` (v1.2.0) and had **no agent**.

---

## 1. Charter

The NOTIFICATIONS team owns **everything that tells a user something happened**: the Firebase
Cloud Messaging pipeline (token registration → `FirebaseMessagingService` → system tray →
deep link), the in-app notification centre with its three role-scoped tabs, and the
read/unread lifecycle of a notification record.

**Boundary:** the team owns the notification *list* and the *routing decision*, but **not the
destination screens** — tapping a notification hands off to FEED (`ViewPostActivity`), PROFILE
(`ViewProfileActivity`, `ApproveRequestActivity`), MEDIA (`PdfActivity2`, `WebViewActivity`) or
APPSHELL (`NoPermissionActivity`). It also does **not** own the FCM token write at login
(AUTH/`LOGIN.md`) nor the `fcmToken` DataStore key (PLATFORM/`STORAGE.md`).

> **Migration note (RN):** this whole team maps to `@react-native-firebase/messaging` plus one
> list screen. The **routing table in §3 is the part worth porting carefully** — it encodes six
> notification types, four permission gates and three different extras shapes.

---

## 2. Modules owned

| Module agent | Screen / unit | Source files | LOC | Status |
|---|---|---|---|---|
| `NOTIFICATION_CENTER.md` | Notification centre shell + tabs | `activity/NotificationViewActivity.kt`, `adapter/NotificationTabAdapter.kt`, `res/layout/activity_notification_view.xml` | 122 | READY |
| `NOTIFICATION_LISTS.md` | The 3 tab bodies + row adapter | `fragments/UserNotificationFragment.kt`, `AdminNotificationFragment.kt`, `WarriorNotificationFragment.kt`, `adapter/NotificationListAdapter.kt`, 3 fragment layouts + `child_notification_list.xml` | 718 | READY |
| `PUSH_SERVICE.md` | FCM receive + system tray | `service/NotificationService.java`, `service/NotificationHelper.java` | 129 | READY |

**Total owned: ~969 LOC across 8 source files + 5 layouts.**

> **Shared-file warning:** `child_notification_list.xml` and `NotificationListAdapter.ViewHolder`
> are **also used by ANNOUNCEMENTS** (`AnnouncementAdapter` inflates this team's layout and
> returns this team's `ViewHolder`). Any change to the row layout's view ids breaks
> `AnnouncementAdapter` silently at runtime. Notify ANNOUNCEMENTS via PM before touching it.

---

## 3. The notification routing table (the core contract)

Six `NotificationType` values, resolved in **two places that must stay in sync**:
`NotificationListAdapter.onBindViewHolder` (in-app tap) and `NotificationHelper.displayNotification`
(system-tray tap). A third partial copy lives in `SplashScreenActivity.getMyDetails` (cold-start
deep link) — **three copies total, see `N-1`**.

| `type` | Permission gate | Destination | Extras |
|---|---|---|---|
| `post` | `POST` / `Read` | `ViewPostActivity` | `postId`, `type` |
| `user` | `USER` / `Read` ¹ | `ViewProfileActivity` | `userId` |
| `warrior` | `USER` / `Edit` | `ApproveRequestActivity` | `userId` |
| `announcement` | `ANNOUNCEMENT` / `Read` | `ViewPostActivity` | `postId`, `type` |
| `file` | **none** | `PdfActivity2` | `fileName`, `url` |
| `event` | **none** | `WebViewActivity` | `url` |

¹ gate applied in the in-app adapter but **not** in `NotificationHelper` — see `N-2`.

A denied gate routes to `NoPermissionActivity` (APPSHELL) in-app; in the tray it leaves
`pendingIntent == null`, producing a **dead notification** (`N-3`).

---

## 4. Shared state touched by this team

| Store / global | Keys / fields | Read | Written |
|---|---|---|---|
| DataStore `SalvationLamb` | `token` | every list fetch + read-receipt | — |
| DataStore `SalvationLamb` | `userId` | list fetch (`Util.userId` fallback) | — |
| DataStore `SalvationLamb` | `fcmToken` | `NotificationService.updateToken` reads | **never written — see `N-4`** |
| `Util.userId` | — | list fetch, token payload | — |
| `Util.user.role` | — | decides whether the Warrior tab exists | — |
| `Util.permissionMap` | via `Util.hasPermission` | 4 gates in-app, 3 in the tray | — |
| `Util.CHANNEL_ID` | `"VEHA"` | notification builder | — |

---

## 5. API surface used by this team

| Endpoint | Method | Used by |
|---|---|---|
| `api/v1/notifications/{userId}?page&size&type` | GET | all 3 list fragments (`type` = `user` \| `admin` \| `warrior`) |
| `api/v1/notifications/count/{userId}` | GET | `MainActivity` badge (APPSHELL calls it, we own the meaning) |
| `api/v1/notifications/{id}` | PUT | read receipt (`{"isVisited": true}`) |
| `api/v1/users/token/update` | PUT | **AUTH owns the call site** (`LoginActivity`); this team owns the token source |

**Response envelope:** `{ "notification": [...], "count": N }` — note the key is
**`notification`**, singular, unlike the `results` used by FEED/ANNOUNCEMENTS and the `result`
used by AUTH. Fourth distinct envelope in the app (extends `CL-5`).

---

## 6. Cross-team dependencies

| Needs | Owner team | Escalate to PM when |
|---|---|---|
| `ViewPostActivity` | FEED | the `postId`/`type` extras contract changes |
| `ViewProfileActivity`, `ApproveRequestActivity` | PROFILE | the `userId` extra changes |
| `PdfActivity2`, `WebViewActivity` | MEDIA | the `url`/`fileName` extras change |
| `NoPermissionActivity` | APPSHELL | the denial destination changes |
| `Util.hasPermission`, `CHANNEL_ID` | PLATFORM | any permission-type rename |
| `fcmToken` key | PLATFORM/STORAGE | the token lifecycle is fixed (`N-4`) |
| Notification badge in `MainActivity` | APPSHELL | the count endpoint or refresh timing changes |
| `child_notification_list.xml` | **shared with ANNOUNCEMENTS** | always — both teams must agree |

---

## 7. Delegation rules

- Anything about **receiving** a push, the channel, or the tray → `PUSH_SERVICE.md`.
- Anything about the **tabs, roles or the shell** → `NOTIFICATION_CENTER.md`.
- Anything about **a row, the list, paging or read state** → `NOTIFICATION_LISTS.md`.
- A **new notification type** touches all three modules *and* `SplashScreenActivity` (AUTH) —
  always a PM-coordinated task, never module-local.
- Changing `child_notification_list.xml` → PM, because ANNOUNCEMENTS depends on it.

---

## 8. Definition of done (team-level)

- [ ] Code follows the conventions in `AGENTS.md` §5.
- [ ] The module agent's `.md` is updated in the same change (incl. its Change log).
- [ ] If the routing table changed, **all three copies** (§3) updated together.
- [ ] Cross-team impact reported to PM.
- [ ] `agents/TASKS.md` updated by PM with date & time.

---

## 9. Team-level known issues

| # | Issue | Module | Risk |
|---|---|---|---|
| N-1 | The routing table is **triplicated** (`NotificationListAdapter`, `NotificationHelper`, `SplashScreenActivity`) and already inconsistent | all + AUTH | **High** — a new type silently works in one place only |
| N-2 | `NotificationHelper` applies **no permission gate** to `type=user`, while the in-app adapter does | PUSH_SERVICE | **High (security)** — the tray bypasses a gate the UI enforces |
| N-3 | A denied gate in the tray leaves `pendingIntent == null`, so the notification posts but **does nothing on tap** | PUSH_SERVICE | **High (UX)** |
| N-4 | `NotificationService.updateToken` builds a payload and **never sends it**; `savefcmToken` is commented out. A rotated FCM token is **never registered** | PUSH_SERVICE | **High** — users silently stop receiving push |
| N-5 | `NotificationViewActivity` calls `MainActivity().getMyDetails()` — a manually constructed Activity with no Context (same bug class as the one fixed in T-025) | NOTIFICATION_CENTER | **High** — always throws; the null-user guard does nothing |
| N-6 | `Util.user.role` is read **one line after** the null check that was meant to protect it → NPE on a cold deep-link | NOTIFICATION_CENTER | **High (crash)** |
| N-7 | `NotificationTabAdapter.getItem` ends `else -> b as Fragment` with `b = null` (same P0 as `TabAdapter`/`SearchAdapter`) | NOTIFICATION_CENTER | **High (crash)** |
| N-8 | All 3 fragments are ~170-line near-identical copies differing only in the `type` string and a layout id | NOTIFICATION_LISTS | Medium — fix one, forget two |
| N-9 | `notificationManagerCompat.notify(1, …)` uses a **constant id**, so every push overwrites the previous one | PUSH_SERVICE | Medium |
| N-10 | Broken `\|\|` token guard (CL-1) in all 3 fragments | NOTIFICATION_LISTS | **High** |
| N-11 | Response envelope key is `notification` (singular) — a 4th shape in the app | all | Medium (`CL-5`) |
| N-12 | No notification channel is ever **created** by this team; `Util.CHANNEL_ID` is used by the builder but `createNotificationChannel` lives in `LoginActivity` (AUTH) | PUSH_SERVICE | **High on API 26+** — pushes are dropped if that path was skipped |

---

## 10. Change log

| Change | Detail |
|---|---|
| Created (T-026, 2026-10-08) | Team created to cover 8 previously unowned source files (~969 LOC) added by the v1.2.0 branch. Documented the 6-type routing table and its 3 inconsistent copies, the 4th API envelope shape, the FCM pipeline, and 12 team-level issues — including the **never-sent token update** (`N-4`), the **ungated `user` deep link in the tray** (`N-2`), and the missing channel creation (`N-12`). |

