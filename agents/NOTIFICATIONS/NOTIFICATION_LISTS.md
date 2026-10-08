# NOTIFICATION_LISTS Module Agent

> Team: **NOTIFICATIONS** · Reports to: `agents/NOTIFICATIONS/NOTIFICATIONS_LEAD.md`
> Scope: this agent knows **every detail** of this screen — views, actions, API, storage,
> validation, messages, navigation. It may only edit the files under **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **The three notification tabs** — User / Admin / Warrior, each a paged list of notification rows inside `NotificationViewActivity` |
| Class | `com.veha.fragments.UserNotificationFragment` (170 LOC) · `AdminNotificationFragment` (175) · `WarriorNotificationFragment` (174) — **three near-identical copies** (`N-8`) — plus `com.veha.adapter.NotificationListAdapter` (199 LOC) and its `ViewHolder` |
| Layout | `res/layout/fragment_user_notification.xml` · `fragment_admin_notification.xml` · `fragment_warrior_notification.xml` (33 lines each, byte-identical except the RecyclerView id and `tools:context`) · row `res/layout/child_notification_list.xml` (**shared with ANNOUNCEMENTS — see §13**) |
| Manifest entry | none (fragments) |
| Entered from | `NotificationTabAdapter.getItem(0\|1\|2)` — the **only** instantiation site. All 2–3 are created eagerly because the pager is a `FragmentPagerAdapter` (`NC10`) |
| Exits to | `ViewPostActivity` (FEED) · `ViewProfileActivity`, `ApproveRequestActivity` (PROFILE) · `PdfActivity2`, `WebViewActivity` (MEDIA) · `NoPermissionActivity` (APPSHELL) · `LoginActivity` (AUTH, on 401 or a missing token) |

> **This single module documents all three fragments.** They are copies; §2a is the only place
> they differ. Everything else in this document applies verbatim to all three.

## 2. UI inventory

| View id | Type | Text / hint | Notes |
|---|---|---|---|
| (root) | `FrameLayout`, `match_parent` | — | not bound. **The `RecyclerView` and the empty state are stacked in a `FrameLayout`**, so they overlap rather than exclude each other — visibility is toggled manually |
| `@id/notification_user_recycler` (User) / `@id/notification_admin_recycler` (Admin **and Warrior**) | `RecyclerView`, `layout_height="wrap_content"`, `marginTop=10dp`, `@color/white` | — | → `list`; `LinearLayoutManager(activity)`, adapter `NotificationListAdapter`. `wrap_content` height on a paged list defeats RecyclerView recycling (`NL13`) |
| `@id/no_data` | `LinearLayout` vertical, `gravity=center`, `visibility="gone"` | — | → `nodata`; shown only when `postlist.size <= 0 && page == 1` |
| (no id) | `ImageView` 150×150dp, `@drawable/ic_no_record`, `margin=20dp` | — | empty-state illustration |
| (no id) | `TextView`, `textSize="30dp"`, `@color/black`, `gravity=center` | **`"No Data found"`** | hard-coded English, and **`30dp` not `sp`** (`NL14`) |

### Row — `child_notification_list.xml` (⚠ shared with ANNOUNCEMENTS)

| View id | Type | Bound as | Notes |
|---|---|---|---|
| (root) | `LinearLayout` vertical | — | holds the row + a 1dp `@color/primary_blue` divider `LinearLayout` |
| `@id/notification_list_linear` | `LinearLayout` horizontal, `padding=5dp`, `marginTop=5dp` | `ViewHolder.notificationLayout` | the **click target** and the read/unread background |
| `@id/profile_pic_fol` | `ImageView` inside two nested 45dp `CardView`s (`cardCornerRadius="250dp"`), `scaleType=centerCrop`, default `@drawable/ic_profile` | `ViewHolder.profilePic` | id is copied from `child_follower.xml` (`_fol` suffix) — PROFILE's row (`NL15`) |
| `@id/notification_content` | `TextView`, `15dp`, `@color/black`, `paddingStart=10dp` | `ViewHolder.notificationContent` | rendered through `Html.fromHtml` |
| `@id/time_ago` | `TextView`, `15dp`, `@color/hintcolor` | `ViewHolder.notificationtime` | `Util.getTimeAgo(createdAt)`; **ANNOUNCEMENTS hides this view** |

