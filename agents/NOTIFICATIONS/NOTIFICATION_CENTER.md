# NOTIFICATION_CENTER Module Agent

> Team: **NOTIFICATIONS** · Reports to: `agents/NOTIFICATIONS/NOTIFICATIONS_LEAD.md`
> Scope: this agent knows **every detail** of this screen — views, actions, API, storage,
> validation, messages, navigation. It may only edit the files under **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **Notification centre** — a 2- or 3-tab shell (User / Admin / **Warrior**) over a `ViewPager` |
| Class | `com.veha.activity.NotificationViewActivity` (79 LOC, Kotlin, `AppCompatActivity`) + `com.veha.adapter.NotificationTabAdapter` (43 LOC, `FragmentPagerAdapter`) |
| Layout | `res/layout/activity_notification_view.xml` (57 lines) |
| Manifest entry | `<activity android:name=".NotificationViewActivity" android:exported="false" />` — **no theme, no `configChanges`, no `screenOrientation`** (unlike most activities in this app) |
| Entered from | **exactly one call site**: `MainActivity.kt:216` — tapping the bell icon (`notification.setOnClickListener`). **No permission gate on that tap**, unlike the adjacent announcement button (`NC11`) |
| Exits to | `MainActivity` (logo tap, and again on Back) · the 3 tab fragments host the actual rows, which exit to FEED / PROFILE / MEDIA / APPSHELL (see `NOTIFICATION_LISTS.md` §9) |

**This activity makes no network call of its own.** It is pure chrome: header, tabs, pager. All
data loading lives in the three fragments (`NOTIFICATION_LISTS.md`). Its one piece of logic is the
**role check that decides whether the Warrior tab exists** — and that check is where its two
crash bugs live.

## 2. UI inventory

| View id | Type | Text / hint | Notes |
|---|---|---|---|
| (root) | `LinearLayout` vertical, `@color/white`, `fitsSystemWindows="true"` | — | not bound; `tools:context=".NotificationViewActivity"` is **wrong** (missing the `com.veha.activity` prefix — tooling-only) |
| `@id/header_main` | `ConstraintLayout`, `@color/primary_blue`, `padding=5dp`, `layout_weight="1"` | — | header bar; not bound in code. **`layout_weight` on a `wrap_content` child of a vertical `LinearLayout`** — see `NC9` |
| `@id/prod_logo` | `ImageView` 200×50dp, `@drawable/ic_sl_logo_01_svg` | — | → `logo` (`lateinit var`); tap → `MainActivity` **without `finish()`** (`NC4`). No `contentDescription` |
| `@id/close` | `ImageButton` 55×55dp, `@drawable/ic_baseline_close_24`, `background="@null"`, `tint="@color/always_white"` | — | → `close`; calls `finish()`. `contentDescription="@null"` — explicitly **un**described (`NC12`) |
| `@id/notification_tab_layout` | `TabLayout` (Material), `@color/primary_blue`, `tabTextColor=@color/always_white`, `tabSelectedTextColor=@color/home_bg`, `tabIndicatorHeight=5dp`, `layout_weight="1"` | tabs built **in code** | bound as a **local** `val tabLayout` (not a field); tabs added imperatively, `tabGravity = GRAVITY_FILL` |
| `@id/notification_viewpager` | `androidx.viewpager.widget.ViewPager`, `layout_weight="10"` | — | → `viewPager` (`lateinit var`); **ViewPager1**, not ViewPager2 (`NC10`) |

### Tabs (constructed in `onCreate`, not in XML)

| Position | `tag` | `text` | Fragment | Added when |
|---|---|---|---|---|
| 0 | `"User"` | `"User"` | `UserNotificationFragment` | always |
| 1 | `"Admin"` | `"Admin"` | `AdminNotificationFragment` | always |
| 2 | `"Warrior"` | `"Warrior"` | `WarriorNotificationFragment` | **only if `Util.user.role == "admin"`** |

`tabLayout.tabCount` is then passed to `NotificationTabAdapter(context, fm, totalTabs)` as
`getCount()`, so a non-admin pager has exactly 2 pages and position 2 is never requested.

