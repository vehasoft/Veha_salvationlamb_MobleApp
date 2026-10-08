# MAIN_NAV Module Agent

> Team: **APPSHELL** · Reports to: `agents/APPSHELL/APPSHELL_LEAD.md`
> This agent knows **every detail** of `MainActivity` — the app's hub: the 5-tab pager, the
> app-wide overflow menu, night-mode application, the warrior banner, the first-run dialog,
> rotation handling and permissions.
> It may only edit the files listed in §11 **Owned files**.

---

## ⚠ This screen is the app's hub

Every signed-in session lives here. It hosts **five fragments owned by three other teams**, applies
the theme for the whole app, and is the **origin of the overflow menu copied into 6+ activities**.

Changes here are felt everywhere — treat them as cross-team by default.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Home / main shell** |
| Classes | `com.veha.activity.MainActivity` (427 lines) + `com.veha.adapter.TabAdapter` (44 lines) |
| Layouts | `activity_main.xml` (10 ids) + `preview_image.xml` (first-run dialog) |
| Manifest | `exported="true"` (no intent-filter — BUILD_CONFIG B-7) with `configChanges="orientation\|screenSize\|screenLayout\|keyboardHidden\|uiMode"` |
| View access | `findViewById` **and** Kotlin synthetics (`banner`, `makewarrior`, `menu`, …) |
| Entered from | `SplashhScreenActivity`, `LoginActivity`, `OTP_VERIFY`, `AddPostActivity`, `SettingsActivity`, and every `prod_logo` tap in the app |
| Exits to | `SearchActivity`, plus the 5 overflow destinations |
| Progress | `SpotsDialog` — **`dismiss()` is called twice in a row** in `onCreate` (issue MN1) |

---

## 2. UI inventory (`activity_main.xml`)

| View id | Type | Purpose |
|---|---|---|
| `header_main` | container | top bar |
| `prod_logo` | `ImageView` | app logo |
| `search` | icon | -> `SearchActivity` [SEARCH] |
| `menu` | icon | the app-wide overflow menu |
| `banner` | container | the "Make Me Warrior" strip |
| `makewarrior_gif` | GIF view | animated banner art (`android-gif-drawable`) |
| `makewarrior` | `TextView` | banner text — changes by warrior state |
| `banner_close` | control | dismisses the banner |
| `viewPager` | `ViewPager` (v1) | hosts the five tab fragments |
| `tabLayout` | `TabLayout` | the five bottom tabs |

### The warrior banner — three states

| Condition | Behaviour |
|---|---|
| `Util.isWarrior` | `banner` **GONE** |
| `Util.user.isReviewState` | text -> `"Your warrior request is \nwaiting for admin approval"`, GIF hidden |
| otherwise | banner VISIBLE, text `"Make Me Warrior"`, GIF visible |

`Util.user.isReviewState.toBoolean()` is read **without a null check** (issue MN2).

---

## 3. The tabs

Built in `onCreate`, with icons and tags, then added in a specific order:

| Position | Tag | Icon | Fragment (via `TabAdapter`) | Owner |
|---|---|---|---|---|
| 0 | `"Home"` | `ic_baseline_home_24` | `HomeFragment.getInstance("user")` | FEED |
| 1 | `"Files"` | `ic_baseline_folder_24` | `FilesFragment()` | MEDIA |
| 2 | `"video"` | `ic_baseline_video_library_24` | `AdminVideoFragment()` | MEDIA |
| 3 | `"audio"` | `ic_baseline_audio_file_24` | `AdminAudioFragment()` | MEDIA |
| 4 | `"Profile"` | `profile` | `ProfileFragment.getInstance(Util.userId, "me")` | PROFILE |

| Fact | Detail |
|---|---|
| Double source of truth | the order lives in **`MainActivity`'s `addTab` calls** *and* in **`TabAdapter`'s `when`** — they must agree (issue MN3) |
| `TabAdapter.getItem` | ends with `else -> b as Fragment` where `b` is a `null` local — an **NPE** if a 6th tab appears (issue MN4) |
| Tag casing | `"Home"`, `"Files"`, `"Profile"` are capitalised; `"video"` and `"audio"` are not (issue MN5) |
| `getDrawable(...)` | the deprecated `Context` overload (issue MN6) |
| On tab change | `dialog.dismiss()` and **`Util.player.stop()`** — stopping audio but **not releasing it** (issue MN7) |
| `onTabUnselected` / `onTabReselected` | both empty |

