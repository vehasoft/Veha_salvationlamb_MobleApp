# OTP_VERIFY Module Agent

> Team: **AUTH** · Reports to: `agents/AUTH/AUTH_LEAD.md`
> This agent knows **every detail** of the OTP Verification screen: the auto-send on open, the
> pre-filled hidden email, the OTP field, buttons, API calls, the `Util.userId` routing fork,
> error messages, UI and navigation.
> It may only edit the files listed in §13 **Owned files**.

---

## ⚠ Shared-file warning (read before editing)

`ForgotPasswordActivity.kt` and `activity_forgot_password.xml` are **one class and one layout serving
two different screens**, selected by the intent extra `page`:

| `page` value | Mode | Owning agent |
|---|---|---|
| **`"verify"`** | OTP Verification | **this agent** |
| anything else (incl. the literal `"null"`) | Forgot Password | `FORGOT_PASSWORD.md` |

**This agent owns the `page.contentEquals("verify")` branch only.** The `onCreate` prologue, the
bodies of `checkValid`/`checkOtp`, `onDestroy`, `onBackPressed` and the whole layout are **shared** —
every edit to them must be reviewed with `FORGOT_PASSWORD.md` through the lead before it ships.

> Mode detection is `page = intent.getStringExtra("page").toString()` followed by
> `page.contentEquals("verify")`. The caller **must** pass the exact lower-case string `"verify"`;
> any typo silently drops the user into Forgot Password mode with an empty email field (issue V1).

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **OTP Verification** |
| Class | `com.veha.activity.ForgotPasswordActivity` (Kotlin, `AppCompatActivity`) — **verify mode** |
| Source | `app/src/main/java/com/veha/activity/ForgotPasswordActivity.kt` (219 lines) |
| Layout | `app/src/main/res/layout/activity_forgot_password.xml` (119 lines) |
| Manifest entry | `<activity android:name=".ForgotPasswordActivity" android:exported="false"/>` |
| View access | `kotlinx.android.synthetic.main.activity_forgot_password.*` |
| Required extras | **`page = "verify"`** and **`email = <user email>`** — both mandatory |
| Entered from | `RegisterActivity` (new account), `LoginActivity` (`isVerified == false`), `SplashhScreenActivity` (`isVerified == false`) |
| Exits to | `MainActivity` (verified **and** `Util.userId` set), `LoginActivity` (verified but `Util.userId` empty, "Cancel", or Back) |
| Progress indicator | `dmax.dialog.SpotsDialog`, message **"Please Wait"**, `setCancelable(false)` |

### The three callers and what they pass

| Caller | Extras | `Util.userId` at that moment | Result after a correct OTP |
|---|---|---|---|
| `RegisterActivity` (200 on `POST users`) | `page="verify"`, `email=<typed email>` | **unset** (registration never creates a session) | -> `LoginActivity` |
| `LoginActivity` (`getMyDetails`, `isVerified == false`) | `page="verify"`, `email=UserRslt.email` | set by `login()` | -> `MainActivity` |
| `SplashhScreenActivity` (`getMyDetails`, `isVerified == false`) | `page="verify"`, `email=UserRslt.email` | set from DataStore | -> `MainActivity` |

This table **is** the contract behind the `Util.userId` fork in §5.2 — see issue V2.

---

## 2. UI inventory

Same layout as Forgot Password, but `onCreate` immediately reshapes it. Root `ConstraintLayout`,
background `@color/primary_blue`; body is a vertical `LinearLayout` card. **No `ScrollView`**
(issue V3).

