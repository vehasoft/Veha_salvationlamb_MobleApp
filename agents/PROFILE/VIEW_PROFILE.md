# VIEW_PROFILE Module Agent

> Team: **PROFILE** · Reports to: `agents/PROFILE/PROFILE_LEAD.md`
> This agent knows **every detail** of the other-user profile screen: its host shell, the
> `ProfileFragment` it embeds in `"other"` mode, the overflow menu, and what it does **not** own.
> It may only edit the files listed in §8 **Owned files**.

---

## ⚠ This screen is a host, not a profile

`ViewProfileActivity` (112 lines) contains **no profile UI, no list and no network call of its own**.
Its entire job is:

```kotlin
val userId = intent.getStringExtra("userId")
val viewProfile = ProfileFragment.getInstance(userId!!, "other")
supportFragmentManager.beginTransaction().replace(R.id.view_profile, viewProfile).commit()
```

**All rendering and loading belongs to `MY_PROFILE.md`** via the `who = "other"` branch. If an avatar,
name, count or post list is wrong here, route it to `MY_PROFILE.md` (or `HOME_FEED.md` for the post
cards).

This is the **same host-shell pattern** as `FavoritesActivity` in FEED.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **View Profile** (another user) |
| Class | `com.veha.activity.ViewProfileActivity` (112 lines) |
| Layout | `app/src/main/res/layout/activity_view_profile.xml` (4 ids) |
| Manifest | `<activity android:name=".ViewProfileActivity" android:exported="false"/>` |
| View access | `findViewById` for `prod_logo`, **plus a synthetic import for `menu`** (issue VPR1) |
| Required extra | **`userId`** — the user to display |
| Entered from | `HomeAdapter` (author tap) [FEED], `ViewLikesAdapter` (reactor row) [FEED], `FollowAdapter` (follower row) [PROFILE], `UsersAdapter` (search result) [SEARCH] |
| Exits to | `MainActivity` (logo) + overflow-menu destinations |
| Network | **none directly** — delegated to `ProfileFragment` |

> **Four callers across three teams.** Changing the `userId` extra name breaks FEED, PROFILE and
> SEARCH simultaneously — always escalate to PM.

---

## 2. UI inventory (`activity_view_profile.xml`)

| View id | Type | Purpose |
|---|---|---|
| `header_main` | container | header bar |
| `prod_logo` | `ImageView` | tap -> `MainActivity` (**no `finish()`** — issue VPR2) |
| `menu` | overflow trigger | the app-wide menu |
| `view_profile` | container | **where `ProfileFragment(userId, "other")` is injected** |

No title, no back affordance other than the system Back, and **no indication whose profile this is**
until the fragment loads (issue VPR3).

---

## 3. The overflow menu

The same duplicated `R.menu.main_menu` + `ContextThemeWrapper(R.style.menuStyle)` block found on 6+
screens: `warrior` (hidden when already a warrior or in review), `logout` (confirm dialog -> clear
session -> `LoginActivity`), `edit_profile`, `fav`, `settings`.

Notably this menu is **not filtered for context** — while viewing *someone else's* profile the user
still sees "Edit Profile", which edits **their own** (issue VPR4).

`Util.user.isReviewState.toBoolean()` is unguarded against a null `Util.user` (issue VPR5).

---

## 4. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` + `userId` | deleted by the menu's logout action |
| `Util.isWarrior`, `Util.user.isReviewState` | read -> menu visibility |

`Util.userId` (the **viewer**) and the `userId` extra (the **subject**) are different values — this
screen passes the extra to the fragment and never conflates them, which is correct.

---

## 5. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | layout -> SpotsDialog (**built, never shown** — issue VPR6) -> `UserPreferences` -> read `userId` -> inject the fragment -> bind `prod_logo` -> wire the overflow menu |
| `onPause` / `onDestroy` | `dialog.dismiss()` |