---

## 4. Night mode (applied for the whole app)

```kotlin
when (Util.isNight) {
    Util.DAY     -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
    Util.NIGHT   -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
    Util.DEFAULT -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
}
super.onCreate(savedInstanceState)
```

| Fact | Detail |
|---|---|
| Source | `Util.isNight`, seeded by **SPLASH** from DataStore and written by **SETTINGS** |
| Timing | set **before** `super.onCreate`, which is correct |
| No `else` | an unexpected value leaves the previous mode in place (issue MN8) |
| Scope | because it is `setDefaultNightMode`, this single block themes the **entire app** |

---

## 5. Rotation handling (unique to this screen)

```kotlin
if (Settings.System.getInt(contentResolver, ACCELEROMETER_ROTATION, 0) == 1)
    requestedOrientation = SCREEN_ORIENTATION_SENSOR
else
    requestedOrientation = SCREEN_ORIENTATION_PORTRAIT

contentResolver.registerContentObserver(
    Settings.System.getUriFor(ACCELEROMETER_ROTATION), true, rotationObserver)
```

The app **follows the system auto-rotate setting manually**, and registers a `ContentObserver` to
react to changes. It is the only screen that does this.

**The observer is never unregistered** in `onDestroy` — a leak (issue MN9).

---

## 6. The first-run dialog

```kotlin
if (Util.isFirst != null && Util.isFirst) {
    if (Util.isWarrior) {
        val nagDialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar)
        nagDialog.setContentView(R.layout.preview_image)   // <- APPSHELL's, not FEED's
        img.setImageResource(R.drawable.covre_pic)         // <- typo in the asset name
        btnClose.setOnClickListener { firstTime(); nagDialog.dismiss() }
        nagDialog.show()
    } /* else { Commons().makeWarrior(...); firstTime() } */   // commented out
}
```

| Fact | Detail |
|---|---|
| Shown to | **warriors only** — non-warriors see nothing, and the `else` is commented out (issue MN10) |
| Layout | `preview_image.xml` (`iv_preview_image`, `btnIvClose`) — **owned here**, not by FEED |
| Asset | `R.drawable.covre_pic` — a misspelling of "cover" (issue MN11) |
| Dismiss | calls `firstTime()` -> `PUT users/freshUser/{userId}` to clear the flag |
| Non-cancelable | the only way out is the close button |

---

## 7. API calls

| Method | Endpoint | Purpose |
|---|---|---|
| `getMyDetails()` | `GET users/{userId}` | **copy #3** of that block; refreshes `Util.user` and clears the session on non-200 |
| `firstTime()` | `PUT users/freshUser/{userId}` | clears the fresh-user flag after the dialog |

`getMyDetails` here is one of the few copies that **does** clear the session on failure —
`SPLASH.md` A27 notes Splash does not.

---

## 8. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | rotation setup -> register observer -> **night mode** -> `super` -> layout -> `UserPreferences` -> SpotsDialog (dismissed twice) -> `checkPermission()` -> `getMyDetails()` -> first-run dialog -> banner state -> logo/search/menu wiring -> build 5 tabs -> `TabAdapter` -> pager/tab listeners |
| `onPause` / `onResume` / `onDestroy` | `dialog.dismiss()` only |
| **`onBackPressed`** | `if (viewPager.currentItem == 0) exitProcess(-1) else viewPager.currentItem = 0` |

> **`exitProcess(-1)`** force-kills the process from the Home tab — skipping `onDestroy`, lifecycle
> teardown and any pending DataStore writes. `finish()` or `moveTaskToBack(true)` is the normal
> idiom (issue MN12).

`checkPermission()` / `requestPermission()` / `permissions()` handle runtime permissions here — the
**only** place in the app that actually requests them (contrast ADD_POST AP4) (issue MN13).

---

## 9. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `HomeFragment` | FEED / HOME_FEED | tab 0 |
| `FilesFragment`, `AdminVideoFragment`, `AdminAudioFragment` | MEDIA | tabs 1–3 |
| `ProfileFragment` | PROFILE / MY_PROFILE | tab 4 |
| `SearchActivity` | SEARCH / SEARCH_ENTRY | the search icon |
| `Commons().makeWarrior` | PLATFORM / COMMONS | banner + menu |
| `Util.isNight`, `isFirst`, `isWarrior`, `user`, `player` | PLATFORM / COMMONS | state |
| `RetrofitAPI.getUser`, `putFreshUser` | PLATFORM / NETWORK | the 2 calls |
| `UserPreferences` | PLATFORM / STORAGE | token + logout |
| `main_menu.xml`, `menuStyle`, tab icons, `covre_pic` | APPSHELL / THEMING | resources |
| `android-gif-drawable` | PLATFORM / BUILD_CONFIG | the banner GIF |

