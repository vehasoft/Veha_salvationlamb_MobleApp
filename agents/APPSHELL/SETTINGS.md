# SETTINGS Module Agent

> Team: **APPSHELL** · Reports to: `agents/APPSHELL/APPSHELL_LEAD.md`
> This agent knows **every detail** of the Settings screen: the four rows, the font-size and theme
> pickers, and **account deletion**.
> It may only edit the files listed in §9 **Owned files**.

---

## ⚠ This screen can delete the user's account

`SettingsActivity` is the **only** screen that calls `DELETE api/v1/users/{userId}`. The guard is a
plain confirmation dialog — no password, no re-authentication, no typed confirmation. Treat every
change near `deleteAccount()` as high-risk (team issue AS-9).

> **No permission gate here is BY DESIGN** — customer-confirmed 2026-10-08: *"Everyone has the
> right to delete their own account."* `SettingsActivity` contains **zero** `Util.hasPermission`
> calls, and that is intentional: the endpoint acts on `Util.userId`, i.e. the caller's **own**
> account, so there is nothing to authorise. Do not add a gate. (The weak confirmation UX in
> `AS-9` is a separate matter and still stands.)

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Settings** |
| Class | `com.veha.activity.SettingsActivity` (197 lines) |
| Layout | `app/src/main/res/layout/activity_settings.xml` (7 ids) |
| Manifest | `<activity android:name=".SettingsActivity" android:exported="false"/>` |
| View access | `findViewById` + a synthetic for `menu` |
| Required extras | **none** |
| Entered from | the overflow menu (`settings`) on 6+ screens |
| Exits to | `ChangePasswordActivity` [AUTH], `MainActivity` (after a theme/font change, and via the logo), `LoginActivity` (after deletion) |

---

## 2. UI inventory (`activity_settings.xml`)

| View id | Type | Row | Action |
|---|---|---|---|
| `header_main` | container | — | header bar |
| `prod_logo` | `ImageView` | — | -> `MainActivity` |
| `menu` | icon | — | the app-wide overflow menu |
| `change_font_size` | row | **Change Font Size** | a 3-item picker (§3) |
| `change_password` | row | **Change Password** | -> `ChangePasswordActivity` with **no extras** (signed-in mode) |
| `settings_theme` | row | **Theme** | a 3-item picker (§4) |
| `settings_delete` | row | **Delete account** | the destructive dialog (§5) |

Four rows, no switches, no section headers, no version number (issue ST1).

---

## 3. Font size

```kotlin
val items = arrayOf<CharSequence>("small", "medium", "large")
AlertDialog.Builder(this).setTitle("Change Font Size").setItems(items) { _, item ->
    lifecycleScope.launch {
        when (items[item]) {
            "small"  -> { userPreferences.saveTextSize(10.0F); Util.fontSize = 10.0F }
            "medium" -> { userPreferences.saveTextSize(15.0F); Util.fontSize = 15.0F }
            "large"  -> { userPreferences.saveTextSize(20.0F); Util.fontSize = 20.0F }
        }
        startActivity(Intent(this@SettingsActivity, MainActivity::class.java)); finish()
    }
}.show()
```

| Fact | Detail |
|---|---|
| Values | **10 / 15 / 20 F**, hard-coded twice each (DataStore + `Util`) |
| Labels | lower-case `"small"` / `"medium"` / `"large"`, compared by **string** (issue ST2) |
| Apply | **restarts `MainActivity`** and finishes Settings (issue ST3 / AS-2) |
| Scope | `Util.fontSize` is read **only** by FEED's `HomeAdapter` for post content and tags — nothing else resizes (issue ST4 / AS-8) |
| Current value | the dialog does **not** show which size is selected (issue ST5) |

---

## 4. Theme

```kotlin
val items = arrayOf<CharSequence>(Util.DAY, Util.NIGHT, Util.DEFAULT)
Log.e("mode", AppCompatDelegate.getDefaultNightMode().toString())
... setItems { _, item ->
    lifecycleScope.launch {
        when (items[item]) {
            Util.DAY     -> { Util.isNight = Util.DAY;     userPreferences.saveIsNightModeEnabled(Util.DAY) }
            Util.NIGHT   -> { Util.isNight = Util.NIGHT;   userPreferences.saveIsNightModeEnabled(Util.NIGHT) }
            Util.DEFAULT -> { userPreferences.saveIsNightModeEnabled(Util.DEFAULT); Util.isNight = Util.DEFAULT }
        }
    }
}
```

| Fact | Detail |
|---|---|
| Options | `"Day"` / `"Night"` / `"Default"` — the `Util` constants, so labels and values are the same strings |
| Writes | **both** `Util.isNight` and DataStore `isNight` |
| Apply | the mode itself is applied by **`MAIN_NAV`** on next create, not here (issue ST6) |
| Dead code | a large commented-out block reads `Configuration.UI_MODE_NIGHT_MASK` to resolve "Default" manually (issue ST7) |
| Debug log | `Log.e("mode", ...)` left in place (issue ST8) |
| Current value | again not indicated in the dialog (issue ST5) |

> The `DEFAULT` branch writes DataStore **before** `Util.isNight`, while the other two do the
> reverse — harmless, but inconsistent (issue ST9).

---

## 5. Account deletion