### Static text (no id)

| Text | Notes |
|---|---|
| `"User"` | hard-coded tab label **and** `tag`, set in Kotlin — not a string resource (`NC8`) |
| `"Admin"` | ditto |
| `"Warrior"` | ditto |

The three `tag` values are set but **never read anywhere in the codebase** — dead state (`NC13`).

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| `onCreate` | `setContentView(activity_notification_view)` → binds `prod_logo`, `close` → finds `notification_tab_layout` → builds the 3 `TabLayout.Tab` objects → `addTab(user, 0)`, `addTab(admin, 1)` → **`if (Util.user == null) MainActivity().getMyDetails()`** (`NC1` / `N-5`) → **`if (Util.user.role == "admin") addTab(warrior, 2)`** (`NC2` / `N-6`) → `tabGravity = GRAVITY_FILL` → builds `NotificationTabAdapter` with `tabLayout.tabCount` → binds + sets `viewPager.adapter` → `addOnPageChangeListener(TabLayoutOnPageChangeListener(tabLayout))` → `addOnTabSelectedListener` |
| Tap `@id/prod_logo` | `startActivity(Intent(this, MainActivity::class.java))` — **no `finish()`, no `CLEAR_TOP`**; the centre stays on the stack behind a new `MainActivity` (`NC4`) |
| Tap `@id/close` | `finish()` — the only clean exit |
| Tab selected | `onTabSelected { if (tab != null) viewPager.currentItem = tab.position }`; `onTabUnselected` / `onTabReselected` are **empty overrides** |
| Page swiped | `TabLayout.TabLayoutOnPageChangeListener` keeps the tab strip in sync (deprecated API, `NC10`) |
| System Back | `super.onBackPressed()` **and then** `startActivity(Intent(this, MainActivity))` — see `NC3`: the activity finishes *and* pushes a fresh `MainActivity` on top of the existing one |

### `NotificationTabAdapter.getItem(position)`

```kotlin
var b: Any? = null
return when (position) {
    0 -> UserNotificationFragment()
    1 -> AdminNotificationFragment()
    2 -> WarriorNotificationFragment()
    else -> b as Fragment      //  b is always null  ->  NullPointerException
}
```

`b` is declared as `Any? = null` and never assigned — the `else` branch is a guaranteed
`NullPointerException` (`NC5` / `N-7`). It is unreachable *today* because `getCount()` returns
`tabCount` (2 or 3), but it is the **same P0 pattern** as `TabAdapter.kt:78` (APPSHELL) and
`SearchAdapter.kt:43` (SEARCH) — cluster-wide, see `AGENTS.md` §7.

The adapter also **imports `com.veha.fragments.AdminAudioFragment` and never uses it** (`NC6`) —
a copy-paste leftover from `TabAdapter`, and the only reason this file references MEDIA at all.
It further carries an unused `context` field and an `init {}` block that merely re-assigns the
constructor parameters.

## 4. Validation rules

| Field | Rule | Failure message |
|---|---|---|
| `Util.user` | `if (Util.user == null) MainActivity().getMyDetails()` — intended as a repair step; in practice **always throws** (`NC1`) | none (crash) |
| `Util.user.role` | read **unconditionally on the very next line**, outside the null check above | none (`NullPointerException`, `NC2`) |
| `savedInstanceState` | **never inspected** — no state restore | — |
| Tab count | implicit: `getCount()` = `tabLayout.tabCount`, the only thing keeping `getItem`'s `else` branch unreachable | none (`NC5`) |

> **There is no user input on this screen**, so §4 is really a crash-guard audit — and both
> guards are broken.

## 5. API contracts

**none.** `NotificationViewActivity` performs **zero** Retrofit calls, has no
`Commons().isNetworkAvailable` check and never constructs `UserPreferences`. It *attempts* to
trigger one indirectly via `MainActivity().getMyDetails()` (`GET api/v1/users/{userId}`), but that
call never reaches the network — see `NC1`.

