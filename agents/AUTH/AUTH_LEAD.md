# AUTH TEAM LEAD Agent

> Reports to: **PROJECT MANAGER** (`AGENTS.md`)
> Manages: the 7 module agents below. **Status: all READY.**

---

## 1. Charter

The AUTH team owns **everything about getting a user into (and out of) the app**: account creation,
email OTP verification, password login, password recovery, password change, and the session
bootstrap that decides whether a returning user lands on the feed or back at the login screen.

The team owns the **lifecycle of the auth token and user id** in DataStore and the initial
population of the `Util` global user state (`Util.user`, `Util.userId`, `Util.isWarrior`,
`Util.isFirst`).

**Boundary:** AUTH stops the moment `MainActivity` is launched. Anything inside the app shell
(bottom nav, theme, settings) is APPSHELL; profile editing is PROFILE. AUTH does **not** own
`RetrofitAPI.kt`, `UserPreferences.kt`, `Util.java` or `DataModels.kt` — those are PLATFORM files;
AUTH only *calls* them and escalates to PM when they must change.

> **Note on logout:** the logout action itself lives in `MainActivity` (APPSHELL) and in several
> feed/media fragments that force a re-login on HTTP 401-ish failures. AUTH owns the *contract*
> ("clear `token` + `userId`, then open `LoginActivity`"); APPSHELL/FEED/MEDIA own the call sites.
> Any change to the logout contract is a PM-coordinated cross-team task.

---

## 2. Modules owned

| Module agent | Screen / unit | Source files | Status |
|---|---|---|---|
| `LOGIN.md` | Login | `activity/LoginActivity.kt`, `res/layout/activity_login.xml` | READY |
| `REGISTER.md` | Sign Up | `activity/RegisterActivity.kt`, `res/layout/activity_register.xml` | READY |
| `FORGOT_PASSWORD.md` | Forgot Password (mode `forgot`) | `activity/ForgotPasswordActivity.kt`, `res/layout/activity_forgot_password.xml` | READY |
| `OTP_VERIFY.md` | OTP Verification (mode `verify`) | same files as above, `page == "verify"` branch | READY |
| `CHANGE_PASSWORD.md` | Change Password (signed-in; no `email` extra) | `activity/ChangePasswordActivity.kt`, `res/layout/activity_change_password.xml` | READY |
| `RESET_PASSWORD.md` | Reset Password (after OTP; `email` + `otp` extras) | same files as above, `email` present branch | READY |
| `SPLASH.md` | Splash / session bootstrap | `activity/SplashhScreenActivity.kt`, `res/layout/activity_splashh_screen.xml` | READY |

> **Shared-file warning — two classes are each split across two agents:**
>
> | Class | Selector | Agent A | Agent B |
> |---|---|---|---|
> | `ForgotPasswordActivity.kt` | intent extra `page` | `FORGOT_PASSWORD.md` (`else` branch) | `OTP_VERIFY.md` (`page == "verify"`) |
> | `ChangePasswordActivity.kt` | intent extra `email` | `CHANGE_PASSWORD.md` (`email` blank) | `RESET_PASSWORD.md` (`email` present) |
>
> Both agents of a pair must be consulted before editing the shared file or layout, and whichever one
> edits it must notify the other via the lead. In `ChangePasswordActivity` the **validation chain,
> `onCreate` prologue and the three lifecycle overrides are shared**; only `changePassword()` and
> `forgotPassword()` are exclusively owned.

---

## 3. Intra-team flows

```
SplashhScreenActivity (LAUNCHER)
   |-- no token / no userId ------------------> LoginActivity
   |-- token present -> GET users/{id}
          |-- isVerified == true -------------> MainActivity            [APPSHELL]
          |-- isVerified == false ------------> ForgotPasswordActivity  (page="verify", email)
          |-- non-200 ------------------------> toast + LoginActivity

LoginActivity
   |-- "Sign Up" ------------------------------> RegisterActivity
   |-- "Forgot Password?" ---------------------> ForgotPasswordActivity (no extras -> forgot mode)
   |-- "Terms and conditions" -----------------> WebViewActivity (WebPageName="terms")   [MEDIA]
   |-- "Privacy Policy" -----------------------> WebViewActivity (WebPageName="privacy") [MEDIA]
   |-- login 200 -> GET users/{id}
          |-- isVerified == true -------------> MainActivity            [APPSHELL]
          |-- isVerified == false ------------> ForgotPasswordActivity  (page="verify", email)

RegisterActivity
   |-- "Sign in" ------------------------------> LoginActivity (no finish())
   |-- "Terms and conditions" -----------------> WebViewActivity (WebPageName="terms")   [MEDIA]
   |-- POST users 200 -> Util.user set, NO token/userId saved
          `--------------------------------> ForgotPasswordActivity (page="verify", email)

