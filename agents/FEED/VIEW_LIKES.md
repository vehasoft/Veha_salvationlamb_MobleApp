# VIEW_LIKES Module Agent

> Team: **FEED** · Reports to: `agents/FEED/FEED_LEAD.md`
> This agent knows **every detail** of the "who reacted" screen: the list, its adapter, the
> app-wide overflow menu it hosts, the API call and navigation.
> It may only edit the files listed in §10 **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Reactions / Likes list** |
| Classes | `com.veha.activity.ViewLikesActivity` (199 lines) + `com.veha.adapter.ViewLikesAdapter` (56 lines) |
| Layouts | `activity_view_likes.xml`, `child_likes_list.xml` (4 ids) |
| Manifest | `<activity android:name=".ViewLikesActivity" android:exported="false"/>` |
| View access | `findViewById` **plus** a synthetic import of **`activity_edit_profile.*`** for the `menu` view (issue VL1) |
| Required extra | **`postId`** |
| Entered from | `HomeAdapter` — tapping `react_btn` / `no_of_reacts` on a post card |
| Exits to | `ViewProfileActivity` (row tap), `MainActivity` (logo), plus 5 overflow-menu destinations |
| Progress | `SpotsDialog`, "Please Wait" |

---

## 2. UI inventory

### `activity_view_likes.xml`

| View id | Type | Purpose |
|---|---|---|
| `prod_logo` | `ImageView` | tap -> `MainActivity` |
| `menu` | (from `activity_edit_profile` synthetics) | opens the app-wide overflow menu |
| `likesListRecycler` | `RecyclerView` | the reaction list |
| `no_data` | `LinearLayout` | empty state |

### `child_likes_list.xml` — one reactor row

| View id | Type | Bound to |
|---|---|---|
| `like_list_linear` | `LinearLayout` | row container; tap -> `ViewProfileActivity` (extra `userId`) |
| `profile_pic_fol` | `ImageView` | `post.user.picture` via Picasso (only if non-empty) |
| `name_fol` | `TextView` | `post.user.name` |
| `react_txt` | `TextView` | `post.reaction` — the raw reaction string |

> The `_fol` suffix is borrowed from the **follower** row layout — shared naming with PROFILE's list,
> though this is a separate file (issue VL2).

---

## 3. The overflow menu (app-wide, hosted here)

Tapping `menu` inflates `R.menu.main_menu` inside a `ContextThemeWrapper(R.style.menuStyle)`:

| Item | Action |
|---|---|
| `warrior` | `Commons().makeWarrior(this, this)` — hidden if `Util.isWarrior` **or** `Util.user.isReviewState` |
| `logout` | confirm dialog "Do you want to Logout?" -> `finish()`, delete `token` + `userId`, open `LoginActivity` |
| `edit_profile` | `EditProfileActivity` [PROFILE] |
| `fav` | `FavoritesActivity` [FEED] |
| `settings` | `SettingsActivity` [APPSHELL] |
| *(commented out)* | a night-mode toggle, left in the source |

This **same menu block is duplicated across several screens** (`MainActivity`, `FollowerActivity`,
`EditProfileActivity`, `FavoritesActivity`, `ViewProfileActivity` …). It is the real home of the
**logout contract** that AUTH defines, and it is copy-pasted, not shared (issue VL3).

`Util.user.isReviewState.toBoolean()` is called **without a null check** — if `Util.user` is null
after process death, this throws (issue VL4).

---

## 4. API contract

| Item | Value |
|---|---|
| Retrofit | `getPostLike("Bearer $token", postId)` |
| HTTP | `GET api/v1/like/post/{postId}` |
| Guard | `Commons().isNetworkAvailable(this)` |
| Token guard | **`&&`** — correct, like VIEW_POST and unlike most of the app |
| Envelope | **`results`** (plural), each element parsed as `PostLikes` |

**Success (200)** — rebuilds `likeslist`; if empty shows `no_data`, otherwise sets a
`LinearLayoutManager` and a **new** `ViewLikesAdapter` on every load (issue VL5).

**Error (non-200)** — **nothing at all**: no `else` branch exists, so the dialog is dismissed and the
screen stays blank with neither data nor an empty state (issue VL6).

**`onFailure`** — dismisses the dialog and logs. No user feedback.

**Token missing** — because the guard is `&&`, the `else` branch is **reachable here**: it toasts
`"Somthing Went Wrong \nLogin again to continue"`, clears the session and opens `LoginActivity`.
This is one of the **few places the logout contract can actually execute**.

---

## 5. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read (observed) |
| DataStore `token` + `userId` | deleted on logout **and** in the reachable session-lost branch |
| `Util.isWarrior`, `Util.user.isReviewState` | read -> menu visibility |

---

## 6. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | layout -> SpotsDialog -> bind `likesListRecycler`, `no_data`, `prod_logo` -> read `postId` -> `getALlLikes(this)` -> wire the overflow menu |
| `onDestroy` | `dialog.dismiss()` |

`postId` is read with `intent.getStringExtra("postId").toString()`, so a missing extra becomes the
literal `"null"` and is sent to the server (issue VL7).

No rotation handling: the activity reloads from scratch.

---

