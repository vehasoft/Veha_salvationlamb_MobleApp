# COMMONS Module Agent

> Team: **PLATFORM** · Reports to: `agents/PLATFORM/PLATFORM_LEAD.md`
> This agent knows **every detail** of `Commons.kt` and `Util.java`: the connectivity guard, the
> warrior dialog, the date helpers, the four validators, the religion list and all global static
> state.
> It may only edit the files listed in §8 **Owned files**.

---

## ⚠ Blast-radius warning

These two files are the app's **grab-bag of shared behaviour and global state**:

- `Commons()` is instantiated at **62 sites** (almost all for `isNetworkAvailable`).
- `Util.userId`, `Util.user` and `Util.isWarrior` are read by nearly every screen.
- The four validators are the **only** input rules in the app.

**Every change needs PM sign-off.** Changing `isValidPassword` alone silently changes LOGIN,
REGISTER, CHANGE_PASSWORD and RESET_PASSWORD at once.

> **Shared file:** `Util.java` also contains `getRetrofit()`, the `retrofitAPI` field and `url`,
> which are owned by **`NETWORK.md`**. This agent owns everything else in the file. Consult the lead
> before editing `Util.java`.

---

## 1. Identity

| Item | Value |
|---|---|
| Role | shared helpers + global in-memory state |
| Files | `util/Commons.kt` (215 lines, Kotlin class) and `util/Util.java` (158 lines, Java static utility) |
| Instantiation | `Commons()` is a **plain class**, constructed on demand — 62 sites; `Util` is all-static |
| Mixed languages | the only PLATFORM module spanning Kotlin and Java |

---

## 2. `Util.java` — constants, globals, helpers

### 2.1 Constants

| Constant | Value | Used for |
|---|---|---|
| `DAY` | `"Day"` | theme mode |
| `NIGHT` | `"Night"` | theme mode |
| `DEFAULT` | `"Default"` | theme mode (follow system) |
| `WARRIOR` | `"Warrior"` | role label |
| `USER` | `"User"` | role label |

### 2.2 Global mutable state (the app's de-facto session object)

| Field | Type | Default | Set by | Read by |
|---|---|---|---|---|
| `userId` | `String` | `null` | LOGIN, SPLASH | nearly every screen |
| `user` | `UserRslt` | `null` | LOGIN, SPLASH, REGISTER | PROFILE, EDIT_PROFILE, APPSHELL |
| `isWarrior` | `Boolean` | `false` | LOGIN, SPLASH | FEED, PROFILE, APPSHELL |
| `isFirst` | `Boolean` | `true` | LOGIN, SPLASH | PROFILE, APPSHELL |
| `isNight` | `String` | `DAY` | SPLASH, SETTINGS | THEMING, MAIN_NAV |
| `fontSize` | `Float` | `10.0F` | SPLASH, SETTINGS | THEMING, FEED |
| `listview` | `boolean` | `true` | FEED | FEED (list/grid toggle) |
| `player` | `MediaPlayer` | `null` | MEDIA | MEDIA (audio playback) |
| `url` | `String` | `https://server.salvationlamb.com` | — | **NETWORK-owned** |
| `religion` | `ArrayList<String>` | private | `getReligion()` | EDIT_PROFILE, warrior dialog |

**None of this survives process death.** Android can kill the process and restore the activity
stack, at which point `Util.userId` is `null` while the screen still believes the user is signed in
(issue C-1). `Util.player` being a static `MediaPlayer` is also a classic leak (issue C-2).

### 2.3 Validators (the app's only input rules)

| Method | Regex | Used by |
|---|---|---|
| `isValidEmail` | `^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,6}$` (case-insensitive, `.find()`) | LOGIN, REGISTER |
| `isValidName` | `^[a-zA-Z\s]+\.?$` | REGISTER, EDIT_PROFILE |
| `isValidPassword` | `^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\S+$).{8,20}$` | LOGIN, REGISTER, CHANGE_PASSWORD, RESET_PASSWORD |
| `isValidMobile` | `^(\+?\d{1,4}[\s-])?(?!0+\s+,?$)\d{10}\s*,?$` | REGISTER, EDIT_PROFILE |

