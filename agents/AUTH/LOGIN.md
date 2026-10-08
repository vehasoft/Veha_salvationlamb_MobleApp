# LOGIN Module Agent

> Team: **AUTH** · Reports to: `agents/AUTH/AUTH_LEAD.md`
> This agent knows **every detail** of the Login screen: fields, buttons, links, API calls,
> storage writes, global state, error messages, UI and navigation.
> It may only edit the files listed in §13 **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **Login** ("Hi, Welcome Back") |
| Class | `com.veha.activity.LoginActivity` (Kotlin, `AppCompatActivity`) |
| Source | `app/src/main/java/com/veha/activity/LoginActivity.kt` (179 lines) |
| Layout | `app/src/main/res/layout/activity_login.xml` |
| Manifest entry | `<activity android:name=".LoginActivity" android:exported="false"/>` — no intent-filter, no `launchMode`, no `windowSoftInputMode` |
| View access | `kotlinx.android.synthetic.main.activity_login.*` (Kotlin synthetics, **not** ViewBinding) |
| Entered from | `SplashhScreenActivity` (no token / bad token), `ForgotPasswordActivity` (verify-mode "cancel"), `MainActivity` (logout + session failure), `HomeFragment`, `FilesFragment`, `AdminAudioFragment`, `AdminVideoFragment` (forced re-login on API failure) |
| Exits to | `RegisterActivity`, `ForgotPasswordActivity` (forgot mode **and** verify mode), `WebViewActivity` (terms / privacy), `MainActivity` (success) |

---

## 2. UI inventory

Root: `androidx.constraintlayout.widget.ConstraintLayout`, background `@color/primary_blue`,
`tools:context="com.veha.activity.LoginActivity"`.
Body: a vertical `LinearLayout` card — `@drawable/rounded_border_login_register`, `padding 20dp`,
`marginTop 56dp`, `marginLeft/Right 10dp`.

| View id | Type | Text / hint | Notes |
|---|---|---|---|
| `prod_logo` | `ImageView` | — | `200dp x 50dp`, `@drawable/ic_sl_logo_01_svg`, pinned top-left, `marginLeft 5dp` |
| `filledTextField` | `TextInputLayout` | hint **"Email"** | `textColorHint @color/black`, background `@color/home_bg` |
| `email` | `TextInputEditText` | — | `drawableEnd @drawable/profile`, `drawableTint @color/primary_blue`, text/hint black. **No `inputType`** — defaults to free text (see issue L6) |
| `password_layout` | `TextInputLayout` | hint **"Password"** | `app:passwordToggleEnabled="true"`, `passwordToggleTint @color/black`, `marginTop 10dp` |
| `password` | `TextInputEditText` | — | `inputType="textPassword"`, text black |
| `forgot_pwd` | `TextView` | **"Forgot Password?"** | `@color/primary_blue`, `15dp`, `marginBottom 10dp` |
| `login_btn` | `Button` | **"Login"** | full width, `backgroundTint @color/primary_blue`, text `@color/always_white`, rounded background |
| `signup_btn` | `Button` | **"Sign Up"** | full width, `backgroundTint @color/white`, text `@color/primary_blue` |
| `terms` | `TextView` | **"Terms and conditions"** | centered, `@color/primary_blue`, `15dp` |
| `privacy` | `TextView` | **"Privacy Policy"** | centered, `@color/primary_blue`, `15dp` |

### Static text (no id)

| Text | Style |
|---|---|
| "Login" | `28dp`, bold, `@color/black` |
| "Hi, Welcome Back" | `20dp`, bold, `@color/black` |
| "Welcome to SalvationLamb. Please login to your account" | `15dp`, `@color/hintcolor` |

### Commented-out in the layout (do not re-enable without asking the customer)

- An "About" link in the header.
- Standalone "Email Address" and "Password" labels (replaced by `TextInputLayout` hints).
- A **"Remember Me"** `CheckBox` — so there is **no remember-me feature**; the session is kept by the DataStore token instead.

### Progress indicator

`dmax.dialog.SpotsDialog` built in `onCreate`: message **"Please Wait"**, `setCancelable(false)`,
`setInverseBackgroundForced(false)`. Shown only around the `login` call (see §5).

---

## 3. Actions / event handlers

All wired in `onCreate` after `setContentView`.

| Trigger | Behaviour |
|---|---|
| `signup_btn` click | `startActivity(Intent(this, RegisterActivity::class.java))` — **no `finish()`** |
| `forgot_pwd` click | `startActivity(Intent(this, ForgotPasswordActivity::class.java))` — **no extras**, so the target runs its **forgot** branch |
| `terms` click | `WebViewActivity` with extra `WebPageName = "terms"` |
| `privacy` click | `WebViewActivity` with extra `WebPageName = "privacy"` |
| `login_btn` click | Reads `email.text` and `password.text`; builds `JsonObject { email, password, isMobile: true }`; validates email -> password; calls `login(data)` only if **both** pass |