`intent.getStringExtra("userId")` is force-unwrapped with `userId!!` when passed to
`getInstance(...)`, so launching this activity **without** the extra throws an NPE immediately
(issue VPR7).

The fragment transaction has **no `savedInstanceState == null` guard**, so each rotation replaces the
fragment and re-runs all 5 of its network calls (issue VPR8).

---

## 6. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `ProfileFragment` | **PROFILE / MY_PROFILE** | the entire content |
| `HomeAdapter` (transitively) | FEED / HOME_FEED | the post list inside the fragment |
| `Commons().makeWarrior`, `Util.*` | PLATFORM / COMMONS | menu |
| `UserPreferences` | PLATFORM / STORAGE | logout |
| `R.menu.main_menu`, `R.style.menuStyle` | APPSHELL / THEMING | menu |
| `MainActivity`, `SettingsActivity` | APPSHELL | logo + menu |
| `EditProfileActivity` | PROFILE / EDIT_PROFILE | menu |
| `FavoritesActivity` | FEED / FAVORITES | menu |
| `LoginActivity` | AUTH | logout target |

---

## 7. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| VPR7 | `userId!!` force-unwrap — launching without the extra **crashes immediately** | **High** | validate and finish gracefully |
| VPR8 | The fragment is `replace()`d with no `savedInstanceState` guard — every rotation re-runs 5 network calls | **High** | guard the transaction |
| VPR5 | `Util.user.isReviewState.toBoolean()` unguarded — NPE after process death | **High** | null-guard |
| VPR1 | `menu` resolved via a **synthetic import of another layout** | **High** | use `findViewById` |
| VPR9 | The ~60-line overflow menu is duplicated here too | **High** | shared handler (PM-level) |
| VPR4 | "Edit Profile" is offered while viewing someone else's profile, and edits your own | Medium | hide it here, or relabel |
| VPR3 | No title or subject indication before the fragment loads | Medium | show the name in the header |
| VPR10 | No "follow" action at the screen level — following is only possible from a post card | Medium (product) | ask the customer |
| VPR6 | A `SpotsDialog` is built and never shown | Low | delete |
| VPR2 | The logo navigates to `MainActivity` without `finish()` | Low | `finish()` |

---

## 8. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/ViewProfileActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_view_profile.xml` | **exclusive** |
| `<activity android:name=".ViewProfileActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `ProfileFragment.kt` / `fragment_profile.xml`
(**MY_PROFILE — where everything actually renders**), `HomeAdapter.kt` (FEED), `main_menu.xml`
(APPSHELL), and all PLATFORM files.

---

## 9. How to make common changes

**"The other user's profile shows wrong data":** **not this agent.** Route to `MY_PROFILE.md`
(`who = "other"` branch), or `HOME_FEED.md` for the post cards.

**Show whose profile it is (VPR3):** add a `TextView` to the header here and have the fragment
report the name back — a **two-agent change** with MY_PROFILE.

**Hide "Edit Profile" in this context (VPR4):** add
`popup.menu.findItem(R.id.edit_profile).isVisible = false`, mirroring how `FavoritesActivity` hides
its own items. Confirm with PM, since the menu is shared.

**Harden the extra (VPR7):** replace `userId!!` with a null check that toasts and `finish()`es.
Local to this agent and low-risk.

**Add a follow button (VPR10):** needs `POST follows` (already in `RetrofitAPI`) plus follow-state
awareness, which `MY_PROFILE` currently cannot provide because it passes empty maps (MP4). Two-agent
change, PM first.

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial VIEW_PROFILE module agent documented from `ViewProfileActivity.kt` (112 lines) and `activity_view_profile.xml` (4 ids). Established that this screen is a **host shell** whose content is entirely `MY_PROFILE`'s `who = "other"` mode. Recorded its **four callers across three teams** (FEED x2, PROFILE, SEARCH), the `userId!!` crash risk, the unguarded fragment transaction, the context-blind overflow menu, and 10 known issues. |