All four use `.find()` rather than `.matches()`; the anchors make that equivalent here, but it is
fragile (issue C-3). `isValidPassword` caps length at **20** and requires one of `@#$%^&+=`
specifically — other symbols fail (issue C-4).

### 2.4 Date helpers

| Method | Behaviour |
|---|---|
| `formatDate(date, toPattern, fromPattern)` | `SimpleDateFormat` reformat; returns `null` for empty input; **throws `ParseException`** |
| `getTimeAgo(date)` | "N seconds/minutes/hours ago", "yesterday", else `dd MMM yyyy, hh:mm a`; swallows exceptions and returns `""` |

Both use `SimpleDateFormat` with the **default locale** — in a non-Gregorian locale (e.g. Thai
Buddhist) output and parsing can break (issue C-5). `getTimeAgo` has a gap: **exactly 24h–48h**
falls into `days == 1` -> "yesterday", which is correct, but the `seconds < 60` branch also catches
negative values from clock skew, showing "-5 seconds ago" (issue C-6).

### 2.5 Other

| Method | Behaviour |
|---|---|
| `getReligion()` | returns a hard-coded `ArrayList` of **36** denominations, starting with `"Select"`; rebuilt on every call (issue C-7) |
| `getVideo(url)` | prefixes `https://salvationlamb.com/video/` — a **second hard-coded host**, unrelated to `Util.url` (issue C-8) |

---

## 3. `Commons.kt` — connectivity, warrior dialog, date extensions

### 3.1 `isNetworkAvailable(context): Boolean` — the most-called function in the app

```kotlin
fun isNetworkAvailable(context: Context?): Boolean {
    val cm = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        if (caps != null) when {
            caps.hasTransport(TRANSPORT_CELLULAR) -> return true
            caps.hasTransport(TRANSPORT_WIFI)     -> return true
            caps.hasTransport(TRANSPORT_ETHERNET) -> return true
        }
    } else {
        if (cm.activeNetworkInfo?.isConnected == true) return true
    }
    Toast.makeText(context, "No Internet ", Toast.LENGTH_LONG).show()   // <-- side effect
    return false
}
```

| Fact | Detail |
|---|---|
| Call sites | ~**62**, guarding essentially every network call |
| **Side effect** | it **toasts `"No Internet "`** (with a trailing space) before returning false — a utility that touches the UI (issue C-9) |
| Consequence | screens that guard a call and then return silently *do* show this toast — so "offline is silent" findings in the screen agents mean *no screen-specific* message; the generic toast still appears |
| Reliability | checks transport presence, **not** actual internet reachability — a captive portal reads as online (issue C-10) |
| `context!!` | force-unwrap; a null context crashes (issue C-11) |

### 3.2 `makeWarrior(context, owner): String` — a 100-line UI dialog inside a util class

Two chained `AlertDialog`s:

1. **Intro** — title `"BECOME A WARRIOR"`, body `@string/make_me_warrior`, buttons **"I agree"** / **"Skip"**.
2. **Form** (`R.layout.child_warrior`) — a religion `Spinner` (from `Util.getReligion()`), a
   `church` EditText, an `err_rel` error TextView, and **5 checkboxes** `gift1`–`gift5`, where
   ticking `gift1` disables and clears the rest.

Validation: religion must not be empty/`"Select"` (shows `err_rel` in red); `church` must not be
empty (`"Please Enter ChurchName"`). On success it builds
`{userId, isWarrior: true, religion, churchName, gift}` and calls `makeMeWarior(...)`.

`makeMeWarior` posts to `users/warrior` with a Bearer token, toasting **"Waiting for Admin Approval"**
on 200.

