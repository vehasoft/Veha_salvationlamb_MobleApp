# NO_PERMISSION Module Agent

> Team: **APPSHELL** · Reports to: `agents/APPSHELL/APPSHELL_LEAD.md`
> Scope: this agent knows **every detail** of this screen — views, actions, API, storage,
> validation, messages, navigation. It may only edit the files under **Owned files**.

---

> **This is the terminal screen of the entire permission model.** Every one of the app's
> permission gates ends here on denial. It is 56 lines of code across two files, and it is the
> single most-referenced destination added by the v1.2.0 branch — **19 call sites in 13 files**
> for the Activity, plus **7 more** for the Fragment.

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **"You Don't have permission to access this page"** — the denial screen |
| Class | `com.veha.activity.NoPermissionActivity` (27 LOC) **and** `com.veha.fragments.NoPermissionFragment` (29 LOC) — two independent implementations of the same message |
| Layout | `res/layout/activity_no_permission.xml` · `res/layout/fragment_no_permission.xml` (near-duplicates) |
| Manifest entry | `<activity android:name=".NoPermissionActivity" android:exported="false" />` — no intent-filter. The Fragment needs none |
| Entered from | **Activity:** 19 `startActivity` sites in 13 files (§9a). **Fragment:** `TabAdapter.getItem` (5 of 6 tabs) and `SearchAdapter.getItem` (both tabs) |
| Exits to | `MainActivity` (logo tap) or **back** (`finish()`); the Fragment exits nowhere (`NOP1`) |

## 2. UI inventory

### `activity_no_permission.xml`

| View id | Type | Text / hint | Notes |
|---|---|---|---|
| `@id/header_main` | header container | — | standard app header, not bound in code |
| `@id/prod_logo` | `ImageView` | — | → `logo`; tap goes to `MainActivity` |
| (no id) | `TextView`, `textSize="20dp"`, `@color/black` | **`"You Don't have permission to access this page \n Please contact administrator"`** | hard-coded English; `20dp` should be `20sp` (`NOP6`) |
| `@id/go_back` | `Button`, `@color/white` text | **`"Go Back"`** | → `goBack`; calls `finish()` |

### `fragment_no_permission.xml`

| View id | Type | Text / hint | Notes |
|---|---|---|---|
| (no id) | `TextView` | **same string, duplicated verbatim** | `NOP7` |
| `@id/go_back` | `Button` | **`"Go Back"`** | **declared in the fragment as `lateinit var goBack: Button` but never bound and never clicked — the button does nothing** (`NOP2`) |

> No header/logo in the fragment variant — it is rendered inside `MainActivity`'s existing chrome.

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| **Activity** `onCreate` | `setContentView` → `findViewById(prod_logo)` → `findViewById(go_back)` → attach both listeners. That is the whole class |
| **Activity** logo tap | `startActivity(Intent(this, MainActivity::class.java))` — **no `finish()`, no `CLEAR_TOP`**, so a second `MainActivity` is stacked on top of the existing one and this screen stays underneath (`NOP3`) |
| **Activity** "Go Back" | `finish()` — correct |
| **Fragment** `onCreate` | `super.onCreate` only — dead override |
| **Fragment** `onCreateView` | inflates the layout and returns it. **`goBack` is never assigned**, so the fragment's only control is inert (`NOP2`) |
| **Fragment** "Go Back" tap | **nothing happens** |

## 4. Validation rules

| Field | Rule | Failure message |
|---|---|---|
| — | none — no input on this screen | — |

## 5. API contracts

**none.** Neither class performs any network call. This is the only screen added by the v1.2.0
branch with no Retrofit usage at all.

## 6. Storage read / written

| Key | Type | Operation | Value |
|---|---|---|---|
| — | — | — | **none** — no `UserPreferences` instance is created |

## 7. Global state touched

| Field | Operation | Value |
|---|---|---|
| — | — | **none** — notably, this screen does **not** read `Util.permissionMap`, so it cannot tell the user *which* permission was missing (`NOP4`) |

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| Screen shown | `"You Don't have permission to access this page \n Please contact administrator"` | static `TextView`, **hard-coded in both layouts** (`NOP7`) |
| — | `"Go Back"` | static `Button` label, hard-coded |

Both strings are missing from `strings.xml` and are **not translatable** (`NOP6`).

