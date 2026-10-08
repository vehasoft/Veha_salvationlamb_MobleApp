# FAVORITES Module Agent

> Team: **FEED** · Reports to: `agents/FEED/FEED_LEAD.md`
> This agent knows **every detail** of the saved-posts screen: its thin host shell, the
> `HomeFragment` it embeds, the overflow menu it customises, and what it does **not** own.
> It may only edit the files listed in §8 **Owned files**.

---

## ⚠ This screen is a host, not a list

`FavoritesActivity` contains **no list, no adapter and no network call of its own**. It is a
~121-line shell whose entire job is:

```kotlin
val viewProfile = HomeFragment.getInstance("fav")
val ft = supportFragmentManager.beginTransaction()
ft.replace(R.id.view_fav, viewProfile)
ft.commit()
```

**All data loading, rendering and post actions belong to `HOME_FEED.md`** via the `type = "fav"`
branch (`getfavPosts` -> `GET favorites/{userId}`). If favourites render wrongly, the bug is almost
certainly in `HomeFragment`/`HomeAdapter`, not here — route it to `HOME_FEED.md`.

> The local variable is named `viewProfile` — copy-pasted from a profile screen (issue FV1).

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Favorites** (saved posts) |
| Class | `com.veha.activity.FavoritesActivity` (121 lines) |
| Layout | `app/src/main/res/layout/activity_favorites.xml` (4 ids) |
| Manifest | `<activity android:name=".FavoritesActivity" android:exported="false"/>` |
| View access | `findViewById` for `prod_logo`, **plus a synthetic import for `menu`** (same cross-layout trap as VIEW_LIKES — issue FV2) |
| Required extras | **none** |
| Entered from | the app-wide overflow menu (`R.id.fav`) on `MainActivity`, `ViewLikesActivity`, `FollowerActivity`, `EditProfileActivity`, `ViewProfileActivity` |
| Exits to | `MainActivity` (logo), plus overflow-menu destinations |
| Network | **none directly** — delegated to `HomeFragment` |

---

## 2. UI inventory (`activity_favorites.xml`)

| View id | Type | Purpose |
|---|---|---|
| `header_main` | container | the header bar |
| `prod_logo` | `ImageView` | tap -> `MainActivity` (**no `finish()`** — issue FV3) |
| `menu` | overflow trigger | opens the app-wide menu |
| `view_fav` | container | **where `HomeFragment("fav")` is injected** |

No empty state, no toolbar title, no "Favorites" heading anywhere — the user gets no label telling
them where they are (issue FV4). The empty state comes from `HomeFragment`'s own `no_data` view.

---

## 3. The overflow menu — customised here

Same `R.menu.main_menu` + `ContextThemeWrapper(R.style.menuStyle)` block as `ViewLikesActivity`, but
with **two items hidden**:

```kotlin
if (Util.isWarrior) { popup.menu.findItem(R.id.warrior).isVisible = false }
popup.menu.findItem(R.id.logout).isVisible = false   // <-- hidden
popup.menu.findItem(R.id.fav).isVisible = false      // <-- hidden (already here)
if (Util.user.isReviewState.toBoolean()) { popup.menu.findItem(R.id.warrior).isVisible = false }
```

| Item | State here |
|---|---|
| `warrior` | hidden if already a warrior or in review |
| **`logout`** | **always hidden** — yet its ~15-line handler (dialog, session clear, `LoginActivity`) is still present below, as **dead code** (issue FV5) |
| **`fav`** | hidden — correct, the user is already here |
| `edit_profile` | visible -> `EditProfileActivity` [PROFILE] |
| `settings` | visible -> `SettingsActivity` [APPSHELL] |

Hiding `logout` here but showing it on sibling screens is an **inconsistency**, not a documented
rule (issue FV6). `Util.user.isReviewState.toBoolean()` is again unguarded against a null
`Util.user` (issue FV7).

---

## 4. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` + `userId` | deleted only in the **dead** logout handler |
| `Util.isWarrior`, `Util.user.isReviewState` | read -> menu visibility |