| Step | Behaviour |
|---|---|
| Trigger | the `settings_delete` row |
| Confirmation | `AlertDialog` — **"Do you want to delete your account permanently?"** |
| Call | `deleteUser("Bearer $token", Util.userId)` -> `DELETE api/v1/users/{userId}` |
| On 200 | `deleteAuthToken()` + `deleteUserId()`, then `LoginActivity` |
| Guard | the usual `isNetworkAvailable` + a fresh `authToken` observer |

| Problem | Detail |
|---|---|
| No re-auth | a single tap plus "Yes" is enough — no password, no typed confirmation (issue ST10 / AS-9) |
| No feedback | a non-200 branch exists but shows **nothing** to the user (issue ST11) |
| Irreversible | the user is not told the action cannot be undone beyond the word "permanently" |
| Local state | `Util.user`, `Util.userId` and the other globals are **not** cleared, only DataStore (issue ST12) |

---

## 6. Storage & global state

| Item | Operation |
|---|---|
| DataStore `textSize` | **written** (10/15/20) |
| DataStore `isNight` | **written** (`Day`/`Night`/`Default`) |
| DataStore `token` + `userId` | deleted on account deletion and by the menu's logout |
| `Util.fontSize`, `Util.isNight` | **written** |
| `Util.isWarrior`, `Util.user.isReviewState` | read by the overflow menu (unguarded — issue ST13) |

This is the **only writer** of `textSize` and the primary writer of `isNight`.

---

## 7. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `UserPreferences.saveTextSize` / `saveIsNightModeEnabled` | PLATFORM / STORAGE | persistence |
| `Util.DAY/NIGHT/DEFAULT`, `Util.fontSize`, `Util.isNight` | PLATFORM / COMMONS | constants + globals |
| `RetrofitAPI.deleteUser` | PLATFORM / NETWORK | account deletion |
| `MainActivity` | APPSHELL / MAIN_NAV | restart target; applies the theme |
| `ChangePasswordActivity` | AUTH / CHANGE_PASSWORD | the password row (no extras = signed-in mode) |
| `main_menu.xml`, `menuStyle` | APPSHELL / THEMING | overflow menu |
| `HomeAdapter` | FEED / HOME_FEED | the only consumer of `Util.fontSize` |

---

## 8. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| ST10 | **Account deletion needs only a Yes tap** — no re-authentication or typed confirmation | **High** (destructive) | require the password, or a typed confirmation |
| ST11 | Deletion failure shows the user **nothing** | **High** | toast the error |
| ST12 | Deletion clears DataStore but leaves `Util.user` / `Util.userId` populated | **High** | clear global state too |
| ST13 | `Util.user.isReviewState.toBoolean()` unguarded in the menu copy | **High** | null-guard |
| ST14 | The overflow menu is duplicated here too (AS-1) | **High** | shared handler (PM-level) |
| ST3 | Theme and font changes **restart `MainActivity`** | Medium | `recreate()` / live `AppCompatDelegate` update |
| ST4 | Font size affects **only** post content and tags | Medium | apply app-wide, or rename the setting |
| ST5 | Neither picker shows the current selection | Medium | use `setSingleChoiceItems` |
| ST2 | Options are matched by **display string** rather than index or enum | Medium | switch on the index |
| ST6 | Theme is written here but applied in `MainActivity` — split responsibility | Medium | documented; centralise if refactoring |
| ST7 | A large commented-out night-mode resolution block | Low | delete |
| ST9 | The `DEFAULT` branch writes in the opposite order to the others | Low | align |
| ST8 | `Log.e("mode", ...)` debug logging | Low | remove |
| ST1 | No app version, no about link, no notification or privacy settings | Low (product) | ask the customer |

---

## 9. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/SettingsActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_settings.xml` | **exclusive** |
| `<activity android:name=".SettingsActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `UserPreferences.kt` (STORAGE), `Util.java` (COMMONS), `RetrofitAPI.kt`
(NETWORK), `MainActivity.kt` (MAIN_NAV — it applies the theme), `main_menu.xml` (THEMING),
`HomeAdapter.kt` (FEED — the only font-size consumer).

---

## 10. How to make common changes

**Harden account deletion (ST10) — highest value here:** add a password field or a typed
confirmation to the dialog, surface failures (ST11), and clear `Util.user`/`Util.userId` (ST12).
All local to this agent.

**Apply the theme without restarting (ST3):** call `AppCompatDelegate.setDefaultNightMode(...)`
directly here instead of relying on `MainActivity`'s `onCreate`. That overlaps **MAIN_NAV's**
responsibility — coordinate so the mode is not applied twice.

**Add a font size:** add the label, the `Float`, and a `when` branch. Remember the value is written
in **two** places (DataStore and `Util`) and read in **one** (FEED's adapter).

**Show the current selection (ST5):** switch both dialogs to `setSingleChoiceItems` with the index
derived from `Util.fontSize` / `Util.isNight`.

**Add a settings row:** add the view to the layout, bind it, and wire it. If it needs persistence,
a **new DataStore key is a PLATFORM/STORAGE task via PM**.

---

## 11. Change log

| Change | Detail |
|---|---|
| Created | Initial SETTINGS module agent documented from `SettingsActivity.kt` (197 lines) and `activity_settings.xml` (7 ids): the four rows, both 3-option pickers with their hard-coded values and string matching, the **restart-MainActivity** apply mechanism, and the **account-deletion flow with no re-authentication**. 14 known issues. Confirmed this is the only writer of `textSize` and the primary writer of `isNight`. |