---

## 10. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| MN12 | `onBackPressed` calls **`exitProcess(-1)`**, killing the process | **High** | `finish()` or `moveTaskToBack(true)` |
| MN9 | The rotation `ContentObserver` is **never unregistered** | **High** | unregister in `onDestroy` |
| MN4 | `TabAdapter.getItem`'s `else -> b as Fragment` NPEs for any new position | **High** | handle all positions explicitly |
| MN2 | `Util.user.isReviewState.toBoolean()` unguarded — NPE after process death | **High** | null-guard |
| AS-1 | The ~60-line overflow menu is duplicated into 5 other activities from here | **High** | extract a shared handler (PM-level) |
| MN7 | On tab change `Util.player.stop()` is called but the player is **not released or nulled** | **High** | release, or delegate to the fragments |
| MN3 | Tab order is defined in **two** places that must agree | Medium | one source of truth |
| MN10 | The first-run dialog is shown **only to warriors**; the non-warrior branch is commented out | Medium | confirm intent with the customer |
| MN8 | The night-mode `when` has no `else` | Medium | add a default |
| MN13 | Permissions are requested only here, though ADD_POST and EDIT_PROFILE need them | Medium | request at point of use |
| MN14 | Mixed `findViewById` and synthetics in one class | Medium | pick one |
| MN6 | Deprecated `getDrawable(...)` | Low | `ContextCompat.getDrawable` |
| MN5 | Inconsistent tab-tag casing | Low | align |
| MN11 | `R.drawable.covre_pic` is misspelled | Low | rename (THEMING) |
| MN1 | `dialog.dismiss()` is called twice consecutively | Low | delete one |
| MN15 | No `onSaveInstanceState` for the selected tab | Low | save `currentItem` |

---

## 11. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/MainActivity.kt` | **exclusive** |
| `app/src/main/java/com/veha/adapter/TabAdapter.kt` | **exclusive** |
| `app/src/main/res/layout/activity_main.xml` | **exclusive** |
| `app/src/main/res/layout/preview_image.xml` | **exclusive** (used only by the first-run dialog) |
| `<activity android:name=".MainActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** the five tab fragments (FEED, MEDIA, PROFILE), `main_menu.xml` and all
resources (**APPSHELL/THEMING**), `Util.java` / `Commons.kt` / `UserPreferences.kt` (PLATFORM), and
the five other activities that copy this screen's overflow menu.

---

## 12. How to make common changes

**Add, remove or reorder a tab:** edit **both** the `addTab` order here **and** `TabAdapter`'s
`when` (MN3), remove the `null as Fragment` branch (MN4), add an icon via THEMING, and confirm the
fragment with its owning team. Note `ProfileFragment` needs `Util.userId`, so new tabs may need
state too.

**Change the warrior banner:** text and visibility are local; the **action** is
`Commons().makeWarrior`, which is PLATFORM-owned and whose return value is broken (C-12).

**Change night-mode behaviour:** the `when` here is the single point where the mode is applied —
but the **value** comes from SETTINGS via `Util.isNight` + DataStore. Two-agent change inside
APPSHELL.

**Fix Back (MN12):** replace `exitProcess(-1)` with `moveTaskToBack(true)` (or `finish()`), keeping
the "jump to Home tab first" behaviour. Local and high-value.

**Extract the overflow menu (AS-1):** the right place to start is here, since this is the original.
It then needs 5 follow-up edits in FEED and PROFILE files — **PM-coordinated**.

---

## 13. Change log

| Change | Detail |
|---|---|
| Created | Initial MAIN_NAV module agent documented from `MainActivity.kt` (427 lines), `TabAdapter.kt` (44 lines) and `activity_main.xml` (10 ids): all 5 tabs with their owners and icons, the three warrior-banner states, app-wide night-mode application, the manual auto-rotate observer, the warriors-only first-run dialog using `preview_image.xml`, `exitProcess(-1)` on Back, and 16 known issues. Confirmed `preview_image.xml` is owned here, not by FEED. |