Two commented-out `findViewById` lines (`name_fol`, `profilePic_fol`) remain in the `ViewHolder`.
The nested `CardView` inside an identical `CardView` is redundant (`NL16`), and the row carries
`app:layout_constraint*` attributes inside a `LinearLayout` parent, where they do nothing (`NL17`).

### 2a. What actually differs between the three fragments

| | `UserNotificationFragment` | `AdminNotificationFragment` | `WarriorNotificationFragment` |
|---|---|---|---|
| LOC | 170 | 175 | 174 |
| Layout inflated | `fragment_user_notification` | `fragment_admin_notification` | `fragment_warrior_notification` |
| RecyclerView id | `notification_user_recycler` | `notification_admin_recycler` | **`notification_admin_recycler`** (its own layout reuses the admin id) |
| `tools:context` | `UserNotificationFragment` | `AdminNotificationFragment` | **`AdminNotificationFragment`** (wrong — copy-paste) |
| `type` query param | `"user"` | `"admin"` | `"warrior"` |
| `size` query param | **10** | **10** | **50** (`NL5`) |
| Paging denominator | `count /= 10` | `count /= 10` | **`count /= 10`** — but the page size is 50 (`NL5`) |
| `page++` in the scroll listener | **absent** (only the `if (!updated)` block increments) | **present** — so a page bump happens **twice** | **present** — same double bump |
| Unused imports | `PostUser`, `NotificationType` | `Posts`, `UserRslt`, `PostUser`, `NotificationType` | — |
| Everything else | — | identical | identical |

**That is the entire delta: a layout name, a view id, one string, one number, and one stray
`page++`.** ~515 LOC of duplication (`N-8` / `NL1`).

---

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| `onCreateView` | `updated = false` → inflate → `contexts = container!!.context` (**force-unwrap**, `NL9`) → `UserPreferences(contexts)` → bind `list` + `nodata` → **`page = 1`** (overriding the `private var page: Int = 0` field initialiser) → build `NotificationListAdapter(ArrayList(), contexts, this)` → `LinearLayoutManager(activity)` → `getNotifications(viewLifecycleOwner)` |
| Scroll to bottom (`!canScrollVertically(1)`) | if `(count + 2) > page` → `getNotifications(owner)` again and `updated = false`. The listener is added **inside `onResponse`**, so a new one is attached per page (`NL3`) |
| Row tap (`notification_list_linear`) | `readNotification(holder, id)` then the 6-way `type` switch (§3a) |

### 3a. Row routing switch (`NotificationListAdapter.onBindViewHolder`)

Fires **after** `readNotification`, so the read receipt is sent even when the gate denies.

| `notification.type` | Gate | Destination | Extras |
|---|---|---|---|
| `post` | `POST`/`Read` | `ViewPostActivity` | `type`, `postId` = `notification.data` |
| `user` | `USER`/`Read` | `ViewProfileActivity` | `userId` = `notification.data` |
| `warrior` | `USER`/`Edit` | `ApproveRequestActivity` | `userId` = `notification.data` |
| `announcement` | `ANNOUNCEMENT`/`Read` | `ViewPostActivity` | `type`, `postId` = `notification.data` |
| `file` | **none** | `PdfActivity2` | `fileName` = `data`, `url` = `fileUrl` |
| `event` | **none** | `WebViewActivity` | **`pageUrl`** = `data` |
| anything else | — | **nothing happens** — no `else`, the tap is silently swallowed (`NL10`) |

A denied gate starts `NoPermissionActivity` (APPSHELL). `file` and `event` are **ungated**
(`NL11`), and `WebViewActivity` is passed `pageUrl` here but `url` from the splash deep link —
**two different extra names for the same destination** (`NL12`).

## 4. Validation rules

