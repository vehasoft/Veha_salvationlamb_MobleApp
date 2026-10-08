# APPSHELL TEAM LEAD Agent

> Reports to: **PROJECT MANAGER** (`AGENTS.md`)
> Manages the 4 module agents below. **Status: all READY.**

---

## 1. Charter

APPSHELL owns the **frame the app lives in**: the bottom-tab host, the app-wide overflow menu, the
settings screen, the About screen, and every shared resource — colours, styles, themes, strings,
menus and dimensions.

It is the only team whose work is **visible on every screen**. When another team says "that's
THEMING" or "that's the shared menu", it lands here.

**Boundary:** APPSHELL owns the container and the look. It owns **no feature content** — the five
tabs are rendered by FEED, MEDIA and PROFILE; APPSHELL only hosts them.

---

## 2. Modules owned

| Module agent | Screen / unit | Source files | Status |
|---|---|---|---|
| `MAIN_NAV.md` | Tab host + overflow menu | `activity/MainActivity.kt` (427), `adapter/TabAdapter.kt` (44), `res/layout/activity_main.xml` (10 ids), `preview_image.xml` | READY |
| `SETTINGS.md` | Settings | `activity/SettingsActivity.kt` (197), `res/layout/activity_settings.xml` (7 ids) | READY |
| `ABOUT.md` | About / my details | `activity/AboutActivity.kt` (152), `activity/ExpandableView.java`, `res/layout/activity_about.xml` (11 ids) | READY |
| `THEMING.md` | All shared resources | `res/values/`, `res/values-night/`, `res/menu/`, `res/drawable*`, `values-land`, `values-w600dp`, `values-w1240dp` | READY |
| `NO_PERMISSION.md` | Permission-denied screen — **terminal destination of all 34 gates** | `activity/NoPermissionActivity.kt` (27), `fragments/NoPermissionFragment.kt` (29), `res/layout/activity_no_permission.xml`, `fragment_no_permission.xml` | READY (T-026) |

> **`preview_image.xml` belongs to `MAIN_NAV.md`**, not to FEED's `IMAGE_DETAIL`. It is inflated
> into a `Dialog` by `MainActivity` for the first-run cover image. Verified and already corrected in
> `IMAGE_DETAIL.md`.

---

## 3. The shell

```
SplashhScreenActivity [AUTH] -> MainActivity
   |
   |-- header: prod_logo, search icon -> SearchActivity [SEARCH]
   |           menu icon -> the app-wide overflow menu (origin of 6+ copies)
   |-- banner: "Make Me Warrior" strip (hidden for warriors / pending review)
   |-- ViewPager + TabLayout -> TabAdapter
   |      tab 0  HomeFragment("user")      [FEED]
   |      tab 1  FilesFragment()           [MEDIA]
   |      tab 2  AdminVideoFragment()      [MEDIA]
   |      tab 3  AdminAudioFragment()      [MEDIA]
   |      tab 4  ProfileFragment(me,"me")  [PROFILE]
   `-- first-run dialog (warriors only): preview_image.xml -> PUT users/freshUser/{id}