`UserPreferences` is instantiated in `onCreate` but, with logout hidden, is **effectively unused**
(issue FV8).

---

## 5. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | SpotsDialog built (**and never shown** — issue FV9) -> `UserPreferences` -> bind `prod_logo` -> inject `HomeFragment("fav")` -> wire the overflow menu |

The fragment transaction uses `replace()` with **no `savedInstanceState == null` guard**, so on every
rotation a **new** `HomeFragment` replaces the old one and the favourites reload from scratch
(issue FV10).

---

## 6. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `HomeFragment` | **FEED / HOME_FEED** | the entire content — all loading and rendering |
| `Commons().makeWarrior`, `Util.*` | PLATFORM / COMMONS | menu action and visibility |
| `UserPreferences` | PLATFORM / STORAGE | dead logout handler |
| `R.menu.main_menu`, `R.style.menuStyle` | APPSHELL / THEMING | the overflow menu |
| `MainActivity`, `SettingsActivity` | APPSHELL | logo + menu |
| `EditProfileActivity` | PROFILE | menu |
| `LoginActivity` | AUTH | dead logout target |

---

## 7. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| FV10 | The fragment is `replace()`d with no `savedInstanceState` guard — every rotation rebuilds it and re-fetches | **High** | guard the transaction |
| FV7 | `Util.user.isReviewState.toBoolean()` unguarded — NPE after process death | **High** | null-guard `Util.user` |
| FV2 | `menu` is resolved via a **synthetic import of another layout** | **High** | use `findViewById` |
| FV11 | The ~60-line menu block is duplicated here too (team issue VL3) | **High** | shared handler (PM-level) |
| FV5 | `logout` is hidden but its full handler remains as dead code | Medium | remove the handler, or show the item |
| FV6 | Hiding `logout` here but not on sibling screens is undocumented and inconsistent | Medium | decide a rule with APPSHELL |
| FV4 | No screen title or heading — the user cannot tell they are in Favorites | Medium | add a title |
| FV9 | A `SpotsDialog` is built and never used | Low | delete |
| FV8 | `UserPreferences` is instantiated but effectively unused | Low | delete with FV5 |
| FV3 | The logo navigates to `MainActivity` without `finish()` | Low | `finish()` |
| FV1 | The fragment variable is named `viewProfile` | Cosmetic | rename |

---

## 8. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/FavoritesActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_favorites.xml` | **exclusive** |
| `<activity android:name=".FavoritesActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `HomeFragment.kt` / `HomeAdapter.kt` / `child_post.xml`
(**HOME_FEED — this is where favourites actually render**), `main_menu.xml` (APPSHELL),
`RetrofitAPI.kt`, `UserPreferences.kt`, `Util.java`, `Commons.kt`.

---

## 9. How to make common changes

**"Favourites show the wrong thing / don't load / can't be unsaved":** **not this agent.** Route to
`HOME_FEED.md`, specifically `getfavPosts` and the `type = "fav"` branch.

**Add a screen title (FV4):** add a `TextView` to `activity_favorites.xml`'s header. Local.

**Fix the rotation reload (FV10):** wrap the transaction in `if (savedInstanceState == null) { … }`.
Local, and the highest-value fix here.

**Show or remove logout (FV5/FV6):** either delete the dead handler or stop hiding the item — but
the menu is shared across 6+ screens, so **ask PM** which behaviour is intended app-wide.

**Add favourites-specific UI** (sort, "clear all"): add the control here, but the data lives in
`HomeFragment("fav")` — a **two-agent change** with HOME_FEED.

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial FAVORITES module agent documented from `FavoritesActivity.kt` (121 lines) and `activity_favorites.xml` (4 ids). Established that this screen is a **host shell with no list, adapter or network call of its own** — all favourites rendering belongs to `HOME_FEED.md` via `HomeFragment.getInstance("fav")`. Recorded the customised overflow menu (logout and fav hidden, logout's handler left as dead code), the unguarded fragment transaction, and 11 known issues. |