**Order of operations in `login_btn`:** the `JsonObject` is built *before* validation (harmless, but
the body exists even when validation fails and no call is made).

---

## 4. Validation rules

Client-side only, via `com.veha.util.Util` (PLATFORM/COMMONS). Evaluated as an `if / else if / else`
chain, so **only the first failure is reported**.

| Order | Field | Rule | Regex | Failure feedback |
|---|---|---|---|---|
| 1 | `email` | `Util.isValidEmail(emailstr)` | `^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,6}$` (case-insensitive, `.find()`) | Toast **"Invalid Email"** (LENGTH_LONG) |
| 2 | `password` | `Util.isValidPassword(passwordstr)` | `^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\S+$).{8,20}$` | Toast **"Invalid Password"** (LENGTH_LONG) |
| 3 | both ok | — | — | `login(data)` is invoked |

- No trimming: leading/trailing spaces are sent as-is (and spaces fail the password regex via `(?=\S+$)`).
- No empty-field short-circuit: an empty email simply fails `isValidEmail`.
- Errors are **Toasts only** — `TextInputLayout.error` is never set on this screen (unlike Forgot Password).
- The password rule means an existing account whose password predates these rules **cannot log in** without a reset.

---

## 5. API contracts

### 5.1 Login — `login(data: JsonObject)`

| Item | Value |
|---|---|
| Retrofit method | `RetrofitAPI.postCall(url = "login", dataModal = data)` |
| HTTP | `POST api/v1/login` |
| Base URL source | `Util.getRetrofit()` -> `https://server.salvationlamb.com` (**not** `APIUtil`) |
| Headers | none (no auth) |
| Guard | wrapped in `try/catch`; runs only if `Commons().isNetworkAvailable(this)` |
| Progress | `dialog.show()` before enqueue (if not already showing) |
| Request body | `{ "email": <String>, "password": <String>, "isMobile": true }` |

**Success (`response.code() == 200`)**
1. `Gson().fromJson(resp.get("result"), Loginresp::class.java)`.
2. Sets `Util.isFirst`, `Util.isWarrior`, `Util.userId` (immediately, outside the coroutine).
3. `lifecycleScope.launch { ... }` writes DataStore: `saveAuthToken(token)`, `saveUserId(id)`,
   `saveIsNightModeEnabled(Util.DEFAULT)`, `saveIsFirstTime(isFreshUser)`; re-sets the same three
   `Util` fields; then calls `getMyDetails(loginresp.token)`.
4. A commented-out direct jump to `MainActivity` remains in the source — navigation is intentionally
   deferred to `getMyDetails`.

**Error (any non-200)**
- `Gson().fromJson(response.errorBody()?.string(), JsonObject::class.java)`.
- Reads `status` and `errorMessage` via `.get(...).toString()` (**values keep their JSON quotes**).
- `Log.e("Status", status)` and `Log.e("result", errorMessage)`.
- Message matching (`contains(..., ignoreCase = true)`):
  | Server `errorMessage` contains | Toast shown |
  |---|---|
  | `Invalid password` | **"INVALID PASSWORD"** |
  | `Invalid Email-Id` | **"INVALID USER"** |
  | anything else | **nothing is shown** (silent failure — issue L3) |
- `dialog.dismiss()` runs at the end of `onResponse` for both branches.

**`onFailure`** — `Log.e("LoginActivity.login()", "fail")` + `dialog.dismiss()`. **No user feedback.**

### 5.2 Fetch profile — `getMyDetails(token: String)`

| Item | Value |
|---|---|
| Retrofit method | `RetrofitAPI.getUser(head = "Bearer $token", userId = Util.userId)` |
| HTTP | `GET api/v1/users/{userId}` |
| Headers | `Authorization: Bearer <token>` |
| Guard | `try/catch` + `Commons().isNetworkAvailable(this)` |
| Progress | **none** — the SpotsDialog is already dismissed by the time this returns |

**Success (200)**
1. `Gson().fromJson(resp.get("result"), UserRslt::class.java)` -> `Util.user`.
2. `Util.isWarrior = loginresp.isWarrior.isNullOrEmpty() || loginresp.isWarrior != "false"`
   -> **anything that is not the literal `"false"` (including empty/null) makes the user a Warrior** (issue L5).
3. `Util.isFirst = loginresp.isFreshUser.toBoolean()`; `lifecycleScope.launch { saveIsFirstTime(...) }`.
4. **`Thread.sleep(2000)` on the main thread** (issue L1).
5. Routing on `loginresp.isVerified.toBoolean()`:
   - `true` -> `MainActivity`, then `finish()`.
   - `false` -> `ForgotPasswordActivity` with extras `page = "verify"`, `email = loginresp.email`; **no `finish()`**.