| Problem | Detail |
|---|---|
| Return value is useless | `makeWarrior` returns `status`, assigned **asynchronously** inside the Retrofit callback — it is always `""` at return. `MainActivity:185` does `if (Commons().makeWarrior(...).contentEquals("success"))`, which **can never be true** (issue C-12) |
| Same broken token guard as CHANGE_PASSWORD | `if (!TextUtils.isEmpty(it) \|\| !it.equals("null") \|\| !it.isNullOrEmpty())` is **always true** (issue C-13) |
| Observer per call | registers a new `authToken` LiveData observer each invocation (issue C-14) |
| No error feedback | a non-200 only sets a local variable; the user sees nothing (issue C-15) |
| 9 call sites | `MainActivity` (x3), `EditProfileActivity` (x2), `FavoritesActivity`, `FollowerActivity`, `ViewProfileActivity`, `ViewLikesActivity` |
| UI in PLATFORM | a dialog, a layout and copy all live in a util class (issue C-16) |

### 3.3 Date extensions

`getDate(date)` -> `date.toDate().formatTo()`, converting an ISO-8601 UTC string to the device
timezone. Both extensions are declared as **members** of `Commons`, so they are only usable on a
`Commons` instance (issue C-17).

### 3.4 Dead code

`private fun showAlert(context: Context) {}` — an empty, unused function (issue C-18).

---

## 4. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `RetrofitAPI.postWarrior` + `Util.getRetrofit()` | PLATFORM / NETWORK | the warrior call |
| `UserPreferences.authToken` | PLATFORM / STORAGE | bearer token in `makeMeWarior` |
| `UserRslt` | PLATFORM / DATA_MODELS | the type of `Util.user` |
| `R.layout.child_warrior`, `@string/make_me_warrior` | APPSHELL / THEMING | the warrior dialog UI |
| `ConnectivityManager` | Android | connectivity check |

---

## 5. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| C-21 | **NEW (T-025, `G13`)** — ~~`Util.hasPermission()` **failed open**: `if (permissionMap == null \|\| permissionMap.isEmpty()) return true;`. A failed `GET /api/v1/permission/users/{userId}` at splash left the map empty, so **all 34 permission gates granted access**, including admin-only screens~~ — **FIXED 2026-10-08**: now fail-CLOSED, backed by a new `permissionsLoaded` flag that distinguishes "not fetched yet" from "fetched, user has nothing". New API: `setPermissionMap()`, `clearPermissions()`, `isPermissionsLoaded()`. Covered by **10 unit tests** in `app/src/test/java/com/veha/util/UtilPermissionTest.kt` | ~~**Critical (security)**~~ | — |
| C-22 | **NEW (T-025)** — `permissionMap` is still a **public mutable static**, so any screen can bypass `setPermissionMap()` and leave `permissionsLoaded` stale | Medium | make the field private and route all writes through the setter |
| C-12 | `makeWarrior` returns a status assigned **asynchronously**, so it is always `""`; `MainActivity:185`'s `contentEquals("success")` can never be true | **High** | take a callback/`suspend`, or move to a ViewModel |
| C-13 | The token guard is an always-true `||` chain (same bug as CHANGE_PASSWORD) | **High** | use `&&`, or test the token explicitly |
| C-1 | `Util` statics are the de-facto session object but do **not** survive process death | **High** | re-hydrate from DataStore on resume, or stop relying on statics |
| C-9 | `isNetworkAvailable` **toasts from a utility**, mixing policy with UI at 62 call sites | **High** | return a result and let screens decide; the trailing space in `"No Internet "` is also a typo |
| C-16 | `makeWarrior` is 100 lines of dialog UI inside PLATFORM | **High** | move to PROFILE as a real screen/dialog fragment |
| C-2 | `Util.player` is a **static `MediaPlayer`** that is never guaranteed to be released | High | own it in the MEDIA screen |
| C-14 | A new `authToken` observer is registered on every `makeWarrior` call | Medium | one-shot read |
| C-15 | A failed warrior request shows the user nothing | Medium | toast on error and `onFailure` |
| C-10 | Connectivity is inferred from transport presence, not reachability | Medium | `NET_CAPABILITY_VALIDATED` |
| C-4 | `isValidPassword` silently caps length at 20 and allows only `@#$%^&+=` as symbols; no screen tells the user about the cap | Medium | widen the symbol class, document the rule |
| C-5 | `SimpleDateFormat` with the default locale in `formatDate` / `getTimeAgo` | Medium | pass `Locale.US` for machine formats |
| C-6 | `getTimeAgo` can render negative durations on clock skew, and swallows all exceptions | Low | clamp at 0, log properly |
| C-3 | Validators use `.find()` instead of `.matches()` | Low | switch to `matches` |
| C-7 | `getReligion()` rebuilds a 36-item list on every call and assigns a private static | Low | make it an immutable constant |
| C-8 | `getVideo()` hard-codes a **second** host, independent of `Util.url` | Medium | move to NETWORK config |
| C-11 | `isNetworkAvailable(context!!)` force-unwraps | Low | take a non-null `Context` |
| C-17 | `toDate()` / `formatTo()` are instance members, not top-level extensions | Low | move to a file-level extension |
| C-18 | Empty `showAlert()` dead code | Cosmetic | delete |
| C-19 | `Commons` is instantiated 62 times (`Commons().isNetworkAvailable(...)`) though it is stateless | Low | make it an `object` |
| C-20 | `Util.java` mixes four unrelated concerns (constants, state, validators, networking) in 158 lines | Medium | split into `Session`, `Validators`, `DateUtils` |