ForgotPasswordActivity (forgot mode)   <- started with NO extras, so page == "null"
   step 1: POST password/forgot-password { email }
           -> otp_op VISIBLE, forgot_btn text -> "confirm", listener re-wired
   step 2: POST password/verify-otp { email, otp }
           -> ChangePasswordActivity (extras: email, otp) + finish()
   "Cancel" -> finish()

ForgotPasswordActivity (verify mode)   <- requires extras page="verify" AND email
   on open (automatic): POST password/forgot-password { email, isVerifyMail: true }
   "Verify"/"confirm":  POST users/verify-otp { email, otp }
          |-- Util.userId empty (came from Register) --> LoginActivity + finish()
          `-- Util.userId set (came from Login/Splash) -> MainActivity  + finish()
   "Cancel" / Back -> finish() + LoginActivity

ChangePasswordActivity                 <- mode chosen by presence of the "email" extra
   signed-in mode (no extras; from Settings / About) [APPSHELL callers]
       POST users/change-password  (Bearer token) { userId, oldPassword, newPassword }
       -> toast "Password changed successfully" + finish()
   reset mode (extras email + otp; from ForgotPasswordActivity)
       POST password/confirm-password (no token) { email, otp, password }
       -> finish() with NO confirmation message

MainActivity "Logout" [APPSHELL]
   confirm dialog -> deleteAuthToken() + deleteUserId() -> LoginActivity
```

### The "get my details" duplication

The exact same `GET api/v1/users/{userId}` + `UserRslt` + verified-routing block is **copy-pasted**
in `LoginActivity.getMyDetails()`, `SplashhScreenActivity.getMyDetails()` and
`MainActivity.getMyDetails()`. Any change to that logic must be applied to **all three**, or
escalated to PM as a refactor task (candidate: a shared `SessionBootstrap` helper owned by PLATFORM).

---

## 4. Shared state touched by this team

### DataStore `SalvationLamb` (via `com.veha.util.UserPreferences`) — owned by PLATFORM/STORAGE

| Key | Type | Read by | Written by |
|---|---|---|---|
| `token` | String | SPLASH, CHANGE_PASSWORD | LOGIN (save), logout call sites (delete) |
| `userId` | String | SPLASH | LOGIN (save), logout call sites (delete) |
| `isNight` | String | SPLASH | LOGIN (writes `Util.DEFAULT` on every successful login) |
| `isFirst` | Boolean | APPSHELL/PROFILE | LOGIN, SPLASH |
| `textSize` | Float | SPLASH (into `Util.fontSize`) | SETTINGS [APPSHELL] |

### `com.veha.util.Util` statics — owned by PLATFORM/COMMONS

| Field | Set by AUTH | Meaning |
|---|---|---|
| `Util.userId` | LOGIN, SPLASH | current user id |
| `Util.user` | LOGIN, SPLASH (`UserRslt`) | full profile object used app-wide |
| `Util.isWarrior` | LOGIN, SPLASH | elevated role flag |
| `Util.isFirst` | LOGIN, SPLASH | fresh-user flag |
| `Util.isNight` | SPLASH | theme mode |
| `Util.fontSize` | SPLASH | global font scale |

---

## 5. API surface used by this team

All calls go through **`Util.getRetrofit()`** -> base URL `https://server.salvationlamb.com`.
(`APIUtil.retrofit` with the Elastic Beanstalk URL is **not** used by AUTH.)

| Endpoint | Retrofit method | Auth | Used by |
|---|---|---|---|
| `POST api/v1/login` | `postCall("login", body)` | none | LOGIN |
| `POST api/v1/users` | `postCall("users", body)` | none | REGISTER |
| `GET api/v1/users/{userId}` | `getUser(header, userId)` | `Bearer <token>` | LOGIN, SPLASH (+ MainActivity) |
| `POST api/v1/password/forgot-password` | `postForgotPassword(body)` | none | FORGOT_PASSWORD, OTP_VERIFY |
| `POST api/v1/password/verify-otp` | `postForgotPasswordOtp(body)` | none | FORGOT_PASSWORD |
| `POST api/v1/users/verify-otp` | `postVerifyUser(body)` | none | OTP_VERIFY |
| `POST api/v1/password/confirm-password` | `postChangeForgotPassword(body)` | none | RESET_PASSWORD |
| `POST api/v1/users/change-password` | `postChangePassword(header, body)` | `Bearer <token>` | CHANGE_PASSWORD |