## 9. Navigation map

| From | Trigger | To | Extras | finish()? |
|---|---|---|---|---|
| `NoPermissionActivity` | logo tap | `MainActivity` | none | **no** (`NOP3`) |
| `NoPermissionActivity` | "Go Back" | — (back) | — | **yes** |
| `NoPermissionFragment` | anything | — | — | — (inert, `NOP2`) |

### 9a. Who sends the user here — the permission-gate inventory

**`NoPermissionActivity` — 19 `startActivity` call sites across 13 files:**

| File | Sites | Gate(s) that fail |
|---|---|---|
| `activity/SplashScreenActivity.kt` | 4 | FCM deep links: `POST`/`Read`, `USER`/`Read`, `USER`/`Edit`, `ANNOUNCEMENT`/`Read` |
| `adapter/NotificationListAdapter.kt` | 4 | in-app notification taps, same four gates |
| `activity/MainActivity.kt` | 2 | `ANNOUNCEMENT`/`Read` (announcement button) + one more |
| `fragments/HomeFragment.kt` | 1 | `POST`/`Create` (add-post button) |
| `activity/FavoritesActivity.kt` | 1 | `POST`/`Read` |
| `activity/AboutActivity.kt` | 1 | — |
| `activity/FollowerActivity.kt` | 1 | `PROFILE`/`Read` |
| `activity/ViewLikesActivity.kt` | 1 | `PROFILE`/`Read` |
| `adapter/FollowAdapter.kt` | 1 | `PROFILE`/`Read` |
| `adapter/HomeAdapter.kt` | 1 | `PROFILE`/`Read` |
| `adapter/UsersAdapter.kt` | 1 | `PROFILE`/`Read` |
| `adapter/ViewLikesAdapter.kt` | 1 | `PROFILE`/`Read` |
| `adapter/AnnouncementAdapter.kt` | 1 | `ANNOUNCEMENT`/`Read` |

**`NoPermissionFragment` — 7 sites, all inside pager adapters:**

| File | Tab / position | Gate |
|---|---|---|
| `adapter/TabAdapter.kt` 0 | Home | `POST`/`Read` |
| `adapter/TabAdapter.kt` 1 | Files | `FILE`/`Read` |
| `adapter/TabAdapter.kt` 2 | **Bible** | **no gate — by design** (`NOP5`, customer-confirmed) |
| `adapter/TabAdapter.kt` 3 | Admin video | `POST`/`Read` **and** `VIDEO`/`Read` |
| `adapter/TabAdapter.kt` 4 | Admin audio | `POST`/`Read` **and** `AUDIO`/`Read` |
| `adapter/TabAdapter.kt` 5 | Profile | `PROFILE`/`Read` |
| `adapter/SearchAdapter.kt` 0, 1 | Posts / Profiles | `POST`/`Read`, `PROFILE`/`Read` |

> **The Fragment is therefore not dead code** — it is how a denied *tab* is rendered in place,
> while the Activity is how a denied *navigation* is handled. The two exist for genuinely
> different reasons; the duplication is in the layout and the string, not the concept.

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| **Activity** `onCreate` | §3 |
| **Activity** `onBackPressed` | not overridden — default back |
| **Fragment** `onCreate` / `onCreateView` | §3 |
| **Fragment** `onDestroyView` | not overridden |

Neither class holds state, so process death is harmless here — **the only screen added by
v1.2.0 for which that is true.**

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `MainActivity` | APPSHELL (`MAIN_NAV.md`) | logo destination |
| `R.layout.activity_no_permission`, `fragment_no_permission` | APPSHELL/THEMING | layouts |
| `Util.hasPermission` | PLATFORM/COMMONS | **not called here** — every caller evaluates the gate and routes here on denial |

