# SPLASH Module Agent

> Team: **AUTH** · Reports to: `agents/AUTH/AUTH_LEAD.md`
> This agent knows **every detail** of the Splash / session-bootstrap screen: the launcher entry,
> the four DataStore observers, the token check, the profile fetch, the routing fork, the global
> state it seeds, and its navigation.
> It may only edit the files listed in §13 **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **Splash** (full-screen logo, no text, no controls) |
| Class | `com.veha.activity.SplashhScreenActivity` (Kotlin, `AppCompatActivity`) — note the **double "h"** in the class name |
| Source | `app/src/main/java/com/veha/activity/SplashhScreenActivity.kt` (133 lines) |
| Layout | `app/src/main/res/layout/activity_splashh_screen.xml` (16 lines) |
| Manifest entry | `<activity android:name=".SplashhScreenActivity" android:exported="true">` with `<intent-filter>` -> `MAIN` + `LAUNCHER` |
| Role | **The app's only launcher entry point.** Every cold start begins here |
| View access | `findViewById` only — **no Kotlin synthetics import**, unlike every other AUTH screen |
| Entered from | the Android launcher (cold start). **No in-app screen ever navigates here** |
| Exits to | `MainActivity` (valid session, verified), `ForgotPasswordActivity` in verify mode (valid session, unverified), `LoginActivity` (no session, or profile fetch failed) |
| Progress indicator | **none** — no SpotsDialog, no spinner; the static logo is the only feedback |
| Theme | inherits `@style/AppTheme` (`Theme.AppCompat.DayNight`) — **no dedicated splash theme / `windowBackground`**, so there is a blank window before the layout inflates (issue S1) |

---

## 2. UI inventory

The simplest layout in the app: 16 lines, **zero ids**, nothing interactive.

| View | Type | Attributes |
|---|---|---|
| root | `androidx.constraintlayout.widget.ConstraintLayout` | `match_parent` x `match_parent`, `android:background="#F0F3F9"` (hard-coded hex), `tools:context="com.veha.activity.SplashhScreenActivity"` |
| *(no id)* | `ImageView` | `match_parent` x `match_parent`, `android:background="#FFFFFF"`, `android:src="@drawable/splash_screen"` (a **PNG**) |

**Notes**
- Two different hard-coded colours (`#F0F3F9` root, `#FFFFFF` image background) that bypass
  `colors.xml` entirely, so the splash **does not respond to night mode** even though the app theme is
  `DayNight` (issue S2).
- No `scaleType` on the `ImageView`, so `splash_screen.png` is stretched to fill the screen and will
  distort on tablets and unusual aspect ratios (issue S3).
- **No progress indicator and no text** — during the mandatory 2-second sleep plus the network call,
  the user has no signal that anything is happening, and no way to tell a slow network from a hang.
- The only view the code touches is `findViewById<View>(android.R.id.content)` — the window's content
  root, not anything from this layout.

---

## 3. Actions / event handlers

There are **no user-facing controls**. Everything is automatic in `onCreate`:

| Step | Behaviour |
|---|---|
| 1 | `setContentView(R.layout.activity_splashh_screen)` |
| 2 | `userPreferences = UserPreferences(this)` |
| 3 | On **API 31+** (`Build.VERSION_CODES.S`): `content.viewTreeObserver.addOnDrawListener { false }` — an attempt to hold the frame; see issue S4 |
| 4 | Observe `isNightModeEnabled` -> seed `Util.isNight` (§6) |
| 5 | Observe `authToken` -> the **main fork** (§4) |
| 6 | (inside the token branch) observe `userId` -> seed `Util.userId`; observe `textSize` -> seed `Util.fontSize` |
| 7 | `Thread.sleep(2000)` on the **main thread**, then `getMyDetails(token)` |

**Back press:** not overridden. During the 2-second sleep the UI thread is blocked, so Back does
nothing until it finishes.

### The API 31+ draw listener (step 3)

```kotlin
val content = findViewById<View>(android.R.id.content)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    content.viewTreeObserver.addOnDrawListener { false }
}
```