| Field | Rule | Failure message |
|---|---|---|
| — | none — this screen has no input | — |

## 5. API contracts

### `getNotifications(owner, postlist)`

| Item | Value |
|---|---|
| Retrofit method | `getNotifications(head, userId, page, size, type)` |
| HTTP | `GET api/v1/notifications/{userId}?page={page}&size={size}&type={user\|admin\|warrior}` |
| Base URL source | `Util.getRetrofit()` → `https://server.salvationlamb.com` |
| Headers | `Authorization: Bearer <token>` from the `token` DataStore key |
| Request body | none |
| Success (200) | `{ "notification": [ NotificationList… ], "count": N }` — key is **singular** (`N-11`). `count /= 10` becomes the page ceiling; rows are appended via `adapter.addItem(postlist)` guarded by `if (!updated)` |
| Error | **401** → toast `@string/Deleted_account` + `LoginActivity` (**no `finish()`, no session clear**, `NL7`). Any other code → two `Log.e` calls only, **no user feedback** (`NL8`) |
| Failure (`onFailure`) | `Log.e("HomeFragment.getMyDetails", "fail")` — **copy-pasted tag from a different class**, and nothing shown to the user (`NL8`, `NL19`) |

### `readNotification(holder, id)` — in the adapter

| Item | Value |
|---|---|
| Retrofit method | `putReadNotification(head, id, data)` |
| HTTP | `PUT api/v1/notifications/{id}` |
| Request body | `{"isVisited": true}` |
| Success (200) | `data.remove("isVisited")` (mutates the shared body — the **second** call for the same holder sends `{}`, `NL6`) then repaints the row white |
| Error / failure | logged only |
| Guard | uses the **correct `&&`** form here, unlike the fragments' `\|\|` (`NL2`) |

## 6. Storage read / written

| Key | Type | Operation | Value |
|---|---|---|---|
| `token` | `String` | read (continuous observer) | `Bearer $it` for both calls |
| `userId` | `String` | read | only when `Util.userId == null`; assigns `Util.userId` |
| `token`, `userId` | — | **delete** | in the (dead) `else` of the broken guard |

## 7. Global state touched

| Field | Operation | Value |
|---|---|---|
| `Util.userId` | read, and **written** from the DataStore observer when null | path param of the list call |
| `Util.permissionMap` | read via `Util.hasPermission` | 4 of the 6 routing gates |
| `Util.getTimeAgo(createdAt)` | read | row timestamp |

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| HTTP 401 | `@string/Deleted_account` → `"Your account is removed\n please contact administrator"` | `Toast.LENGTH_LONG` |
| Broken-guard `else` (**dead code**) | `"Somthing Went Wrong \nLogin again to continue"` | `Toast` — hard-coded and **misspelled** (`NL18`) |
| Empty list | `"No Data found"` | static `TextView` |
| Any other HTTP error, or `onFailure`, or offline | **nothing** | — (`NL8`) |

## 9. Navigation map