For the endpoints used by the tab bodies see `NOTIFICATION_LISTS.md` §5 and
`NOTIFICATIONS_LEAD.md` §5.

## 6. Storage read / written

| Key | Type | Operation | Value |
|---|---|---|---|
| — | — | **none** | this activity never constructs `UserPreferences`; DataStore is only touched by the child fragments |

## 7. Global state touched

| Field | Operation | Value |
|---|---|---|
| `Util.user` (`UserRslt`, static) | **read** — null check in `onCreate` | populated by `MainActivity.getMyDetails()` / `SplashScreenActivity`; `null` after process death (`NC2`) |
| `Util.user.role` | **read, unguarded** | `"admin"` gates the Warrior tab; any other value → 2 tabs; `null` user → NPE |
| `Util.permissionMap` / `Util.hasPermission` | **never called** — the notification centre itself is ungated (`NC11`) | — |
| `Util.userId`, `Util.isNight`, `Util.fontSize` | **not touched** | the screen ignores the app-wide night-mode and font-size settings (`NC7`) |

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| any | **none** | this activity shows **no toast, no dialog, no empty state and no error text whatsoever** |
| `Util.user == null` | **none** — the app crashes instead (`NC1`, `NC2`) | — |

The only user-visible strings are the three hard-coded tab labels (§2).

## 9. Navigation map

| From | Trigger | To | Extras | finish()? |
|---|---|---|---|---|
| `MainActivity` | tap the bell `@id/notification` | **`NotificationViewActivity`** | none | no |
| `NotificationViewActivity` | tap `@id/prod_logo` | `MainActivity` | none | **no** (`NC4`) |
| `NotificationViewActivity` | tap `@id/close` | — (returns to `MainActivity` via the stack) | — | **yes** |
| `NotificationViewActivity` | system Back | `MainActivity` | none | **yes, and *also* starts a second `MainActivity`** (`NC3`) |
| child fragment row tap | see `NOTIFICATION_LISTS.md` §9 | `ViewPostActivity` / `ViewProfileActivity` / `ApproveRequestActivity` / `PdfActivity2` / `WebViewActivity` / `NoPermissionActivity` | per type | no |

