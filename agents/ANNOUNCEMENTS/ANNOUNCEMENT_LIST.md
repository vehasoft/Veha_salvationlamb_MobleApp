# ANNOUNCEMENT_LIST Module Agent

> Team: **ANNOUNCEMENTS** · Reports to: `agents/ANNOUNCEMENTS/ANNOUNCEMENTS_LEAD.md`
> Scope: this agent knows **every detail** of this screen — views, actions, API, storage,
> validation, messages, navigation. It may only edit the files under **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **Announcements** — a paged, read-only list of admin broadcasts |
| Class | `com.veha.activity.AnnouncementActivity` (158 LOC) + `com.veha.adapter.AnnouncementAdapter` (90 LOC) |
| Layout | `res/layout/activity_announcement.xml` · row **`child_notification_list.xml` — owned by NOTIFICATIONS** (§13) |
| Manifest entry | `<activity android:name=".AnnouncementActivity" android:exported="false" />` |
| Entered from | the `announcement` button in `MainActivity`'s header (APPSHELL), gated on `ANNOUNCEMENT`/`Read` — **the only entry point** |
| Exits to | `ViewPostActivity` (FEED) · `NoPermissionActivity` (APPSHELL) · `MainActivity` (logo) · `LoginActivity` (AUTH, on 401) |

## 2. UI inventory

| View id | Type | Text / hint | Notes |
|---|---|---|---|
| `@id/header_main` | header container | — | standard app header, not bound |
| `@id/prod_logo` | `ImageView` | — | → `logo`; tap → `MainActivity` (**no `finish()`**, `AL8`) |
| `@id/close` | `ImageButton` | — | → `close`; `finish()` |
| `@id/announcement_recycler` | `RecyclerView` | — | → `list`; `LinearLayoutManager`, `AnnouncementAdapter` |
| `@id/no_data` | `LinearLayout`, `visibility="gone"` | — | → `nodata`; empty state, shown when `postlist.size <= 0` |

### Row — `child_notification_list.xml` (⚠ **borrowed from NOTIFICATIONS**)

| View id | Bound as | Used here for |
|---|---|---|
| `@id/notification_list_linear` | `ViewHolder.notificationLayout` | click target |
| `@id/profile_pic_fol` | `ViewHolder.profilePic` | author avatar (Picasso, fallback `ic_profile`) |
| `@id/notification_content` | `ViewHolder.notificationContent` | **repurposed** to hold `"<b>{author}</b>  {title}"` |
| `@id/time_ago` | `ViewHolder.notificationtime` | **`visibility = View.GONE`** — announcements show no timestamp |

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| `onCreate` | `setContentView` → `UserPreferences(this)` → bind 4 views → wire `logo` / `close` → build `AnnouncementAdapter(ArrayList(), this, this)` → `LinearLayoutManager` → `getAnnouncenents()` |
| Scroll to bottom (`!canScrollVertically(1)`) | if `(count + 2) > page` → `getAnnouncenents()` and `updated = false`. The listener is attached **inside `onResponse`**, so one is added per page (`AL3`) |
| `logo` tap | `startActivity(MainActivity)` — **no `finish()`, no `CLEAR_TOP`** (`AL8`) |
| `close` tap | `finish()` |
| Row tap | gate `ANNOUNCEMENT`/`Read` → `ViewPostActivity` with `type="announcement"` + `postId`; else `NoPermissionActivity` |

## 4. Validation rules

| Field | Rule | Failure message |
|---|---|---|
| — | none — the screen has no input | — |

## 5. API contracts

### `getAnnouncenents(postlist)` *(spelling as in the source — `AL13`)*

| Item | Value |
|---|---|
| Retrofit method | `getAnnouncements(head, page, size)` |
| HTTP | `GET api/v1/announcements?page={page}&size=10` |
| Base URL source | `Util.getRetrofit()` → `https://server.salvationlamb.com` |
| Headers | `Authorization: Bearer <token>` from the `token` DataStore key |
| Request body | none |
| Success (200) | `{ "results": [...], "count": N }`. Each element is parsed as **`Posts`** (not `Announcement` — `AL11`); `count /= 10` becomes the page ceiling; rows appended by `adapter.addItem(postlist)` guarded by `if (!updated)`, then `page++` |
| Empty | `list` → `GONE`, `nodata` → `VISIBLE`. **No `page == 1` check** (unlike the notification lists), so a later empty page hides an already-populated list (`AL5`) |
| Error | **401** → toast `@string/Deleted_account` + `LoginActivity`, **no `finish()`, no session clear** (`AL7`). Other codes → `Log.e("failAnnouncements - Status", …)` only (`AL6`) |
| Failure (`onFailure`) | `Log.e("HomeFragment.getMyDetails", "fail")` — **copy-pasted tag from a different class**, nothing shown to the user (`AL6`, `AL12`) |
| Guard | the always-true `\|\|` form (`CL-1`) — the `else` branch below is **dead code** (`AL2`) |

`GET api/v1/announcements/{postId}` exists in `RetrofitAPI.kt` but is **never called** (`A-6`);
the detail view goes through FEED's post endpoint instead.