| From | Trigger | To | Extras | finish()? |
|---|---|---|---|---|
| row | `type=post` + gate | `ViewPostActivity` | `type`, `postId` | no |
| row | `type=user` + gate | `ViewProfileActivity` | `userId` | no |
| row | `type=warrior` + gate | `ApproveRequestActivity` | `userId` | no |
| row | `type=announcement` + gate | `ViewPostActivity` | `type`, `postId` | no |
| row | `type=file` | `PdfActivity2` | `fileName`, `url` | no |
| row | `type=event` | `WebViewActivity` | `pageUrl` | no |
| row | gate denied | `NoPermissionActivity` | none | no |
| list | HTTP 401 | `LoginActivity` | none | **no** (`NL7`) |

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | overridden, calls `super` only — dead override |
| `onCreateView` | everything (§3). **All state is rebuilt per view creation**, so a tab revisit refetches page 1 while the adapter keeps the old rows |
| `onDestroyView` / `onDestroy` | **not overridden** — the Retrofit call is never cancelled (`CL-9`) and the scroll listeners are never removed |

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `RetrofitAPI.getNotifications`, `putReadNotification` | PLATFORM/NETWORK | list + read receipt |
| `NotificationList`, `PostUser` models | PLATFORM/DATA_MODELS | row data |
| `UserPreferences` | PLATFORM/STORAGE | token, userId |
| `Util.hasPermission`, `Util.getTimeAgo` | PLATFORM/COMMONS | gates, timestamps |
| `Commons().isNetworkAvailable` | PLATFORM/COMMONS | offline guard |
| Picasso | 3rd-party | avatar loading |
| `ViewPostActivity` / `ViewProfileActivity` / `ApproveRequestActivity` / `PdfActivity2` / `WebViewActivity` / `NoPermissionActivity` | FEED / PROFILE / MEDIA / APPSHELL | destinations |

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| NL1 | **Three ~170-line near-identical fragments** (`N-8`); the only real differences are a layout id, the `type` string and the page size | **High** | one `NotificationListFragment` with `newInstance(type)` + a `Bundle` |
| NL2 | All three fragments use the always-true `\|\|` token guard (`CL-1`, `N-10`) — the session-lost `else` is dead and a missing token sends `Bearer null` | **High** | use `&&`, as `readNotification` already does |
| NL3 | `addOnScrollListener` is called **inside `onResponse`**, so page *n* attaches the *n*-th listener; all of them fire at the bottom | **High** | attach once in `onCreateView` |
| NL4 | `addItem` calls `notifyItemRangeInserted(notifications.size, post.size)` using the **post-insert** size, so the start index is wrong by `post.size` | **High** | capture `oldSize` before `addAll` |
| NL5 | `WarriorNotificationFragment` requests `size = 50` but still divides `count / 10`, so the page ceiling is **5x too high** and it refetches empty pages | **High** | divide by the page size actually used |
| NL21 | `AdminNotificationFragment` and `WarriorNotificationFragment` increment `page` **twice** per fetch (in `if (!updated)` and again in the scroll listener) — every other page is skipped | **High** | increment in one place |
| NL7 | HTTP 401 starts `LoginActivity` without `finish()` and **without clearing the session**, so back returns to a dead screen and the next launch repeats the 401 | **High** | mirror the `bailToLogin` pattern added to `SplashScreenActivity` in T-024 |
| NL8 | Non-401 errors, `onFailure` and the offline path show the user **nothing** (`CL-7`) | **High** | toast + retry affordance |
| NL6 | `readNotification` mutates its shared `data` body with `data.remove("isVisited")` on success | Medium | build the body per call |
| NL9 | `contexts = container!!.context` force-unwraps the container | Medium | use `requireContext()` |
| NL10 | The routing switch has **no `else`** — an unknown or newly added `type` makes the row inert | Medium | log and fall back to a detail screen |
| NL11 | `file` and `event` rows are routed with **no permission gate**, unlike the other four types | Medium | gate them, or document why not |
| NL12 | `WebViewActivity` receives `pageUrl` here but `url` from `SplashScreenActivity` — one destination, two extra names | Medium | pick one and fix both call sites |
| NL13 | The `RecyclerView` is `wrap_content` inside a `FrameLayout`, defeating recycling on a paged list | Medium | `match_parent` |
| NL14 | `"No Data found"` is hard-coded English at `textSize="30dp"` (should be `sp`) | Low | string resource + `sp` |
| NL15 | Row view ids are copied from PROFILE's follower row (`profile_pic_fol`) | Low | rename jointly with ANNOUNCEMENTS |
| NL16 | `child_notification_list.xml` nests a 45dp `CardView` inside an identical `CardView` | Low | flatten |
| NL17 | The row carries `app:layout_constraint*` attributes inside a `LinearLayout`, where they are inert | Low | delete |
| NL19 | Log tags read `HomeFragment.getMyDetails` in all three notification fragments | Low | use `<Class>.<method>` |
| NL18 | `"Somthing Went Wrong \nLogin again to continue"` — hard-coded and misspelled (shared with 5 other files) | Cosmetic | app-wide string fix |
| NL20 | Unused imports: `PostUser` + `NotificationType` (User); `Posts` + `UserRslt` + `PostUser` + `NotificationType` (Admin) | Cosmetic | remove |

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/fragments/UserNotificationFragment.kt` | **exclusive** |
| `app/src/main/java/com/veha/fragments/AdminNotificationFragment.kt` | **exclusive** |
| `app/src/main/java/com/veha/fragments/WarriorNotificationFragment.kt` | **exclusive** |
| `app/src/main/java/com/veha/adapter/NotificationListAdapter.kt` | **shared — see the warning below** |
| `res/layout/fragment_user_notification.xml`, `fragment_admin_notification.xml`, `fragment_warrior_notification.xml` | **exclusive** |
| `res/layout/child_notification_list.xml` | **shared — see the warning below** |

> ### ⚠ Shared with ANNOUNCEMENTS — read before editing
>
> `AnnouncementAdapter` (owned by the **ANNOUNCEMENTS** team) has **no row of its own**:
>
> * it inflates **our** `R.layout.child_notification_list`, and
> * it is declared `RecyclerView.Adapter<NotificationListAdapter.ViewHolder>` — it returns
>   **our `ViewHolder` type** from `onCreateViewHolder` (it even defines a `ViewHolder` class of
>   its own that is never used).
>
> Consequences:
> * renaming or removing any id in `child_notification_list.xml` makes **`AnnouncementAdapter`
>   crash at runtime**, not at compile time;
> * changing the `ViewHolder` field set breaks their bind method;
> * they hide `time_ago` and repurpose `notification_content` for an announcement title.
>
> **Any change to either shared file must go through PM and be agreed with ANNOUNCEMENTS.**

**Not owned (escalate):** `RetrofitAPI.kt`, `DataModels.kt`, `UserPreferences.kt`, `Util.java`,
`Commons.kt` (PLATFORM); every destination activity; `NotificationViewActivity` and
`NotificationTabAdapter` (sibling module `NOTIFICATION_CENTER.md`).

## 14. How to make common changes

**Add a notification type:** the switch in `NotificationListAdapter.onBindViewHolder` is only
*one* of three copies — `NotificationHelper.displayNotification` (tray) and
`SplashScreenActivity.getMyDetails` (cold deep link) must change too (`N-1`). PM-coordinated.

**De-duplicate the three fragments (`NL1`):** one fragment taking `type` through a `Bundle`
(SEARCH's `SearchPostFragment` is the in-repo reference for the pattern), then delete the other
two and update `NotificationTabAdapter.getItem`. This also removes `NL5` and `NL21`.

**Fix paging (`NL3`/`NL4`/`NL21`):** move `addOnScrollListener` into `onCreateView`, capture
`oldSize` before `addAll`, and increment `page` exactly once per successful fetch.

**Change the row's appearance:** `child_notification_list.xml` is shared — see §13. Escalate.

**Add user-visible error handling (`NL8`):** this is cluster `CL-7`; PM sets the app-wide policy
before individual screens change.

## 15. Change log

| Change | Detail |
|---|---|
| Created (T-026, 2026-10-08) | Documented all three notification tabs as one module (they are copies) plus `NotificationListAdapter`, from `UserNotificationFragment.kt` (170), `AdminNotificationFragment.kt` (175), `WarriorNotificationFragment.kt` (174), `NotificationListAdapter.kt` (199) and 4 layouts. Captured a per-fragment delta table proving the only differences are a layout id, the `type` string, the page size and a stray `page++`; the `"notification"` (singular) envelope; the 6-type routing switch and its 4 gates; and the read-receipt `PUT`. **21 issues** recorded — headline: paging is wrong three separate ways (`NL3` one scroll listener per page, `NL4` wrong `notifyItemRangeInserted` start index, `NL21` double `page++` skipping every other page), plus `NL5` Warrior's 50-vs-10 page-size mismatch and the `CL-1` guard in all three copies. Also recorded the **shared-file hazard**: `AnnouncementAdapter` reuses this module's row layout *and* `ViewHolder` type, so changes here break another team at runtime. |