overflow menu (every copy): warrior · logout · edit_profile · fav · settings
```

### The overflow menu — APPSHELL's biggest liability

`R.menu.main_menu` + its ~60-line handler is **copy-pasted into at least 6 activities**:
`MainActivity`, `ViewLikesActivity`, `FavoritesActivity`, `FollowerActivity`, `EditProfileActivity`,
`ViewProfileActivity`. Every copy re-implements **logout**, so the AUTH logout contract has six
independent implementations (team issue AS-1). Each copy also calls
`Util.user.isReviewState.toBoolean()` without a null guard.

---

## 4. Theming model

| Mechanism | Where |
|---|---|
| Night mode | `AppCompatDelegate.setDefaultNightMode(...)` chosen in `MainActivity.onCreate` from **`Util.isNight`** (`"Day"` / `"Night"` / `"Default"`) |
| Persistence | DataStore key `isNight` [PLATFORM/STORAGE], written by SETTINGS |
| Font size | **`Util.fontSize`** (10/15/20 F) persisted as `textSize`; applied only to post content and tags by FEED's `HomeAdapter` |
| Resources | `values/` + `values-night/` each with `colors.xml`, `dimens.xml`, `styles.xml`, `themes.xml`; plus `values-land`, `values-w600dp`, `values-w1240dp` |
| App theme | `@style/AppTheme` = `Theme.AppCompat.DayNight` |

**Both theme and font size apply by restarting `MainActivity`** rather than recreating in place
(team issue AS-2).

---

## 5. API surface

| Endpoint | Retrofit method | Used by |
|---|---|---|
| `GET api/v1/users/{userId}` | `getUser` | MAIN_NAV (**copy #3** of `getMyDetails`), ABOUT (**copy #9**) |
| `PUT api/v1/users/freshUser/{userId}` | `putFreshUser` | MAIN_NAV — clears the first-run flag |
| `DELETE api/v1/users/{userId}` | `deleteUser` | SETTINGS — **account deletion** |
| `POST api/v1/users/warrior` | `postWarrior` | via `Commons().makeWarrior` in every menu copy |

`THEMING.md` owns **no** endpoints.

---

## 6. Shared state

| State | Who | Note |
|---|---|---|
| `Util.isNight` | read by MAIN_NAV, written by SETTINGS | drives `setDefaultNightMode` |
| `Util.fontSize` | written by SETTINGS | read by FEED when rendering posts |
| `Util.isFirst` | read by MAIN_NAV | gates the first-run dialog |
| `Util.isWarrior`, `Util.user.isReviewState` | read everywhere | banner + menu visibility |
| `Util.player` | **stopped** on every tab change in `MainActivity` | shared with FEED and MEDIA |
| DataStore `isNight`, `textSize` | written by SETTINGS | |
| DataStore `token` + `userId` | deleted by every logout copy and by account deletion | |

---

## 7. Cross-team dependencies

| Needs | Owner | Escalate when |
|---|---|---|
| the five tab fragments | FEED, MEDIA, PROFILE | when a tab is added, removed or reordered |
| `Util.fontSize` consumption | FEED / HOME_FEED | when font sizing changes |
| `Util.player` stop-on-tab-change | FEED + MEDIA | when playback behaviour changes |
| `Commons().makeWarrior` | PLATFORM / COMMONS | always (its return value is broken — C-12) |
| endpoints, DataStore keys, `Util` statics | PLATFORM | always |
| every screen that copies the overflow menu | FEED, PROFILE | always — 6+ copies |
| Terms/Privacy pages | MEDIA / WEBVIEW | when the entry points change |

---

## 8. Definition of done (team-level)

- [ ] A resource change is checked in **both** `values/` and `values-night/`.
- [ ] A menu change is propagated to **all 6+ copies**, or an extraction is proposed to PM.
- [ ] Tab changes are confirmed with the owning feature team (`TabAdapter` positions are hard-coded).
- [ ] `Util.isNight` / `Util.fontSize` stay consistent with their DataStore keys.
- [ ] Module `.md` updated in the same change, including its Change log.

---

## 9. Team-level known issues

| # | Issue | Module | Risk |
|---|---|---|---|
| AS-1 | The overflow menu + logout handler is **duplicated in 6+ activities** | MAIN_NAV (+ FEED, PROFILE) | inconsistent behaviour; 6 places to fix |
| AS-9 | Account deletion sits behind a plain list dialog with no re-authentication | SETTINGS | **destructive action, weak guard** |
| AS-6 | `MainActivity.onBackPressed` calls **`exitProcess(-1)`** | MAIN_NAV | kills the process, skips lifecycle |
| AS-3 | `Util.user.isReviewState.toBoolean()` is unguarded in every menu copy | MAIN_NAV + copies | NPE after process death |
| AS-5 | `TabAdapter.getItem` ends with `else -> b as Fragment` where `b` is `null` | MAIN_NAV | crash if a tab is added |
| AS-4 | `TabAdapter` positions are hard-coded in **two** places (the adapter's `when` and `MainActivity`'s `addTab` order) | MAIN_NAV | easy to desync |
| AS-2 | Theme and font changes **restart `MainActivity`** instead of recreating in place | SETTINGS | jarring, loses state |
| AS-7 | `getMyDetails` copies #3 (MAIN_NAV) and #9 (ABOUT) | MAIN_NAV, ABOUT | drift (AUTH A4) |
| AS-8 | Font size is applied **only** to post content/tags; the rest of the app ignores it | SETTINGS, THEMING | inconsistent accessibility |
| AS-10 | `ExpandableView.java` is a custom view whose usage is not visible in `activity_about.xml` | ABOUT | possibly dead code |

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial APPSHELL team lead agent: charter, 4 modules, the shell diagram with all five tab owners, the overflow-menu duplication (6+ copies, the team's biggest liability), the theming model (`Util.isNight` / `Util.fontSize` + DataStore), a 4-endpoint API surface, and 10 team-level issues. Confirmed `preview_image.xml` belongs to MAIN_NAV. |
| APPSHELL team complete | `MAIN_NAV.md`, `SETTINGS.md`, `ABOUT.md` and `THEMING.md` written and verified. Two ownership corrections: **`preview_image.xml`** is MAIN_NAV's (not FEED's) and **`ExpandableView.java` is not an About component** — its only consumer is FEED's `HomeAdapter`. Key findings escalated to PM: `exitProcess(-1)` on Back, the unregistered rotation `ContentObserver`, **account deletion with no re-authentication**, and **`@color/black` / `@color/white` being inverted in night mode**. **All 4 APPSHELL module agents are READY — the hierarchy is complete.** |