> **No FCM deep link lands here.** `NotificationHelper` (`PUSH_SERVICE.md`) and
> `SplashScreenActivity` (AUTH) route straight to the destination screen; the centre is only ever
> reached by the bell icon.

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | everything (§3) — 55 lines, no extraction |
| `onBackPressed` | overridden: `super.onBackPressed()` **then** `startActivity(MainActivity)` (`NC3`) |
| `onStart` / `onResume` / `onPause` / `onStop` / `onDestroy` | **not overridden** — the lists are therefore **never refreshed**; a row marked read in another tab, or a push arriving while the screen is open, is invisible until recreation (`NC15`) |
| `onSaveInstanceState` / restore | **not implemented**; `savedInstanceState` is passed to `super` and ignored — the selected tab is lost on recreation |
| Rotation | no `configChanges` in the manifest → the activity **is** recreated; `onCreate` re-runs the `Util.user.role` read (→ `NC2` fires again) and every fragment reloads page 1 |
| Process death | `Util.user` is `null` → `NC1` + `NC2` crash the screen on the first frame |
| `FragmentPagerAdapter` | built with the **deprecated no-`behavior` constructor** → off-screen pages stay `RESUMED` (`BEHAVIOR_SET_USER_VISIBLE_HINT`), so all 2–3 tabs fire their `GET api/v1/notifications` simultaneously when the centre opens (`NC10`) |

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `Util.user` / `UserRslt.role` | **PLATFORM / COMMONS** + `DATA_MODELS` | the Warrior-tab role check |
| `activity/MainActivity` (`getMyDetails()`, the bell click) | **APPSHELL / MAIN_NAV** | the (broken) user-repair call `NC1`; also the launch site and every exit |
| `fragments/UserNotificationFragment`, `AdminNotificationFragment`, `WarriorNotificationFragment` | **NOTIFICATIONS / `NOTIFICATION_LISTS.md`** | the three pages |
| `fragments/AdminAudioFragment` | MEDIA / `ADMIN_AUDIO.md` | **imported but unused** (`NC6`) |
| `com.google.android.material.tabs.TabLayout`, `androidx.viewpager.widget.ViewPager` | PLATFORM / BUILD_CONFIG | tabs + pager |
| `@color/primary_blue` / `white` / `always_white` / `home_bg`, `@drawable/ic_sl_logo_01_svg`, `@drawable/ic_baseline_close_24` | APPSHELL / THEMING | chrome |

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| NC1 | **`MainActivity().getMyDetails()`** (line 45) — team id `N-5`. A manually constructed `Activity` has no `Context`, no `userPreferences`, no window. `getMyDetails()` immediately touches `userPreferences.userId.asLiveData().observe(this)` on an un-attached instance → `NullPointerException` **every time**. So the `Util.user == null` repair path does **nothing except crash** — and it crashes inside the very branch that was supposed to prevent `NC2` | Critical | Delete the call. Either bounce to `SplashScreenActivity`/`LoginActivity` when `Util.user == null`, or fetch the user from *this* activity with its own `UserPreferences(this)` + Retrofit call. Same bug class as the manually-constructed-Activity cluster in `AGENTS.md` |
| NC2 | **`Util.user.role` is read one line after the null check meant to protect it** (lines 44–47) — team id `N-6`. The `if (Util.user == null) { … }` block does **not** `return`, so execution falls straight through into `Util.user.role`. After process death (the OS restores the task into `MainActivity`, the user taps the bell) `Util.user` is `null` and the screen **NPEs before the pager is built** | Critical | `val role = Util.user?.role ?: run { startActivity(Intent(this, SplashScreenActivity::class.java)); finish(); return }`, then gate the Warrior tab on `role`. Same change set as `NC1` |
| NC5 | **`else -> b as Fragment` with `b: Any? = null`** in `NotificationTabAdapter.getItem` — team id `N-7`. A guaranteed NPE for any position ≥ 3 or < 0; unreachable only by accident, because `getCount()` happens to equal `tabCount`. Third copy of the same P0 (`TabAdapter.kt:78`, `SearchAdapter.kt:43`) | High | `else -> throw IllegalArgumentException("unknown tab $position")`, or return `UserNotificationFragment()` as a safe default. Fix all three copies together (PM-coordinated, cross-team) |
| NC3 | **`onBackPressed` calls `super` *and* `startActivity(MainActivity)`** — the activity finishes **and** a second `MainActivity` is pushed on top of the one already below it. The user must press Back twice to leave, and `MainActivity.onCreate` re-runs its whole bootstrap (badge count, user fetch, nag dialog) | High | Keep only `super.onBackPressed()`; `MainActivity` is already on the stack. If explicit up-navigation is wanted, drop the `super` call and use `FLAG_ACTIVITY_CLEAR_TOP \| FLAG_ACTIVITY_SINGLE_TOP` |
| NC4 | Logo tap does `startActivity(MainActivity)` **without `finish()` or `CLEAR_TOP`**, stacking duplicate `MainActivity` instances (same pattern as `BR7` in BIBLE and several other screens) | Medium | `finish()` after `startActivity`, or `FLAG_ACTIVITY_CLEAR_TOP \| FLAG_ACTIVITY_SINGLE_TOP` |
| NC10 | **Fully deprecated pager stack**: `androidx.viewpager.widget.ViewPager` + `FragmentPagerAdapter(fm)` (no-`behavior` constructor) + `TabLayout.TabLayoutOnPageChangeListener`. Consequence: every off-screen tab is `RESUMED`, so opening the centre fires **2 or 3 simultaneous** `GET api/v1/notifications` calls, each with its own always-true token observer (`N-10`) | Medium | Migrate to `ViewPager2` + `FragmentStateAdapter` + `TabLayoutMediator`; in RN use a lazy top-tab navigator |
| NC14 | The adapter captures `tabLayout.tabCount` **once**. Nothing re-creates it if `Util.user.role` changes while the app is alive (role promotion, admin demotion) — the Warrior tab only appears/disappears after a full restart | Medium | Re-evaluate the role and re-create the adapter in `onResume` when `tabCount` changed |
| NC15 | The activity **never refreshes**: `onResume` is not overridden and the fragments only load in `onCreateView`. Rows marked read in a sibling tab, and notifications arriving while the screen is open, stay invisible; the `MainActivity` badge also goes stale | Medium | Add a `reload()` to the fragments and call it from `onResume` (joint change with `NOTIFICATION_LISTS.md`) |
| NC6 | `NotificationTabAdapter` **imports `AdminAudioFragment` and never uses it** (copy-paste residue from `TabAdapter`), and carries an unused `context` field plus an `init {}` block that only re-assigns its constructor parameters | Low | Delete the import, the field and the `init` block; use `class NotificationTabAdapter(fm: FragmentManager, private val totalTabs: Int)` |
| NC7 | The screen ignores `Util.isNight` and `Util.fontSize`: the root is hard-coded `@color/white` and the tab labels use the default size. In night mode the header is right but the row text (`@color/black` on `@color/white` in `child_notification_list.xml`) does not follow the theme | Low | Use a night-aware colour / `?attr/colorSurface`; apply `Util.fontSize` to the tab labels |
| NC8 | Tab labels `"User"` / `"Admin"` / `"Warrior"` are **hard-coded English literals in Kotlin**, not `strings.xml` — untranslatable, while the rest of the app does localise the "Warrior" concept | Low | Move to `strings.xml` (`R.string.tab_user`, …) |
| NC9 | `@id/header_main` and `@id/notification_tab_layout` both carry `layout_weight="1"` with `layout_height="wrap_content"` in a vertical `LinearLayout` (the pager has `weight="10"`). Weighting `wrap_content` heights makes the header stretch on short content and compete with the pager — fragile layout maths | Low | Header + tabs: `wrap_content` with **no** weight; pager: `0dp` + `weight="1"` |
| NC13 | `user.tag = "User"` / `admin.tag` / `warrior.tag` are assigned and **never read** anywhere in the app — dead state | Cosmetic | Delete the three `tag` assignments |
| NC12 | Accessibility: `@id/prod_logo` has **no** `contentDescription` and `@id/close` has `contentDescription="@null"` — both header controls are unlabelled for TalkBack | Cosmetic | Add real content descriptions (`@string/app_name`, `@string/close`) |