## 7. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `RetrofitAPI.getPostLike` | PLATFORM / NETWORK | the fetch |
| `PostLikes` -> `PostUser` | PLATFORM / DATA_MODELS | parsing |
| `UserPreferences` | PLATFORM / STORAGE | token + logout |
| `Commons().makeWarrior`, `Commons().isNetworkAvailable`, `Util.*` | PLATFORM / COMMONS | menu action + guard |
| Picasso | PLATFORM / BUILD_CONFIG | avatars |
| `R.menu.main_menu`, `R.style.menuStyle` | APPSHELL / THEMING | the overflow menu |
| `HomeAdapter` | FEED / HOME_FEED | the only entry point |
| `ViewProfileActivity`, `EditProfileActivity` | PROFILE | row tap + menu |
| `FavoritesActivity` | FEED / FAVORITES | menu |
| `SettingsActivity` | APPSHELL / SETTINGS | menu |
| `LoginActivity` | AUTH | logout target |

---

## 8. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| VL6 | A non-200 has **no `else` branch** — no data, no empty state, no message; the screen just stays blank | **High (UX)** | add an error state |
| VL3 | The ~60-line overflow menu is **copy-pasted across 6+ screens**, each owning its own logout implementation | **High** | extract a shared menu handler (PM-level, cross-team) |
| VL4 | `Util.user.isReviewState.toBoolean()` with no null check — NPE after process death | **High** | null-guard `Util.user` |
| VL1 | Imports **`activity_edit_profile.*`** synthetics to resolve `menu`, coupling this screen to another layout | **High** | bind `menu` with `findViewById` |
| VL7 | `getStringExtra("postId").toString()` yields `"null"` for a missing extra | Medium | validate the extra |
| VL5 | A brand-new adapter and layout manager are created on every load instead of updating the existing one | Medium | reuse + `notifyDataSetChanged` |
| VL8 | `react_txt` shows the **raw** reaction string with no icon or grouping | Medium | map to an icon/label |
| VL9 | The Retrofit call is never cancelled in `onDestroy` | Medium | cancel |
| VL10 | `ViewLikesAdapter` uses a **secondary constructor** with `lateinit` fields instead of primary-constructor params | Low | use a primary constructor |
| VL11 | `Picasso.with(context)` — deprecated API (Picasso 2.5.2) | Low | upgrade |
| VL12 | `logo` navigates to `MainActivity` without `finish()` | Low | `finish()` |
| VL13 | No pagination — all reactions load at once | Low | paginate if lists grow |
| VL14 | Commented-out night-mode code left in the menu handler | Cosmetic | delete |
| VL15 | Method named `getALlLikes` (capital L) | Cosmetic | rename |

---

## 9. Notable: this screen gets two things right

1. **The token guard uses `&&`**, so the session-lost branch actually works — unlike LOGIN,
   CHANGE_PASSWORD, HOME_FEED and `Commons.makeWarrior`.
2. **Empty state is handled**: `likeslist.size <= 0` toggles `no_data` against the list.

When fixing the always-true guard elsewhere, this screen is the **reference implementation**.

---

## 10. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/ViewLikesActivity.kt` | **exclusive** |
| `app/src/main/java/com/veha/adapter/ViewLikesAdapter.kt` | **exclusive** (adapters belong to their screen) |
| `app/src/main/res/layout/activity_view_likes.xml` | **exclusive** |
| `app/src/main/res/layout/child_likes_list.xml` | **exclusive** |
| `<activity android:name=".ViewLikesActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `RetrofitAPI.kt`, `DataModels.kt`, `UserPreferences.kt`, `Util.java`,
`Commons.kt`, `main_menu.xml`, `activity_edit_profile.xml`, `HomeAdapter.kt`, and every menu
destination.

---

## 11. How to make common changes

**Add an error state (VL6):** add an `else` to the `response.code() == 200` check that shows
`no_data` (or a dedicated error view) and toasts. Local to this agent.

**Show reaction icons (VL8):** `post.reaction` holds one of the 14 `react1`–`react14` strings owned
by APPSHELL/THEMING. Mapping string -> icon needs a lookup; if you add one, coordinate with
`HOME_FEED.md`, which owns the reaction list.

**Drop the `activity_edit_profile` synthetic import (VL1):** bind `menu` with `findViewById` from
`activity_view_likes.xml`. Local, and it removes an accidental cross-layout dependency.

**Change the overflow menu:** `main_menu.xml` is APPSHELL/THEMING, and the handler is duplicated
across 6+ screens — **always escalate to PM** so every copy stays consistent.

**Paginate (VL13):** `getPostLike` takes no `page`/`size` today — a **PLATFORM/NETWORK** change via
PM.

---

## 12. Change log

| Change | Detail |
|---|---|
| Created | Initial VIEW_LIKES module agent documented from `ViewLikesActivity.kt` (199 lines), `ViewLikesAdapter.kt` (56 lines) and both layouts: the 4 screen views, the 4-id reactor row, the full app-wide overflow menu it hosts (6 destinations incl. the live logout contract), the `GET like/post/{postId}` call with its `results` envelope, and 15 known issues. Noted this screen as the **reference implementation** for the correct `&&` token guard and for empty-state handling. |