### Response envelope

| Case | Shape |
|---|---|
| Success | `{ "result": { ... }, "message": "..." }` — parsed with `Gson().fromJson(resp.get("result"), X::class.java)` |
| Error | `{ "status": ..., "errorMessage": "..." }` read from `response.errorBody()` |

> `errorMessage` is read via `JsonObject.get(...).toString()`, so it arrives **with surrounding quotes**.
> Screens that toast it raw show the quotes. Matching is done with `contains(..., true)`.

---

## 6. Cross-team dependencies

| Needs | Owner team | Escalate to PM when |
|---|---|---|
| `RetrofitAPI` endpoint added/renamed/signature changed | PLATFORM / NETWORK | always |
| New DataStore key, or key type change | PLATFORM / STORAGE | always |
| `Loginresp` / `UserRslt` field added or retyped | PLATFORM / DATA_MODELS | always |
| Validator change (`isValidEmail`, `isValidPassword`, `isValidName`, `isValidMobile`) | PLATFORM / COMMONS | always (shared with REGISTER, EDIT_PROFILE) |
| Base URL change | PLATFORM / NETWORK | always |
| Post-login landing screen change | APPSHELL / MAIN_NAV | always |
| Terms / Privacy page content or routing | MEDIA / WEBVIEW | when the extra key or URL changes |
| Logout trigger sites | APPSHELL / FEED / MEDIA | when the logout contract changes |
| Country/State/City spinners on Register | PLATFORM / NETWORK (`getCountries`/`getState`/`getCity`) | when the endpoints change |

---

## 7. Delegation rules

| Incoming request mentions... | Assign to |
|---|---|
| sign in, email/password field, login button, invalid credentials, "remember me" | `LOGIN.md` |
| sign up, create account, registration form/fields, religion/country spinners | `REGISTER.md` |
| forgot password, reset link, "send OTP" from the forgot screen | `FORGOT_PASSWORD.md` |
| verify account, verification OTP, unverified user, resend OTP | `OTP_VERIFY.md` |
| change password while signed in, old password field, password rules dialog, Settings/About "Change Password" row | `CHANGE_PASSWORD.md` |
| set a new password after an OTP, reset link landing screen, "reset my password" | `RESET_PASSWORD.md` |
| app start, auto-login, session restore, splash duration/logo | `SPLASH.md` |
| logout | AUTH defines the contract -> **escalate to PM** for the APPSHELL call site |
| "auth is slow / ANR on login" | `LOGIN.md` + `SPLASH.md` (shared `Thread.sleep(2000)` issue) |

**Escalate to PM, do not handle locally:**
- anything requiring a new or changed endpoint, DataStore key, data-class field, or `Util` static;
- anything that changes where the user lands after login;
- a refactor touching the duplicated `getMyDetails()` in three classes;
- adding a new auth method (social login, biometrics) — needs a new module agent and likely PLATFORM work.

---

## 8. Definition of done (team-level)

- [ ] Change follows `AGENTS.md` §5 conventions (synthetics, `JsonObject`, network guard, SpotsDialog, Toast, `Log.e("<Class>.<method>", ...)`).
- [ ] `Commons().isNetworkAvailable(this)` guard retained before every call.
- [ ] Both the 200 branch **and** the error branch handled; `dialog.dismiss()` on every exit path including `onFailure`.
- [ ] If the flow writes session state, all four of `token`, `userId`, `Util.userId`, `Util.user` stay consistent.
- [ ] The module agent `.md` updated in the same change, including its Change log.
- [ ] Sibling screens affected by a shared file (`ForgotPasswordActivity`, duplicated `getMyDetails`) reviewed.
- [ ] Cross-team impact reported to PM; PM updates `agents/TASKS.md` with date & time.

---

## 9. Team-level known issues