---

## 6. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/util/Commons.kt` | **exclusive** |
| `app/src/main/java/com/veha/util/Util.java` | **shared with `NETWORK.md`** — this agent owns everything **except** `getRetrofit()`, the `retrofitAPI` field and `url` |

**Not owned (escalate):** `RetrofitAPI.kt` (NETWORK), `UserPreferences.kt` (STORAGE),
`DataModels.kt` (DATA_MODELS), `child_warrior.xml` and `strings.xml` (APPSHELL/THEMING), and every
screen that calls these helpers.

---

## 7. How to make common changes

**Change a validation rule:** edit the regex in `Util.java`. **Name every affected screen** in the
report — `isValidPassword` alone touches LOGIN, REGISTER, CHANGE_PASSWORD and RESET_PASSWORD, and
loosening it can lock out existing users. PM sign-off required.

**Add a validator:** add a `public static boolean isValidX(String)` beside the others and document
it in §2.3. Additive and low-risk.

**Add a global static:** possible, but push back first — §2.2 already carries 10 fields with no
lifecycle (C-1). If it is per-user session data it probably belongs in **STORAGE**, not here.

**Stop `isNetworkAvailable` from toasting (C-9):** removing the toast makes ~62 call sites silently
do nothing when offline, because most screens have no offline message of their own. This must be a
**coordinated multi-team change**: PM first, then each screen adds its own feedback.

**Fix `makeWarrior`'s return value (C-12):** change the signature to accept a callback
`(Boolean) -> Unit`. That breaks **9 call sites** across PROFILE, FEED and APPSHELL — PM sign-off and
simultaneous updates required.

**Add a religion:** append to the list in `Util.getReligion()`. Cosmetic and safe; confirm the
backend accepts the new value.

**Change the video host (C-8):** `Util.getVideo()` is independent of `Util.url`; changing one does
**not** change the other. Coordinate with NETWORK.

---

## 8. Change log

| Change | Detail |
|---|---|
| T-025 (2026-10-08) | **`G13` fail-open permissions fixed.** `Util.hasPermission()` now denies when the permission map has not been loaded, instead of returning `true`. Added `permissionsLoaded`, `setPermissionMap()`, `clearPermissions()`, `isPermissionsLoaded()` to `Util.java`. Verified by 10 new unit tests (`UtilPermissionTest.kt`) — mutation-tested by reintroducing the old `return true` and confirming 4 tests fail. Issues `C-21` (fixed) and `C-22` (new, follow-up) recorded. |
| Created | Initial COMMONS module agent documented from `Commons.kt` (215 lines) and `Util.java` (158 lines): 5 constants, 10 global statics with their writers/readers, 4 validators with regexes, 2 date helpers, the 36-item religion list, `getVideo`'s second host, the 62-site `isNetworkAvailable` guard with its hidden toast, the 9-call-site `makeWarrior` dialog and its always-`""` return value, and 20 known issues. |