**Non-200** -> `Log.e("responseee", "fail")` only. The user stays on Login with no message and no
visible progress — the screen looks frozen.

**`onFailure`** -> `Log.e("LoginActivity.getMyDetails", "fail")`. No user feedback.

### 5.3 Models consumed (PLATFORM/DATA_MODELS — `util/DataModels.kt`)

| Model | Fields this screen actually uses |
|---|---|
| `Loginresp` | `id`, `token`, `isFreshUser`, `isWarrior` (all `String`) |
| `UserRslt` | `isWarrior`, `isFreshUser`, `isVerified`, `email` (all `String`); whole object cached in `Util.user` |

> Note the asymmetry: `Loginresp` exposes `isVerifiedUser` while `UserRslt` exposes `isVerified`.
> This screen reads verification **only** from `UserRslt`, which is why the second call is required.

---

## 6. Storage read / written

DataStore preferences file **`SalvationLamb`**, via `UserPreferences(this@LoginActivity)`
(instantiated as the first statement of `onCreate`).

| Key | Type | Operation | Value written |
|---|---|---|---|
| `token` | String | write | `Loginresp.token` |
| `userId` | String | write | `Loginresp.id` |
| `isNight` | String | write | `Util.DEFAULT` (`"Default"`) — **resets the user's theme choice on every login** (issue L4) |
| `isFirst` | Boolean | write (twice) | `Loginresp.isFreshUser.toBoolean()`, then `UserRslt.isFreshUser.toBoolean()` |

This screen **reads nothing** from DataStore — no pre-filled email, no remembered session.

---

## 7. Global state touched (`com.veha.util.Util`)

| Field | When | Value |
|---|---|---|
| `Util.userId` | login 200 (set twice) | `Loginresp.id` |
| `Util.isFirst` | login 200 (x2), profile 200 (x2) | `isFreshUser.toBoolean()` |
| `Util.isWarrior` | login 200 (x2), profile 200 | `Loginresp.isWarrior.toBoolean()`, then the `!= "false"` rule |
| `Util.user` | profile 200 | the whole `UserRslt` |

---

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| Email fails regex | `Invalid Email` | Toast LENGTH_LONG |
| Password fails regex | `Invalid Password` | Toast LENGTH_LONG |
| Server error contains "Invalid password" | `INVALID PASSWORD` | Toast LENGTH_LONG |
| Server error contains "Invalid Email-Id" | `INVALID USER` | Toast LENGTH_LONG |
| While the login call runs | `Please Wait` | SpotsDialog (non-cancelable) |
| Any other server error, network failure, offline, or profile-fetch failure | **(none)** | — silent |

All strings are **hard-coded in Kotlin/XML**; none are in `res/values/strings.xml`, so the screen is
not localisable as-is.

---

## 9. Navigation map

| Trigger | Destination | Extras | `finish()`? |
|---|---|---|---|
| `signup_btn` | `RegisterActivity` | — | no |
| `forgot_pwd` | `ForgotPasswordActivity` | — (target falls into **forgot** mode) | no |
| `terms` | `WebViewActivity` | `WebPageName="terms"` | no |
| `privacy` | `WebViewActivity` | `WebPageName="privacy"` | no |
| Login success + `isVerified == true` | `MainActivity` | — | **yes** |
| Login success + `isVerified == false` | `ForgotPasswordActivity` | `page="verify"`, `email=<user email>` | no (issue L7) |

**Back button:** not overridden. From Login, Back exits to whatever is beneath it in the task — and
because the forced-logout call sites in `MainActivity`/fragments start Login **without** `finish()`,
Back can return the user to a signed-out screen that still renders stale data.

---

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | `userPreferences = UserPreferences(this)` -> build SpotsDialog -> `setContentView(R.layout.activity_login)` -> wire the 5 click listeners. **Note:** the dialog is created *before* `setContentView`. |
| `onDestroy` | `super.onDestroy()` then `dialog.dismiss()` — unconditional, so it can throw/warn if the window is already gone. Retrofit calls are **not** cancelled, so a late callback can touch a destroyed activity (issue L2). |

No `onResume`/`onPause`/`onSaveInstanceState` overrides. No rotation handling — a rotation mid-login
recreates the activity, loses the dialog and leaks the in-flight callback.