| # | Issue | Module | Risk |
|---|---|---|---|
| A1 | `Thread.sleep(2000)` runs on the main thread inside the Retrofit callback | LOGIN, SPLASH | ANR / frozen UI |
| A2 | `onFailure` shows no user feedback (log only) in Login and Splash | LOGIN, SPLASH | user sees a dead button |
| A3 | Offline -> `isNetworkAvailable` false -> the method silently returns; the progress dialog is never even shown and nothing is said | all AUTH screens | user thinks the app is broken |
| A4 | `getMyDetails()` duplicated in 3 classes | LOGIN, SPLASH (+ APPSHELL) | fixes drift |
| A5 | Server error matching relies on English substrings (`"Invalid password"`, `"Invalid Email-Id"`); any backend wording change silently shows nothing | LOGIN | silent failure |
| A6 | `errorMessage` toasted with its JSON quotes | FORGOT_PASSWORD, OTP_VERIFY, REGISTER, CHANGE_PASSWORD | cosmetic |
| A7 | Password text is sent and also present in `Loginresp.password` from the server | LOGIN, REGISTER | security smell — raise with backend |
| A8 | Unverified user is routed to `ForgotPasswordActivity` without `finish()`, so Back returns to the previous screen | LOGIN, SPLASH | nav inconsistency |
| A9 | `isVerified` / `isWarrior` / `isFreshUser` are `String` fields parsed with `.toBoolean()`; `null`/`"1"`/`"TRUE"` behave unexpectedly | all AUTH | wrong routing |
| A10 | `SplashhScreenActivity` observes DataStore flows that re-emit, so `getMyDetails` can fire more than once | SPLASH | duplicate calls |
| A11 | `RegisterActivity` wildcard-imports **both** `activity_register.*` and `activity_edit_profile.*` synthetics; the two layouts share 14 ids | REGISTER | wrong view bound silently |
| A12 | Registration never creates a session (no token/userId), so a newly verified user is always bounced to Login | REGISTER, OTP_VERIFY | expected today — document before changing |
| A13 | `page = intent.getStringExtra("page").toString()` yields the literal string `"null"` when no extra is passed; forgot mode works only because `"null" != "verify"` | FORGOT_PASSWORD, OTP_VERIFY | a "null-safety cleanup" silently flips the screen into the wrong mode |
| A14 | `ForgotPasswordActivity` is the only AUTH screen with **no email format validation** (`Util.isValidEmail` unused) | FORGOT_PASSWORD | inconsistent UX vs Login/Register |
| A15 | `Log.e("data", data.toString())` logs the email **and OTP** in clear text | FORGOT_PASSWORD, OTP_VERIFY | privacy leak in release logcat |
| A16 | Verify mode fires a verification email on **every** entry and every rotation (`checkValid` is called from `onCreate`), with no cooldown | OTP_VERIFY | mail flooding / rate limiting |
| A17 | Post-verification routing reads the ambient `Util.userId` instead of an explicit extra, so the destination silently depends on which screen opened it | OTP_VERIFY | wrong landing screen if a caller changes |
| A18 | The magic strings `"verify"`, `"page"` and `"email"` are hand-typed in 4 files (Register, Login, Splash, ForgotPasswordActivity) | OTP_VERIFY, REGISTER, LOGIN, SPLASH | a typo silently renders the wrong mode |
| A19 | `ChangePasswordActivity`'s token guard is an always-true `||` chain, so its session-lost branch is **dead code** and a missing token is sent as `Bearer null` | CHANGE_PASSWORD | silent auth failure |
| A20 | A new `authToken` LiveData observer is registered on **every** button tap and never removed | CHANGE_PASSWORD | duplicate requests |
| A21 | Both error branches in Change/Reset Password have their `errorMessage` parsing **commented out**, so a wrong old password / expired OTP both show "Something went wrong" | CHANGE_PASSWORD, RESET_PASSWORD | undiagnosable failures |
| A22 | Intent extras are read with `.toString()` in `ForgotPasswordActivity` (yielding `"null"`) but **without** it in `ChangePasswordActivity` (yielding a real `null`) — two opposite conventions inside one flow | FORGOT_PASSWORD, OTP_VERIFY, CHANGE_PASSWORD, RESET_PASSWORD | confusing / fragile |
| A23 | A successful password **reset** shows no confirmation at all — the screen just closes | RESET_PASSWORD | user cannot tell if it worked |
| A24 | The "new password is same as old password" check is **inert** during a reset (`old_pwd` is GONE, so the value is `""`), so a user can reset to their current password | RESET_PASSWORD | no reuse protection |
| A25 | `ChangePasswordActivity` shares its validation chain, `onCreate` prologue and all three lifecycle overrides between two agents; only `changePassword()` / `forgotPassword()` are exclusive | CHANGE_PASSWORD, RESET_PASSWORD | cross-agent regressions |
| A26 | **`SplashhScreenActivity.onFailure` does nothing** — no toast, no navigation. A network failure or an offline launch leaves the user stuck on the splash logo forever | SPLASH | **app unusable at launch** |
| A27 | Splash does **not** clear a proven-invalid session (no `deleteAuthToken`/`deleteUserId`), unlike every other 401-ish handler, so the failing round-trip repeats on every cold start | SPLASH | permanent broken state |
| A28 | Splash runs **two** `Thread.sleep(2000)` calls on the main thread (before the request and inside its callback) — ≥ 4 s of frozen UI for a returning user, on top of A1 | SPLASH, LOGIN | ANR at launch |
| A29 | The empty-`userId` branch in Splash calls `finish()` without `return@observe`, so execution continues and issues `GET api/v1/users/` with no id | SPLASH | malformed request |
| A30 | Splash is the **only** launcher entry and the sole seeder of `Util.isNight`, `Util.fontSize`, `Util.userId`, `Util.user`, `Util.isWarrior`, `Util.isFirst` — bypassing it leaves global state unset | SPLASH | must never lose the intent-filter |

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial AUTH team lead agent: charter, 6 modules, flows, shared state, API surface, delegation rules, 10 known issues. |
| LOGIN ready | `LOGIN.md` written and verified against `LoginActivity.kt` + `activity_login.xml`. |
| REGISTER ready | `REGISTER.md` written and verified against `RegisterActivity.kt` + `activity_register.xml`. Added team issues A11 (synthetic id clash with Edit Profile) and A12 (sign-up creates no session). |
| FORGOT_PASSWORD ready | `FORGOT_PASSWORD.md` written and verified against the forgot branch of `ForgotPasswordActivity.kt` + `activity_forgot_password.xml`. Expanded the flow diagram with the two-step email/OTP sequence and the `email`+`otp` handoff to Change Password. Added team issues A13 (`page == "null"` mode selection), A14 (no email validation) and A15 (OTP logged in clear text). |
| OTP_VERIFY ready | `OTP_VERIFY.md` written and verified against the `page == "verify"` branch of the same class. Flow diagram now shows the automatic `isVerifyMail` send on open and the `Util.userId` routing fork. Added team issues A16 (OTP email on every entry/rotation), A17 (ambient `Util.userId` routing) and A18 (hand-typed `"verify"`/`"page"`/`"email"` magic strings across 4 files). AUTH is now 4 of 6 modules READY. |
| CHANGE_PASSWORD ready | `CHANGE_PASSWORD.md` written and verified against `ChangePasswordActivity.kt` + `activity_change_password.xml`. Added team issues A19 (always-true token guard / `Bearer null`), A20 (observer registered per tap), A21 (error parsing commented out) and A22 (opposite extra-reading conventions). |
| Split into CHANGE_PASSWORD + RESET_PASSWORD | On customer instruction, `ChangePasswordActivity`'s two modes were split into **two agents**, mirroring the FORGOT_PASSWORD / OTP_VERIFY pattern. `CHANGE_PASSWORD.md` now covers the signed-in mode only (`email` extra absent, `old_pwd_op` VISIBLE, `users/change-password`); the new `RESET_PASSWORD.md` covers the reset-after-OTP mode (`email` + `otp` extras, `old_pwd_op` GONE, `password/confirm-password`). Both carry a shared-file warning naming the other. Team table, §2 warning block, §5 API surface, §7 delegation rules and §9 issues updated; added A23 (silent reset success), A24 (inert reuse check during reset) and A25 (shared chain/prologue/lifecycle). AUTH is now **6 of 7 modules READY** — only `SPLASH.md` remains. |
| SPLASH ready — **AUTH team complete** | `SPLASH.md` written and verified against `SplashhScreenActivity.kt` (133 lines) + `activity_splashh_screen.xml` (16 lines): the launcher contract, 4 DataStore observers, the token/userId/verified routing fork, the triplicated `getMyDetails`, 5 storage keys, 6 `Util` globals seeded, 21 known issues. Added team issues A26 (**silent `onFailure` hangs the app at launch**), A27 (invalid session never cleared), A28 (two main-thread sleeps ≥ 4 s), A29 (missing `return@observe`) and A30 (sole launcher + sole global-state seeder). **All 7 AUTH module agents are READY; the team is complete.** |