> Team issues confirmed in this file: **`N-5`** (= `NC1`), **`N-6`** (= `NC2`), **`N-7`** (= `NC5`).
> Newly found here: `NC3` (Back both finishes *and* launches a second `MainActivity`), `NC11`
> (ungated centre + ungated Admin/Warrior tabs), `NC10`/`NC14`/`NC15` (deprecated pager → all tabs
> fetch at once, tab set never re-evaluated, no refresh) and `NC6` (unused `AdminAudioFragment`
> import).
> **Not present here:** `CL-1` (no network code in this file), `CL-8` (no LiveData observers),
> `N-8`, `N-10`, `N-11` — all of those live in `NOTIFICATION_LISTS.md`.

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/NotificationViewActivity.kt` (79 LOC) | **exclusive** |
| `app/src/main/java/com/veha/adapter/NotificationTabAdapter.kt` (43 LOC) | **exclusive** — instantiated by this activity only |
| `app/src/main/res/layout/activity_notification_view.xml` (57 lines) | **exclusive** |
| `fragments/UserNotificationFragment.kt`, `AdminNotificationFragment.kt`, `WarriorNotificationFragment.kt` | **not owned** — `NOTIFICATION_LISTS.md` (same team) |
| `activity/MainActivity.kt` (`getMyDetails()`, the bell click at line 216) | **not owned** — APPSHELL / `MAIN_NAV.md` |
| `util/Util.java` (`user`) · `util/DataModels.kt` (`UserRslt.role`) | **not owned** — PLATFORM / `COMMONS.md`, `DATA_MODELS.md` |
| `fragments/AdminAudioFragment.kt` | **not owned** — MEDIA (only an unused import here, `NC6`) |

## 14. How to make common changes

Recipes for the most likely change requests on this screen.

- **Fix the two cold-start crashes (`NC1` + `NC2` — this module's #1 job).** Replace lines 44–49
  with:

  ```kotlin
  val role = Util.user?.role ?: run {
      startActivity(Intent(this, SplashScreenActivity::class.java)); finish(); return
  }
  if (role == "admin") tabLayout.addTab(warrior, 2)
  ```

  This deletes `MainActivity().getMyDetails()` entirely. Escalate to PM only if you prefer to
  re-fetch the user in place — that needs a `UserPreferences(this)` + Retrofit call added here,
  which is module-local and acceptable.
- **Fix the tab-adapter NPE (`NC5`):** a one-line change in this module, but the identical
  `else -> … as Fragment` exists in `TabAdapter.kt` (APPSHELL) and `SearchAdapter.kt` (SEARCH).
  Raise it with PM so all three land in one change set.
- **Add a 4th tab:** add a `TabLayout.Tab`, a `getItem` branch and a new fragment. The tab **must**
  be added *before* line 51, because `getCount()` is `tabLayout.tabCount` captured at construction.
  A new tab also means a new `type` query value, which is a free-form string on
  `RetrofitAPI.getNotifications` → coordinate with PLATFORM / NETWORK.
- **Hide a tab by permission (`NC11`):** wrap each `addTab` in
  `if (Util.hasPermission(PermissionType.USER.value, Permission.Edit.value)) { … }`. Because the
  pager count derives from `tabCount`, nothing else needs changing — this is the cheapest correct
  fix and it aligns the shell with the row-level gates.
- **Make Back behave (`NC3`):** delete the `startActivity` inside `onBackPressed`, leaving only
  `super.onBackPressed()`. Regression check: bell → Back → bell must not stack `MainActivity`.
- **Refresh when the user returns (`NC15`):** override `onResume` and call a `reload()` on the
  current fragment. That method does not exist yet → joint change with `NOTIFICATION_LISTS.md`.
- **Migrate to ViewPager2 (`NC10`):** swap the layout widget, change `NotificationTabAdapter` to
  `FragmentStateAdapter`, and replace both listeners with a single `TabLayoutMediator`. The three
  fragments need no edits. This also removes the "all tabs fetch simultaneously" behaviour — worth
  measuring the request count before and after.
- **React Native note:** this whole screen is a `createMaterialTopTabNavigator` with one
  conditional screen. The only logic worth porting is the role check — and it must be written as
  `user?.role === 'admin'`, i.e. the bug must **not** be ported.

## 15. Change log

| Change | Detail |
|---|---|
| Created (T-026, 2026-10-08) | Documented the notification-centre shell (79 + 43 LOC + a 57-line layout) against the v1.2.0 baseline: the 6 views, the three imperatively-built tabs (User 0 / Admin 1 / Warrior 2, the last added only when `Util.user.role == "admin"`), the `TabLayout` ↔ `ViewPager` double wiring, the `tabCount`-driven `getCount()`, the logo/close actions and the `onBackPressed` behaviour. 15 issues recorded. Top defects: **`NC1`** (= `N-5`) — `MainActivity().getMyDetails()` constructs an `Activity` by hand, so the `Util.user == null` repair path always throws; **`NC2`** (= `N-6`) — `Util.user.role` is dereferenced on the line *after* that null check, which never returns, so the screen NPEs after process death; **`NC5`** (= `N-7`) — `else -> b as Fragment` with `b = null` in `NotificationTabAdapter.getItem`, the third copy of that P0. Newly found: `NC3` (Back both finishes *and* launches a second `MainActivity`), `NC11` (the centre and the Admin/Warrior tabs carry **no** permission gate although their rows do), `NC10`/`NC14`/`NC15` (deprecated ViewPager1 stack → all tabs fetch at once, tab set never re-evaluated, no refresh on resume) and `NC6` (unused `AdminAudioFragment` import). |