**No PLATFORM runtime dependency at all:** no Retrofit, no DataStore, no `Util` state.

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| NOP4 | **The user is never told which permission was denied.** The Activity receives **no extras** and reads no state, so all 19 call sites produce one identical message. With `hasPermission` now failing closed (T-025), a failed permission fetch sends the user here from anywhere with no way to tell a real denial from a loading failure | **High (UX)** | pass `type` + `permission` as extras and render them; distinguish "not permitted" from "could not load permissions" |
| NOP2 | `NoPermissionFragment` declares `lateinit var goBack: Button` and **never binds it**, so the fragment's only control is inert. A user on a denied tab has **no action at all** | **High (UX)** | bind it and pop to a permitted tab, or remove the button from the layout |
| NOP5 | ~~`TabAdapter` position 2 (**Bible**) is the only tab with no permission gate~~ — **BY DESIGN, confirmed by the customer 2026-10-08: "Bible is open to everyone."** Also recorded as `BIBLE B-1` | By design | **do not add a gate** |
| NOP3 | The logo tap starts `MainActivity` with **no `finish()` and no `CLEAR_TOP`**, stacking a duplicate `MainActivity` and leaving the denial screen underneath it | Medium | `FLAG_ACTIVITY_CLEAR_TOP` + `finish()` |
| NOP8 | Both the Activity and the Fragment exist to render the **same message**, with duplicated layout and duplicated hard-coded text | Medium | one shared layout `<include>`d by both |
| NOP6 | The two user-visible strings are **hard-coded in the layouts**, absent from `strings.xml`, and therefore untranslatable. `textSize="20dp"` should be `20sp` | Low | move to `strings.xml`; use `sp` |
| NOP7 | `"You Don't have permission to access this page \n Please contact administrator"` is duplicated verbatim in two layouts; it also reads awkwardly (`Don't`, missing full stops) | Cosmetic | single string resource, reworded |
| NOP9 | There is **no way to retry** — if the denial came from a transient permission-fetch failure the user must kill and relaunch the app | Medium | add a "Retry" action that re-runs the permission fetch |

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/NoPermissionActivity.kt` | **exclusive** |
| `app/src/main/java/com/veha/fragments/NoPermissionFragment.kt` | **exclusive** |
| `app/src/main/res/layout/activity_no_permission.xml` | **exclusive** |
| `app/src/main/res/layout/fragment_no_permission.xml` | **exclusive** |
| `<activity android:name=".NoPermissionActivity">` in `AndroidManifest.xml` | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** every one of the 26 call sites listed in §9a — they belong to AUTH,
FEED, PROFILE, SEARCH, BIBLE, NOTIFICATIONS and ANNOUNCEMENTS. `Util.hasPermission` and the
permission model itself are PLATFORM/COMMONS. `MainActivity` is `MAIN_NAV.md`.

## 14. How to make common changes

**Tell the user what was denied (`NOP4` — highest value):** add `type` and `permission` string
extras, render them in the `TextView`, and update all 19 call sites to pass them. The call sites
belong to seven other teams, so this is **PM-coordinated**. A cheaper first step: pass a single
human-readable `reason` string and default it when absent.

**Distinguish "denied" from "permissions unavailable":** since T-025, `Util.isPermissionsLoaded()`
tells the two apart. Reading it here (a PLATFORM call, read-only) would let the screen show
either "You don't have permission" or "We couldn't load your permissions — please sign in again".
Pairs naturally with `NOP9`.

**Make the fragment's button work (`NOP2`):** bind `go_back` in `onCreateView` and call
`requireActivity().onBackPressed()`, or switch the pager to a permitted tab.

**Change the wording:** both strings live in the layouts, not `strings.xml` (`NOP6`). Moving them
is a THEMING-adjacent change but the layouts are owned here, so it can be done locally — add the
entries to `strings.xml` via PM since that file is shared.

**Never add a network call here.** This screen is reached *because* something failed; keeping it
dependency-free is what makes it the one screen that cannot itself fail.

## 15. Change log

| Change | Detail |
|---|---|
| Created (T-026, 2026-10-08) | Documented both halves of the denial screen — `NoPermissionActivity.kt` (27) and `NoPermissionFragment.kt` (29) plus their two layouts — previously unowned since the v1.2.0 branch. Built the **full gate inventory** (§9a): 19 Activity call sites across 13 files and 7 Fragment sites in the two pager adapters, with the specific `PermissionType`/`Permission` pair that fails at each. Established that the Fragment is **not** dead code (it renders denied *tabs* in place) but that its "Go Back" button is **never bound** (`NOP2`). 9 issues recorded; headline is `NOP4` — the screen takes no extras and reads no state, so **the user is never told which permission was denied**, and since T-025 made `hasPermission` fail closed it cannot distinguish a real denial from a permission-fetch failure. Also recorded `NOP5`: the Bible tab is the only ungated tab in `TabAdapter`. |