| View id | Type | Text in verify mode | Visibility | Set by |
|---|---|---|---|---|
| *(no id)* | `TextView` | `@string/Product_name` = **"SalvationLamb"** | visible | XML |
| *(no id)* | `TextView` | **"Hi"** | visible | XML |
| `forgot_pwd_head` | `TextView` | **"OTP Verification"** (overrides the XML's "Forgot Password") | visible | **code** |
| `forgot_pwd_content` | `TextView` | **"Welcome to SalvationLamb. Please enter otp to verify your registered email id"** | visible | **code** |
| `email_op` | `TextInputLayout` | hint "Email-Id" | **`GONE`** | **code** |
| `email` | `TextInputEditText` | pre-filled with the `email` extra via `Editable.Factory.getInstance().newEditable(emailID)` | hidden (inside `email_op`) | **code** |
| `otp_op` | `TextInputLayout` | hint **"OTP"** | **`VISIBLE`** from the start (XML default is `gone`) | **code** |
| `otp` | `TextInputEditText` | — | visible | XML (`singleLine="true"`; **no `inputType="number"`, no `maxLength`** — issue V4) |
| `forgot_btn` | `Button` | XML text **"Verify"**; silently relabelled to **"confirm"** when the auto-send succeeds | visible | XML, then code |
| `cancel_btn` | `Button` | **"Cancel"** | `VISIBLE` (redundantly re-set in code — it is already visible in XML) | XML + code |

### Single visual state

Unlike Forgot Password, this mode has **no two-step reveal** — the OTP field is visible on arrival
because the OTP is dispatched automatically (§5.1). The email field is hidden but **still populated**,
which matters because the shared success handler re-reads `email.text` (see issue V5).

---

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| `onCreate` (verify mode) | Reads `emailID` from the `email` extra -> sets the two texts -> writes `emailID` into the hidden `email` field -> `otp_op` VISIBLE, `cancel_btn` VISIBLE, `email_op` GONE -> **calls `checkValid(emailID)` immediately** -> wires `forgot_btn` and `cancel_btn` |
| `forgot_btn` click | Blank OTP -> `otp.error = "Enter OTP"` + Toast **"Enter OTP"**. Otherwise `Log.e("email", emailID)` then `checkOtp(emailID, otp.text.toString())` |
| `cancel_btn` click | `finish()` **then** `startActivity(LoginActivity)` — always returns to Login, never to the caller |
| Back press | `super.onBackPressed()` -> `finish()` -> `startActivity(LoginActivity)` — identical to Cancel. A user sent here from Login/Splash cannot reach the app without verifying (intentional), and one sent from Register lands on Login (correct) |

### The silent listener swap

`checkValid`'s 200 handler is **shared with forgot mode** and unconditionally runs:

```kotlin
otp_op.visibility = View.VISIBLE       // already visible here - no-op
forgot_btn.text = "confirm"            // relabels "Verify" -> "confirm"
if (forgot_btn.text != "verify") {     // always true (dead check)
    forgot_btn.setOnClickListener { ... checkOtp(email.text.toString(), otp.text.toString()) }
}
```

So moments after this screen opens, the button the user sees says **"confirm"**, and the
`onCreate` listener (which passes `emailID`) is **replaced** by the shared one (which passes
`email.text.toString()`). Both happen to carry the same address only because `onCreate` pre-filled the
hidden `email` field — remove that line and verification breaks (issue V5).

---

## 4. Validation rules

| Field | Rule | Field `.error` | Toast |
|---|---|---|---|
| `otp` | `TextUtils.isEmpty(otp.text!!.trim())` | `Enter OTP` | `Enter OTP` |

- The email is **never validated** here — it arrives as an extra and is trusted.
- No length, digit or expiry check on the OTP.
- Emptiness is tested on the trimmed value, but the **untrimmed** `otp.text.toString()` is sent
  (issue V6).
- If the `email` extra is missing, `emailID` becomes the literal string **`"null"`** (same
  `.toString()` pattern as `page`) and that is what gets sent to the server (issue V7).

---

## 5. API contracts

Both calls go through `Util.getRetrofit()` -> `https://server.salvationlamb.com`, both are
unauthenticated, both are wrapped in `try/catch` + `Commons().isNetworkAvailable(this)`, and both show
the SpotsDialog.

### 5.1 On open — send the verification OTP: `checkValid(emailID)`

| Item | Value |
|---|---|
| Retrofit method | `RetrofitAPI.postForgotPassword(data)` |
| HTTP | `POST api/v1/password/forgot-password` |
| Headers | none |
| Request body | `{ "email": <emailID>, "isVerifyMail": true }` — **the `isVerifyMail` flag is what distinguishes this from a password reset**; it is added only when `page == "verify"` |
| Trigger | **automatic**, from `onCreate` — the user never asks for it |

**Success (200)** — toasts the server `message` (with JSON quotes), re-reveals `otp_op` (no-op here),
relabels the button to "confirm", and swaps the listener (§3).

**Error (non-200)** — reads `errorMessage` from `errorBody()`, sets `email.error = errorMessage` **and**
toasts it. Because `email_op` is **`GONE`** in this mode, the field-level error is **invisible**; only
the Toast is seen (issue V8).

**`onFailure`** — `dialog.dismiss()` + `Log.e("ForgotPasswordActivity.checkValid", "fail")`, no user feedback.

> **Every entry to this screen sends an email.** Arriving from Register, Login or Splash — and every
> device rotation — re-runs `onCreate` and fires this call again (issue V9).

### 5.2 Submit the OTP: `checkOtp(emailtxt, otpTxt)`

| Item | Value |
|---|---|
| Retrofit method | **`RetrofitAPI.postVerifyUser(data)`** (the `if (page.contentEquals("verify"))` branch) |
| HTTP | **`POST api/v1/users/verify-otp`** — note: *users*, not *password* |
| Headers | none |
| Request body | `{ "email": <String>, "otp": <String> }` |
| Debug log | `Log.e("data", data.toString())` — **logs the email and OTP in clear text** (issue V10), plus `Log.e("email", emailID)` in the click listener |

> Sibling-endpoint trap: forgot mode sends the same body to **`password/verify-otp`**
> (`postForgotPasswordOtp`). Swapping the two breaks both flows.

**Success (200)**
1. Toasts the server `message` (quoted).
2. **Routing fork on `Util.userId`:**
   | Condition | Destination | `finish()`? |
   |---|---|---|
   | `Util.userId.isNullOrEmpty()` | `LoginActivity` | yes |
   | otherwise | `MainActivity` | yes |
3. **No token is ever saved by this screen**, and `Util.user` is not refreshed — the account is now
   verified on the server, but the local `UserRslt.isVerified` copy is still `"false"` (issue V11).

**Error (non-200)** — `otp.error = errorMessage` + Toast (both visible; `otp_op` is shown). `status` is
not read or logged.

**`onFailure`** — `dialog.dismiss()` + `Log.e("ForgotPasswordActivity.checkOTP", "fail")`, no user feedback.

### 5.3 Models

**None.** Like forgot mode, this screen only reads `message` / `errorMessage` off the raw `JsonObject`.

---

## 6. Storage read / written

| Key | Operation |
|---|---|
| — | **none** |

`UserPreferences` is not imported by this class. Crucially, when a user arrives from **Register** the
token does not exist yet, which is exactly why the `Util.userId` fork sends them to Login to sign in
properly (§5.2).

---

## 7. Global state touched (`com.veha.util.Util`)

| Field | Operation | Notes |
|---|---|---|
| `Util.userId` | **read** | the sole input to the post-verification routing fork |
| — | | this screen **writes nothing** to global state |

Read-only use: `Util.getRetrofit()`.

---

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| OTP blank | `Enter OTP` | Toast LENGTH_LONG + `otp.error` |
| Auto-send succeeds (on open) | the server's `message`, **with JSON quotes** | Toast LENGTH_LONG |
| Auto-send fails | the server's `errorMessage`, **with quotes** | Toast (+ an **invisible** `email.error`) |
| OTP accepted | the server's `message`, **with quotes** | Toast LENGTH_LONG |
| OTP rejected / expired | the server's `errorMessage`, **with quotes** | Toast + `otp.error` |
| Either call in flight | `Please Wait` | SpotsDialog (non-cancelable) |
| Network failure or offline | **(none)** | silent (issues V12, V13) |

Only the header uses a string resource (`@string/Product_name`); everything else is hard-coded or
server-supplied.

---

## 9. Navigation map

| Trigger | Destination | Extras | `finish()`? |
|---|---|---|---|
| OTP accepted **and** `Util.userId` non-empty | `MainActivity` | — | **yes** |
| OTP accepted **and** `Util.userId` empty | `LoginActivity` | — | **yes** |
| `cancel_btn` | `LoginActivity` | — | **yes** (`finish()` is called *before* `startActivity`) |
| Back press | `LoginActivity` | — | **yes** |
| Either call errors | stays on this screen | — | — |

**Entry contract (must hold, or the screen misbehaves):**
- `page` **exactly** `"verify"` — otherwise forgot mode renders with an empty email field.
- `email` present — otherwise `emailID` becomes `"null"` and the OTP email goes nowhere.

**Note:** `LoginActivity` and `SplashhScreenActivity` start this screen **without** `finish()`, so the
back stack can still hold them; this screen always `finish()`es itself and pushes a fresh Login.

---

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | `setContentView` -> SpotsDialog -> read `page` -> **verify branch**: read `emailID`, set texts, pre-fill + hide `email`, show `otp_op`, **fire `checkValid(emailID)`**, wire both buttons |
| `onDestroy` | `super.onDestroy()` then unconditional `dialog.dismiss()`. Retrofit calls are **not** cancelled (issue V14) |
| `onBackPressed` | `super.onBackPressed()` -> `finish()` -> `startActivity(LoginActivity)` — **shared with forgot mode** |

`emailID` is a `lateinit var` assigned **only** in this branch (forgot mode leaves it uninitialised —
the mirror of issue F15 in `FORGOT_PASSWORD.md`).

**No rotation handling.** A rotation recreates the activity, re-runs the verify branch and therefore
**sends a second OTP email**; the typed OTP survives in the `EditText` but may now be stale if the
server invalidates the previous code (issue V9).

---

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `Util.getRetrofit()`, `RetrofitAPI.postForgotPassword`, `RetrofitAPI.postVerifyUser` | PLATFORM / NETWORK | both calls |
| `Util.userId` | PLATFORM / COMMONS | the post-verification routing fork |
| `Commons().isNetworkAvailable` | PLATFORM / COMMONS | connectivity guard |
| `dmax.dialog.SpotsDialog` | PLATFORM / BUILD_CONFIG | progress dialog |
| `@string/Product_name` | APPSHELL / THEMING | header text |
| `LoginActivity` | AUTH / `LOGIN.md` | caller **and** exit target |
| `RegisterActivity` | AUTH / `REGISTER.md` | caller |
| `SplashhScreenActivity` | AUTH / `SPLASH.md` | caller |
| `MainActivity` | APPSHELL / MAIN_NAV | exit target for an already-signed-in user |
| `FORGOT_PASSWORD.md` | AUTH (same class!) | shares this file — coordinate every edit |

Not used here: `UserPreferences`, `DataModels`, `Util.isValidEmail`, `lifecycleScope`.

---

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| V1 | Mode hinges on the magic string `"verify"` spelled identically in 4 files; a typo silently renders Forgot Password with an empty email | High | a shared constant (PLATFORM/COMMONS) used by all callers |
| V2 | Post-verification routing depends on the ambient `Util.userId` rather than on an explicit extra, so the destination changes with the caller | High | pass the intent (`"from"`) explicitly, or re-login deliberately |
| V3 | No `ScrollView` and no `windowSoftInputMode`; with the keyboard open the buttons can be unreachable | Medium | wrap in `ScrollView` / set `adjustResize` |
| V4 | `otp` has no `inputType="number"`, no `maxLength`, no autofill hint (`smsOTPCode`) | Medium | add numeric input + length cap |
| V5 | After the auto-send, the shared listener sends `email.text` instead of `emailID`; it only works because `onCreate` pre-fills the hidden field | High | always pass `emailID` in verify mode |
| V6 | Emptiness is checked on the trimmed OTP but the untrimmed value is sent | Low | send `.trim()` |
| V7 | A missing `email` extra yields the literal `"null"`, which is sent to the server | Medium | validate the extra and bail out with a message |
| V8 | On auto-send failure the error is written to `email.error`, but `email_op` is `GONE` — **the field error is invisible** | Medium | set the error on `otp` (or a shared banner) in verify mode |
| V9 | **Every entry — including every rotation — fires a new OTP email**; no cooldown, no rate-limit guard | High | send only on first create (`savedInstanceState == null`) and add a resend button with a timer |
| V10 | `Log.e("data", ...)` and `Log.e("email", emailID)` print the email and OTP in clear text | Medium (privacy) | remove the logs |
| V11 | After success the local session is not refreshed (`Util.user.isVerified` stays `"false"`, no token saved) | Medium | re-fetch the user, or rely on the Login round-trip |
| V12 | `onFailure` shows nothing on either call | High (UX) | toast a generic failure message |
| V13 | Offline -> the method returns silently; no dialog, no toast, and the user never learns no OTP was sent | High | toast "No internet connection" |
| V14 | Retrofit calls are never cancelled in `onDestroy` | High | keep the `Call` and `cancel()` it |
| V15 | The button silently relabels from "Verify" to lower-case **"confirm"** right after the screen opens, which reads as a glitch | Medium | keep the label stable in verify mode |
| V16 | `cancel_btn.visibility = View.VISIBLE` is redundant (already visible in XML) | Cosmetic | delete |
| V17 | No "resend OTP" affordance and no countdown, even though the auto-send is invisible to the user | Medium (product) | add a timed resend (coordinate with FORGOT_PASSWORD — shared layout) |
| V18 | `message` / `errorMessage` are toasted with their JSON quotes | Low | use `asString` |
| V19 | `status` is never read or logged in either method | Low | log it |
| V20 | An unverified user reaching this screen from Login/Splash can leave only via Login — correct, but there is no explanation shown to them | Low | add copy clarifying why they are here |

---

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/ForgotPasswordActivity.kt` | **shared with `FORGOT_PASSWORD.md`** — this agent owns the `page.contentEquals("verify")` branch, the `isVerifyMail` addition in `checkValid`, and the `postVerifyUser` path of `checkOtp` |
| `app/src/main/res/layout/activity_forgot_password.xml` | **shared with `FORGOT_PASSWORD.md`** — both modes render the same views |
| `<activity android:name=".ForgotPasswordActivity">` block in `AndroidManifest.xml` | shared with PLATFORM/BUILD_CONFIG — notify lead |

**Not owned (escalate):** `RetrofitAPI.kt`, `Util.java`, `Commons.kt`, `strings.xml`,
`LoginActivity.kt`, `RegisterActivity.kt`, `SplashhScreenActivity.kt`, `MainActivity.kt`.

---

## 14. How to make common changes

**Stop the duplicate OTP email on rotation (fixes V9):** guard the auto-send with
`if (savedInstanceState == null) checkValid(emailID)`. Forgot-mode impact: none — that branch never
auto-sends. Update §3, §5.1 and §10 here.

**Add a "Resend OTP" button with a cooldown (V17):** add a `TextView`/`Button` to the **shared**
layout with a new id, make it visible only in this branch, and have it call `checkValid(emailID)`
again behind a timer. Because the layout is shared you **must** notify `FORGOT_PASSWORD.md` via the
lead — forgot mode will inflate your new view too and needs its own visibility rule.

**Make the auto-send error visible (fixes V8):** in `checkValid`'s error branch, set the error on
`otp` (or a dedicated message view) when `page == "verify"`, keeping `email.error` for forgot mode.
This edits a **shared** method — coordinate.

**Harden the OTP field (V4):** add `android:inputType="number"`, `android:maxLength="6"` and
`android:autofillHints="smsOTPCode"` to `@+id/otp`. Shared layout — confirm the forgot flow uses the
same OTP format first.

**Change where a verified user lands (V2):** the fork reads `Util.userId`. Changing it affects
Register, Login and Splash simultaneously — **escalate to PM**; this is a multi-agent change and
`MainActivity` (APPSHELL) is the landing contract.

**Change the verification endpoint or body:** `postVerifyUser` / `postForgotPassword` live in
`RetrofitAPI.kt` — **PLATFORM / NETWORK task via PM**. Do not drop `isVerifyMail`, and do not swap
`users/verify-otp` for `password/verify-otp` (that is the forgot flow's endpoint).

---

## 15. Change log

| Change | Detail |
|---|---|
| Created | Initial OTP_VERIFY module agent, documented line-by-line from the `page == "verify"` branch of `ForgotPasswordActivity.kt` (219 lines) and `activity_forgot_password.xml` (119 lines): 10 views with their code-driven overrides, 4 actions, 1 validation, 2 API calls (auto-send on open + `users/verify-otp`), zero storage writes, 7 user-visible strings, 5 navigation edges, 20 known issues. Recorded the three callers and the `Util.userId` routing fork, the `isVerifyMail` flag, the silent listener swap that switches `emailID` for `email.text`, and the invisible `email.error` caused by `email_op` being `GONE`. |