`addOnDrawListener` takes an `OnDrawListener` whose `onDraw()` returns `Unit`; the lambda's `false`
is simply a discarded expression. This is almost certainly a mis-port of the
`addOnPreDrawListener { false }` pattern from the official SplashScreen API — **it does not hold the
splash and has no effect** (issue S4). The app does **not** use `androidx.core:core-splashscreen`.

---

## 4. Session bootstrap logic (the heart of this screen)

```
observe authToken
   |-- token empty / "null" ------------------> LoginActivity + finish()
   `-- token present
          observe userId  -> Util.userId  (empty -> LoginActivity + finish())
          observe textSize -> Util.fontSize
          Thread.sleep(2000)
          getMyDetails(token)   GET api/v1/users/{userId}  Bearer <token>
                 |-- 200 + isVerified == true ---> MainActivity + finish()
                 |-- 200 + isVerified == false --> ForgotPasswordActivity (page="verify", email)
                 |-- non-200 --------------------> toast + LoginActivity + finish()
                 `-- onFailure ------------------> nothing (screen hangs, issue S5)
```

### Token check

```kotlin
if (TextUtils.isEmpty(it) || it.equals("null") || it.isNullOrEmpty()) { ... LoginActivity ... }
```

Correctly written with `||` — **contrast with `ChangePasswordActivity`, whose equivalent guard is an
always-true `||` chain of negations** (AUTH issue A19). The `"null"` string check is required because
`UserPreferences.authToken` maps a missing key with `.toString()`, yielding the literal `"null"`.

### The userId sub-check (subtly broken)

Inside the token branch, `userId` is observed and, when empty, `LoginActivity` is started and
`finish()` called — **but `Util.userId = it` executes immediately afterwards anyway**, because there
is no `return@observe`. The flow then continues to `getMyDetails` with an empty `userId`, producing a
request to `api/v1/users/` (issue S6).

### Order-of-execution hazard

`Thread.sleep(2000)` and `getMyDetails(it)` sit **inside the `authToken` observer**, while
`Util.userId` is set by a **separate, nested observer**. Both are LiveData callbacks on the main
thread, so the ordering is not guaranteed by construction — it works today because the nested
observer is registered first and DataStore emits synchronously enough. A DataStore or lifecycle
change could make `getMyDetails` run with a stale/empty `Util.userId` (issue S7).

---

## 5. API contract — `getMyDetails(token: String)`

| Item | Value |
|---|---|
| Retrofit method | `RetrofitAPI.getUser("Bearer $token", Util.userId)` |
| HTTP | `GET api/v1/users/{userId}` |
| Base URL source | `Util.getRetrofit()` -> `https://server.salvationlamb.com` |
| Headers | `Authorization: Bearer <token>` |
| Guard | `try/catch` + `Commons().isNetworkAvailable(this)` |
| Progress | **none** |