## 6. Storage read / written

| Key | Type | Operation | Value |
|---|---|---|---|
| `token` | `String` | read (continuous observer inside the fetch) | `Bearer $it` |
| `token`, `userId` | — | **delete** | only in the dead `else` branch (`AL2`) |

## 7. Global state touched

| Field | Operation | Value |
|---|---|---|
| `Util.permissionMap` | read via `Util.hasPermission` | the row-tap gate (`ANNOUNCEMENT`/`Read`) |
| — | — | nothing is written; `Util.userId` and `Util.user` are not read |

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| HTTP 401 | `@string/Deleted_account` → `"Your account is removed\n please contact administrator"` | `Toast.LENGTH_LONG` |
| Dead `else` branch | `"Somthing Went Wrong \nLogin again to continue"` | `Toast` — hard-coded, **misspelled**, and **unreachable** (`AL2`, `AL14`) |
| Empty list | `"No Data found"` | static `TextView` in `no_data` |
| Any other error, `onFailure`, or offline | **nothing** | — (`AL6`) |

## 9. Navigation map

| From | Trigger | To | Extras | finish()? |
|---|---|---|---|---|
| `MainActivity` header | `ANNOUNCEMENT`/`Read` granted | **`AnnouncementActivity`** | none | no |
| this | row tap + gate granted | `ViewPostActivity` | `type="announcement"`, `postId` | no |
| this | row tap + gate denied | `NoPermissionActivity` | none | no |
| this | logo tap | `MainActivity` | none | **no** (`AL8`) |
| this | close tap | — (back) | — | yes |
| this | HTTP 401 | `LoginActivity` | none | **no** (`AL7`) |

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | §3 |
| `onDestroy` | **not overridden** — the Retrofit call is never cancelled (`CL-9`), the `authToken` observer is never removed, and the per-page scroll listeners are never detached |
| rotation | re-runs `onCreate`, resets `page = 1` and refetches from scratch |

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `RetrofitAPI.getAnnouncements` | PLATFORM/NETWORK | the list |
| `Posts` model | PLATFORM/DATA_MODELS | row data (**not** `Announcement` — `AL11`) |
| `UserPreferences` | PLATFORM/STORAGE | token |
| `Util.hasPermission` | PLATFORM/COMMONS | row gate |
| `Commons().isNetworkAvailable` | PLATFORM/COMMONS | offline guard |
| Picasso | 3rd-party | author avatar |
| **`child_notification_list.xml`** | **NOTIFICATIONS** | **the row layout** |
| **`NotificationListAdapter.ViewHolder`** | **NOTIFICATIONS** | **the adapter's type parameter** |
| `ViewPostActivity` | FEED | detail view |
| `NoPermissionActivity` | APPSHELL | denial |
| `MainActivity` | APPSHELL | entry point + logo destination |
| `LoginActivity` | AUTH | 401 destination |

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| AL1 | **The adapter has no row of its own**: `AnnouncementAdapter : RecyclerView.Adapter<NotificationListAdapter.ViewHolder>` inflates NOTIFICATIONS' `child_notification_list.xml` and returns **their** `ViewHolder`. A row-id change in that team breaks this screen **at runtime, not compile time** (`A-1`) | **High** | give this team its own `child_announcement.xml` + `ViewHolder` |
| AL2 | The always-true `\|\|` token guard (`CL-1`) — the whole session-lost `else` (toast + token delete + `LoginActivity`) is **unreachable**, and a missing token is sent as `Bearer null` | **High** | use `&&` |
| AL3 | `addOnScrollListener` is attached **inside `onResponse`**, so page *n* adds the *n*-th listener and all of them fire at the bottom | **High** | attach once in `onCreate` |
| AL4 | `addItem` calls `notifyItemRangeInserted(announcements.size, post.size)` with the **post-insert** size, so the start index is wrong by `post.size` | **High** | capture `oldSize` before `addAll` |
| AL9 | `getAnnouncenents()` recurses with its **default empty `postlist`** on scroll, but the adapter already holds the previous pages — combined with `AL4` the list can duplicate or misrender rows | **High** | pass the accumulated list explicitly, or let the adapter own it |
| AL6 | Non-401 errors, `onFailure` and the offline path show the user **nothing** (`CL-7`) | **High** | toast + retry affordance |
| AL7 | HTTP 401 starts `LoginActivity` with **no `finish()` and no session clear** | **High** | adopt the `bailToLogin` pattern from `SplashScreenActivity` (T-024) |
| AL5 | The empty-state check has **no `page == 1` guard** (the notification lists do), so an empty later page hides an already-populated list | Medium | add the page check |
| AL11 | Rows are parsed into **`Posts`**; the purpose-built `Announcement` data class is never used anywhere in the app | Medium | use it, or delete it from `DataModels.kt` |
| AL8 | Logo tap starts `MainActivity` with **no `finish()` or `CLEAR_TOP`**, stacking duplicates | Medium | `CLEAR_TOP` + `finish()` |
| AL10 | `AnnouncementAdapter` takes a `LifecycleOwner` it never uses, and builds a `UserPreferences` in `onCreateViewHolder` that it also never uses | Low | drop both |
| AL15 | `AnnouncementAdapter` declares a **private `ViewHolder` class that is never used** — it obscures the fact that another team's `ViewHolder` is the real one | Low | delete it (or make it the real one, fixing `AL1`) |
| AL16 | `count` is declared `var count: Int` **outside** the callback and assigned inside it — it is read by the scroll listener, so a second page can read a stale value | Medium | capture per response |
| AL12 | `onFailure` and the `catch` both log with the tag `HomeFragment.getMyDetails` | Low | use `<Class>.<method>` |
| AL13 | The method is spelled **`getAnnouncenents`** | Cosmetic | rename |
| AL14 | Hard-coded, misspelled `"Somthing Went Wrong \nLogin again to continue"` (shared with 5 other files) | Cosmetic | app-wide string fix |
| AL17 | 8 unused imports (`NotificationListAdapter`, `NotificationList`, `LifecycleOwner`, …) — residue of the copy-paste from the notification list | Cosmetic | remove |

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/AnnouncementActivity.kt` | **exclusive** |
| `app/src/main/java/com/veha/adapter/AnnouncementAdapter.kt` | **exclusive** |
| `app/src/main/res/layout/activity_announcement.xml` | **exclusive** |
| `<activity android:name=".AnnouncementActivity">` in `AndroidManifest.xml` | shared with PLATFORM/BUILD_CONFIG |

> ### ⚠ The row is NOT owned by this team
>
> `res/layout/child_notification_list.xml` and `NotificationListAdapter.ViewHolder` belong to
> **NOTIFICATIONS** (`agents/NOTIFICATIONS/NOTIFICATION_LISTS.md` §13). This module only
> *consumes* them — and consumes them in an unusually tight way, because the borrowed
> `ViewHolder` appears in `AnnouncementAdapter`'s own type signature.
>
> * **Never edit the row layout from here.** Escalate to PM; NOTIFICATIONS must agree.
> * If they rename `notification_content`, `time_ago`, `profile_pic_fol` or
>   `notification_list_linear`, **this screen crashes at runtime** with no compile error.
> * The clean fix is `AL1`: give this team its own row and stop borrowing.

**Not owned (escalate):** `RetrofitAPI.kt`, `DataModels.kt` (`Posts`, `Announcement`),
`UserPreferences.kt`, `Util.java`, `Commons.kt` (PLATFORM); `ViewPostActivity` (FEED);
`MainActivity` and its announcement button, `NoPermissionActivity` (APPSHELL);
`LoginActivity` (AUTH).

## 14. How to make common changes

**Give the team its own row (`AL1` — highest value):** copy `child_notification_list.xml` to
`child_announcement.xml`, declare a real `AnnouncementAdapter.ViewHolder` (the unused private
class at line 35 is already there, `AL15`), and switch the adapter's type parameter. This
**removes the only cross-team runtime coupling** this team has, and is entirely local once the
new layout exists. It also lets the row drop the hidden `time_ago` view.

**Fix paging (`AL3`/`AL4`/`AL9`/`AL16`):** attach the scroll listener once in `onCreate`, capture
`oldSize` before `addAll`, stop passing a fresh default `postlist` on the recursive call, and
read `count` from the response rather than a shared `var`. All four are local to
`AnnouncementActivity` + `AnnouncementAdapter.addItem`.

**Fix the token guard (`AL2`):** change `||` to `&&` — this makes the existing session-lost
branch (toast → delete token → `LoginActivity`) reachable for the first time. Part of the
app-wide `CL-1` sweep; safe to do in isolation here.

**Change what a row shows:** the text is composed in `onBindViewHolder` as
`"<b>" + user.name + "</b>  " + title` and rendered with `Html.fromHtml`. Composition is owned
here; the **views it writes into are not** (§13).

**Add announcement creation/editing:** out of scope for this module — the client is read-only
today even though the permission model defines `ANNOUNCEMENT`/`Create`, `Edit` and `Delete`.
That is a new module and a PM decision (see lead §7).

## 15. Change log

| Change | Detail |
|---|---|
| Created (T-026, 2026-10-08) | Documented the announcements list from `AnnouncementActivity.kt` (158), `AnnouncementAdapter.kt` (90) and `activity_announcement.xml`, previously unowned since the v1.2.0 branch. Captured the single entry point and its double `ANNOUNCEMENT`/`Read` gate, the `results` + `count` paging contract, the `Html.fromHtml` row composition, and the hidden `time_ago` view. **17 issues** recorded. Headline: `AL1` — the adapter **borrows NOTIFICATIONS' row layout *and* `ViewHolder` type**, putting another team's class in its public signature and creating a runtime-only breakage path, while a private unused `ViewHolder` sits in the file obscuring it (`AL15`). Paging is inherited wholesale from the notification lists and carries four defects (`AL3`, `AL4`, `AL9`, `AL16`); the empty-state check is **missing the `page == 1` guard** those lists have (`AL5`); and the `CL-1` guard makes the entire session-lost branch unreachable (`AL2`). Also confirmed the `Announcement` model and the `announcements/{postId}` endpoint are **both dead code**. |