---

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `Util.getRetrofit()`, `RetrofitAPI.postCall`, `RetrofitAPI.getUser` | PLATFORM / NETWORK | both API calls |
| `Loginresp`, `UserRslt` | PLATFORM / DATA_MODELS | response parsing |
| `UserPreferences` | PLATFORM / STORAGE | session persistence |
| `Util.isValidEmail`, `Util.isValidPassword`, `Util.DEFAULT`, `Util.*` statics | PLATFORM / COMMONS | validation + global state |
| `Commons().isNetworkAvailable` | PLATFORM / COMMONS | connectivity guard |
| `dmax.dialog.SpotsDialog` | PLATFORM / BUILD_CONFIG | progress dialog |
| `RegisterActivity`, `ForgotPasswordActivity` | AUTH (siblings) | navigation targets |
| `WebViewActivity` | MEDIA / WEBVIEW | terms & privacy |
| `MainActivity` | APPSHELL / MAIN_NAV | post-login landing |

---

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| L1 | `Thread.sleep(2000)` inside `getMyDetails` runs on the **main thread** | High (ANR) | remove it, or use `Handler.postDelayed` / `lifecycleScope.launch { delay(2000) }` |
| L2 | Retrofit calls are never cancelled in `onDestroy`; callbacks touch a dead activity | High | keep the `Call` references and `cancel()` them, or guard with `isFinishing`/`isDestroyed` |
| L3 | Unmatched server errors show **no** message; `onFailure` shows none either | High (UX) | add an `else` toast with the server text and a generic failure toast |
| L4 | Every successful login overwrites `isNight` with `Util.DEFAULT`, wiping the saved theme | Medium | only write the theme when no value exists |
| L5 | `isWarrior` logic treats empty/null as **true** (`isNullOrEmpty() \|\| != "false"`) | Medium | compare explicitly with `"true"` |
| L6 | `email` field has no `inputType="textEmailAddress"` and no `imeOptions`; no field-level `error` display | Medium | add `inputType`, IME next/done, and `TextInputLayout` errors |
| L7 | Unverified users are sent to the verify screen **without** `finish()` | Medium | `finish()` after `startActivity` |
| L8 | Offline (`isNetworkAvailable == false`) -> method returns silently, no dialog, no toast | Medium | toast "No internet connection" |
| L9 | `getMyDetails` has no progress indicator — a ~2s+ blank gap after the dialog closes | Low | keep the dialog up until routing completes |
| L10 | `Util` fields are assigned twice (before and inside the coroutine) | Low | assign once |
| L11 | All strings hard-coded; not localisable; `status`/`errorMessage` carry JSON quotes | Low | move to `strings.xml`; use `asString` instead of `toString()` |
| L12 | Password is sent in plain JSON over a base URL that is HTTPS, but the app sets `usesCleartextTraffic="true"` globally | Medium (security) | PLATFORM/BUILD_CONFIG to restrict cleartext |
| L13 | No rotation/state handling; dialog and in-flight request are lost | Low | handle config change or move state to a ViewModel |

---

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/LoginActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_login.xml` | **exclusive** |
| `<activity android:name=".LoginActivity">` block in `app/src/main/AndroidManifest.xml` | shared with PLATFORM/BUILD_CONFIG — notify lead |

**Not owned (escalate):** `RetrofitAPI.kt`, `DataModels.kt`, `UserPreferences.kt`, `Util.java`,
`Commons.kt`, `colors.xml`/`styles.xml`, `rounded_border_login_register.xml`,
`ic_sl_logo_01_svg`, and every other screen.

---

## 14. How to make common changes

**Add a field (e.g. "Remember me" or a phone login):**
1. Add the view to `activity_login.xml` with an id (synthetics pick it up automatically — no binding code).
2. Read it in the `login_btn` listener, add a validation branch *before* `login(data)`.
3. If it must be sent to the server -> new body key -> confirm with PLATFORM/NETWORK that the backend accepts it.
4. If it must be persisted -> new DataStore key -> **PLATFORM/STORAGE task via PM**.

**Change a validation message:** edit the Toast literal in the `login_btn` listener (§4) and update §4 + §8 here.

**Handle a new server error:** add a branch to the `contains(...)` chain in `login()`'s error block,
and always finish with an `else` fallback (fixes L3). Update §5.1 + §8.

**Change where a verified user lands:** that is an APPSHELL contract — escalate to PM; do not edit
`getMyDetails` routing unilaterally (the same block is duplicated in `SplashhScreenActivity` and
`MainActivity` and must stay in sync).

**Restyle:** colors/dimens live in `res/values` (APPSHELL/THEMING). Only layout-local attributes may
be changed here.

---

## 15. Change log

| Change | Detail |
|---|---|
| Created | Initial LOGIN module agent, documented line-by-line from `LoginActivity.kt` (179 lines) and `activity_login.xml` (183 lines): 10 view ids, 5 actions, 2 validations, 2 API calls, 4 DataStore writes, 4 `Util` globals, 6 user-visible strings, 6 navigation edges, 13 known issues. |