**Success (200)**
1. `Gson().fromJson(resp.get("result"), UserRslt::class.java)` -> **`Util.user`**.
2. `Util.isWarrior = loginresp.isWarrior.isNullOrEmpty() || loginresp.isWarrior != "false"`
   -> **anything that is not the literal `"false"` (including empty/null) makes the user a Warrior**
   (issue S8 — identical to LOGIN's L5).
3. `Util.isFirst = loginresp.isFreshUser.toBoolean()`; `lifecycleScope.launch { saveIsFirstTime(...) }`.
4. **A second `Thread.sleep(2000)`** on the main thread, inside the Retrofit callback (issue S9).
5. Routing on `loginresp.isVerified.toBoolean()`:
   - `true` -> `MainActivity`, then `finish()`.
   - `false` -> `ForgotPasswordActivity` with `page = "verify"`, `email = loginresp.email`;
     **no `finish()`** (issue S10).

**Non-200**
- `Log.e("responseee", "fail")`, Toast **"Somthing Went Wrong \nLogin again to continue"** (sic — typo),
  then `LoginActivity` + `finish()`.
- **The stale token and userId are NOT cleared** — unlike `MainActivity`/`HomeFragment`, whose
  equivalent branch calls `deleteAuthToken()` + `deleteUserId()`. So the next cold start repeats the
  same failing round-trip forever (issue S11).

**`onFailure`** — `Log.e("Splashscreen", "fail")` **only**. No toast, **no navigation** — the user is
left staring at the splash logo indefinitely (issue S5). This is the most severe failure mode on this
screen: a server timeout **bricks the app at launch**.

**Offline** — `isNetworkAvailable` false means the method returns immediately and silently; again, no
navigation and no message (issue S12).

### The `getMyDetails` triplication

This method is **byte-for-byte nearly identical** to `LoginActivity.getMyDetails()` and
`MainActivity.getMyDetails()` (AUTH issue A4). Differences: Login has no toast on non-200; Main
clears the session on non-200; Splash toasts but does not clear. **Any fix here must be mirrored in
all three**, or escalated to PM as the shared `SessionBootstrap` refactor.

---

## 6. Storage read / written

DataStore **`SalvationLamb`** via `UserPreferences(this)`. This screen is the app's **primary reader**
— it is where persisted preferences are loaded back into the `Util` globals.

| Key | Type | Operation | Effect |
|---|---|---|---|
| `isNight` | String | **read** (observed) | `Util.isNight = NIGHT / DAY / DEFAULT`; on an unrecognised value it **writes** `Util.DEFAULT` back |
| `token` | String | **read** (observed) | the main session fork; used as the `Bearer` header |
| `userId` | String | **read** (observed, nested) | `Util.userId` |
| `textSize` | Float | **read** (observed, nested) | `Util.fontSize` |
| `isFirst` | Boolean | **write** | `saveIsFirstTime(UserRslt.isFreshUser.toBoolean())` after a successful fetch |

**Nothing is deleted here**, even when the session is proven invalid (issue S11).

> All four reads use `.asLiveData().observe(this)`, so they are **continuous observers, not one-shot
> reads**. Any later write to those keys re-triggers these callbacks while the activity is alive —
> which is how `getMyDetails` can fire more than once (issue S13, AUTH issue A10).

---

## 7. Global state touched (`com.veha.util.Util`)

This screen **seeds almost the entire global state** for a returning user:

| Field | When | Value |
|---|---|---|
| `Util.isNight` | `isNight` observer | `NIGHT` / `DAY` / `DEFAULT` (writes `DEFAULT` back on an unknown value) |
| `Util.userId` | `userId` observer | the stored id — **set even when it is empty** (issue S6) |
| `Util.fontSize` | `textSize` observer | the stored float |
| `Util.user` | profile 200 | the whole `UserRslt` |
| `Util.isWarrior` | profile 200 | the `!= "false"` rule (issue S8) |
| `Util.isFirst` | profile 200 (x2) | `isFreshUser.toBoolean()` |

If this screen is bypassed (e.g. a deep link straight into another activity), none of these are set —
which is why **`SplashhScreenActivity` must remain the only launcher entry**.

---

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| Profile fetch returns non-200 | `Somthing Went Wrong \nLogin again to continue` (sic) | Toast LENGTH_LONG |
| Network failure (`onFailure`) | **(none)** | silent — the app hangs on the splash (issue S5) |
| Offline | **(none)** | silent — same hang (issue S12) |
| While loading (always ≥ 2 s) | **(none)** | no spinner, no text (issue S14) |

The same "Somthing Went Wrong" typo appears in `MainActivity`, `HomeFragment`, `FilesFragment`,
`AdminAudioFragment` and `AdminVideoFragment` — fixing it is a cross-team task.

---

## 9. Navigation map

| Condition | Destination | Extras | `finish()`? |
|---|---|---|---|
| `token` empty / `"null"` | `LoginActivity` | — | **yes** |
| `userId` empty (inside the token branch) | `LoginActivity` | — | **yes** (but execution continues — issue S6) |
| Profile 200 + `isVerified == true` | `MainActivity` | — | **yes** |
| Profile 200 + `isVerified == false` | `ForgotPasswordActivity` | `page="verify"`, `email=<user email>` | **no** (issue S10) |
| Profile non-200 | `LoginActivity` (after a toast) | — | **yes** |
| `onFailure` / offline | **nowhere — the screen hangs** | — | — |

**Outbound contract to OTP_VERIFY:** the extras are literally `"page"` = `"verify"` and `"email"`.
This is one of the three call sites (with `LoginActivity` and `RegisterActivity`) that
`OTP_VERIFY.md` §1 documents; because the splash path arrives with `Util.userId` **set**, a successful
verification from here routes the user on to `MainActivity`.

---

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | everything (§3) |
| others | **none overridden** — no `onDestroy`, no `onPause`, no `onResume` |

- The Retrofit call is **never cancelled**, and the four LiveData observers are never removed
  (they are lifecycle-bound to `this`, so they stop at destroy, but the in-flight request is not)
  (issue S15).
- **No rotation handling.** Rotating during the 2-second sleep recreates the activity and **re-runs
  the whole bootstrap**, including a second `getMyDetails` call.
- `userPreferences` is a `private lateinit var` assigned first in `onCreate`.
- Minimum time on this screen is **~2 s**, and ~4 s for a returning user (two `Thread.sleep(2000)`
  calls: one before the request, one inside its callback) **plus** network latency.

---

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `UserPreferences` (`authToken`, `userId`, `isNightModeEnabled`, `textSize`, `saveIsFirstTime`, `saveIsNightModeEnabled`) | PLATFORM / STORAGE | the whole bootstrap — the heaviest consumer in the app |
| `Util.getRetrofit()`, `RetrofitAPI.getUser` | PLATFORM / NETWORK | the profile fetch |
| `UserRslt` | PLATFORM / DATA_MODELS | response parsing |
| `Util.NIGHT` / `Util.DAY` / `Util.DEFAULT` + the `Util` globals | PLATFORM / COMMONS | theme constants and global state |
| `Commons().isNetworkAvailable` | PLATFORM / COMMONS | connectivity guard |
| `@drawable/splash_screen` (PNG) | APPSHELL / THEMING | the logo |
| `LoginActivity` | AUTH / `LOGIN.md` | no-session destination |
| `ForgotPasswordActivity` | AUTH / `OTP_VERIFY.md` | unverified destination |
| `MainActivity` | APPSHELL / MAIN_NAV | signed-in destination |
| manifest `<intent-filter>` | PLATFORM / BUILD_CONFIG | launcher registration |

Not used here: Kotlin synthetics, `SpotsDialog`, `Util.isValid*` validators.

---

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| S5 | ~~**`onFailure` does nothing**~~ — **FIXED 2026-10-08 (T-024)**: `onFailure` now calls `bailToLogin(R.string.splash_server_unreachable)` | ~~Critical~~ | — |
| S12 | ~~Offline -> `isNetworkAvailable` false -> silent return, permanent hang at launch~~ — **FIXED 2026-10-08 (T-024)**: the `if` now has an `else { bailToLogin(R.string.splash_offline) }` | ~~Critical~~ | — |
| S9 / S16 | ~~**Two** `Thread.sleep(2000)` calls on the main thread~~ — **FIXED 2026-10-08 (T-024)**: on the v1.2.0 baseline only one remained (line 159) and it is now deleted. **0 `Thread.sleep` left in this file** | ~~High (ANR)~~ | — |
| S11 | ~~A proven-invalid session is **not cleared**~~ — **FIXED 2026-10-08 (T-024)**: `bailToLogin` calls `deleteAuthToken()` + `deleteUserId()` and clears `Util.userId` / `Util.user` / `Util.clearPermissions()` before routing | ~~High~~ | — |
| S22 | **NEW (T-025, `G13`)** — ~~`getMyPermission` was fire-and-forget **after** routing, so the destination screen read an empty `permissionMap`; combined with the fail-open `Util.hasPermission` this granted every permission~~ — **FIXED 2026-10-08**: `getMyPermission(token) { … }` now takes a completion lambda, routing happens **inside** it, and any failure calls `bailToLogin` | ~~**Critical (security)**~~ | — |
| S13 | Four **continuous** LiveData observers; any later write to `token`/`userId`/`isNight`/`textSize` re-fires them and can launch `getMyDetails` again. *(Partially mitigated by the new `bailedOut` guard, which makes the failure path idempotent; the happy path can still re-fire.)* | **High** | one-shot reads (`first()`), or guard with a `hasBootstrapped` flag |
| S6 | The empty-`userId` branch starts Login and calls `finish()` but **does not return**, so `Util.userId` is still assigned and the flow continues to `GET api/v1/users/` | **High** | add `return@observe` |
| S7 | `getMyDetails` depends on a **sibling observer** having already set `Util.userId`; the ordering is incidental, not guaranteed | Medium | read `userId` once and pass it explicitly |
| S8 | `isWarrior` treats empty/null as **true** (`isNullOrEmpty() \|\| != "false"`) | Medium | compare explicitly with `"true"` (same as LOGIN L5) |
| S10 | The unverified route omits `finish()`, leaving the splash on the back stack | Medium | `finish()` after `startActivity` |
| S15 | The Retrofit call is never cancelled; no `onDestroy` override at all | Medium | keep the `Call` and `cancel()` it |
| S4 | The API 31+ `addOnDrawListener { false }` block is a **no-op** (mis-port of `addOnPreDrawListener`); `androidx.core:core-splashscreen` is not used | Medium | adopt the official SplashScreen API, or delete the dead block |
| S1 | No splash theme / `windowBackground`, so a blank window shows before the layout inflates | Medium | add a themed `windowBackground` |
| S2 | Hard-coded `#F0F3F9` and `#FFFFFF`; the splash ignores night mode despite a `DayNight` theme | Medium | move to `colors.xml` + `values-night` |
| S14 | No progress indicator during the bootstrap wait *(the artificial 2 s delay is gone, but a slow network still shows a static logo)* | Medium | add a spinner |
| S23 | **NEW (T-024)** — `getBible()` throws `RuntimeException(e)` from inside a Retrofit callback on a malformed payload, and its `onFailure` only logs | **High** | handle the parse failure without rethrowing |
| S3 | The `ImageView` has no `scaleType` and is stretched to `match_parent` | Low | use `centerInside` / `centerCrop`, or a vector |
| S17 | ~~`Log.e("responseee", …)` / `Log.e("Splashscreen", …)` debug-grade tags~~ — **FIXED 2026-10-08 (T-024)** in `getMyDetails` / `getMyPermission` / `bailToLogin`; `getBible` and `readNotification` still use ad-hoc tags | Low | finish the sweep in the two remaining methods |
| S18 | ~~`"Somthing Went Wrong"` is misspelled~~ — **FIXED here 2026-10-08 (T-024)**: replaced by `@string/splash_server_unreachable`. Still present in 5 other files | Cosmetic | fix the remaining files app-wide (cross-team) |
| S19 | ~~The class name `SplashhScreenActivity` contains a typo (double "h")~~ — **FIXED upstream** on the v1.2.0 baseline: renamed to `SplashScreenActivity` | ~~Cosmetic~~ | — |
| S20 | `getMyDetails` is **triplicated** across Splash, Login and MainActivity, with three different error behaviours. *(T-025 removed MainActivity's broken `SplashScreenActivity().getMyPermission(it)` call, but the duplication itself remains.)* | **High** | extract a shared `SessionBootstrap` (PM-level refactor) |
| S21 | Rotating during the bootstrap re-runs it from scratch, including a duplicate network call | Medium | guard with `savedInstanceState == null` |

---

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/SplashhScreenActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_splashh_screen.xml` | **exclusive** |
| `<activity android:name=".SplashhScreenActivity">` block (incl. the `MAIN`/`LAUNCHER` intent-filter) in `AndroidManifest.xml` | shared with PLATFORM/BUILD_CONFIG — **any change here affects how the app launches**; notify lead |

**Not owned (escalate):** `UserPreferences.kt`, `RetrofitAPI.kt`, `DataModels.kt`, `Util.java`,
`Commons.kt`, `@drawable/splash_screen`, `styles.xml` / `colors.xml` (APPSHELL/THEMING),
`LoginActivity.kt`, `ForgotPasswordActivity.kt`, `MainActivity.kt`.

---

## 14. How to make common changes

**Fix the launch-time hang (S5/S12 — highest value):** in `onFailure`, toast a failure message and
start `LoginActivity` + `finish()`; add an `else` to the `isNetworkAvailable` check doing the same
with an offline message. Both are local to this agent. Update §5, §8 and §9 here.

**Remove the artificial delay (S9/S16):** delete both `Thread.sleep(2000)` calls. If the customer
wants the logo held for branding, replace the first with
`lifecycleScope.launch { delay(2000); getMyDetails(it) }` and drop the second entirely — it sits
inside a network callback and adds latency on top of latency.

**Clear a dead session (S11):** in the non-200 branch, wrap
`userPreferences.deleteAuthToken()` + `deleteUserId()` in `lifecycleScope.launch { }` before routing
to Login — mirroring what `MainActivity` already does. `UserPreferences` itself is PLATFORM/STORAGE:
do not edit it, only call it.

**Stop the double bootstrap (S13/S21):** replace the four `.asLiveData().observe(this)` calls with
one-shot reads, or guard `getMyDetails` with a `private var bootstrapped = false`. Prefer the guard
for a minimal diff; prefer one-shot reads for a real fix.

**Change the splash image or colours:** `@drawable/splash_screen` and the palette belong to
APPSHELL/THEMING — you may edit `scaleType` and layout-local attributes here, but a new asset or a
`colors.xml` entry must be requested through PM.

**Change where a returning user lands:** the destinations are owned by APPSHELL (`MainActivity`) and
AUTH siblings. The same routing block exists in `LoginActivity` and `MainActivity` — **escalate to PM**
so all three stay in sync (S20).

**Adopt the official SplashScreen API (S4):** adding `androidx.core:core-splashscreen` is a
**PLATFORM/BUILD_CONFIG** dependency change — escalate to PM; the manifest/theme work would follow here.

**Never remove the `MAIN`/`LAUNCHER` intent-filter** — it is the app's only entry point, and this
screen is where every `Util` global is seeded.

---

## 15. Change log

| Change | Detail |
|---|---|
| T-024 / T-025 (2026-10-08) | **Wave 1 + 2 fixes.** Closed P0 #1/#2 (`S5`, `S12`, `S11`) with a single `bailToLogin(messageRes)` recovery path: toast → clear `token`/`userId` → clear `Util` session + permissions → `LoginActivity` + `finish()`. Reached from `onFailure`, the offline `else`, the `catch`, HTTP 401 and any other non-200. Deleted the last main-thread `Thread.sleep(2000)` (`S9`/`S16`). Closed `G13` here (`S22`): `getMyPermission(token, onComplete)` now gates routing on the permission map being loaded, and a failed fetch bails instead of continuing with an empty map. Added 3 strings (`splash_offline`, `splash_server_unreachable`, `splash_permissions_failed`) and the `bailedOut` re-entry guard. New issues `S22` (fixed) and `S23` (`getBible` rethrows). Verified: `assembleDebug` + 11 unit tests green. |
| Created | Initial SPLASH module agent, documented line-by-line from `SplashhScreenActivity.kt` (133 lines) and `activity_splashh_screen.xml` (16 lines): the launcher contract, a 2-view id-less layout, the 4 DataStore observers, the token/userId/verified routing fork, the `GET users/{userId}` call, 5 storage keys, 6 `Util` globals seeded, 1 user-visible string, 6 navigation edges, 21 known issues. Recorded the two main-thread `Thread.sleep(2000)` calls, the **silent `onFailure` that hangs the app at launch**, the missing `return@observe` on the empty-userId path, the no-op API 31+ draw listener, and the `getMyDetails` triplication shared with LOGIN and MAIN_NAV. |
