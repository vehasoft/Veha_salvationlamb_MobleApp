# FORGOT_PASSWORD Module Agent

> Team: **AUTH** · Reports to: `agents/AUTH/AUTH_LEAD.md`
> This agent knows **every detail** of the Forgot Password screen: the two-step email -> OTP flow,
> fields, buttons, dynamic text, API calls, error messages, UI and navigation.
> It may only edit the files listed in §13 **Owned files**.

---

## ⚠ Shared-file warning (read before editing)

`ForgotPasswordActivity.kt` and `activity_forgot_password.xml` are **one class and one layout serving
two different screens**, selected by the intent extra `page`:

| `page` value | Mode | Owning agent |
|---|---|---|
| `"verify"` | OTP Verification | `OTP_VERIFY.md` |
| anything else (incl. **`"null"`**) | Forgot Password | **this agent** |

**This agent owns the `else` branch only.** Any edit to the shared parts (`onCreate` prologue, the
`checkValid`/`checkOtp` bodies, `onDestroy`, `onBackPressed`, the layout) must be reviewed with
`OTP_VERIFY.md` through the lead before it ships.

> **How `page` is read:** `page = intent.getStringExtra("page").toString()`. When Login opens this
> screen it passes **no extras**, so `getStringExtra` returns `null` and `.toString()` produces the
> **literal string `"null"`** — not a null reference. The forgot flow therefore works "by accident":
> `"null".contentEquals("verify")` is false, so the `else` branch runs. Never change this to a
> null-check without updating both agents (issue F1).

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **Forgot Password** |
| Class | `com.veha.activity.ForgotPasswordActivity` (Kotlin, `AppCompatActivity`) — **forgot mode** |
| Source | `app/src/main/java/com/veha/activity/ForgotPasswordActivity.kt` (219 lines) |
| Layout | `app/src/main/res/layout/activity_forgot_password.xml` (119 lines) |
| Manifest entry | `<activity android:name=".ForgotPasswordActivity" android:exported="false"/>` — no intent-filter, no `windowSoftInputMode` |
| View access | `kotlinx.android.synthetic.main.activity_forgot_password.*` (single wildcard import — no id clash here, unlike REGISTER) |
| Entered from | `LoginActivity` — "Forgot Password?" link, **with no extras** (the only entry point for this mode) |
| Exits to | `ChangePasswordActivity` (OTP accepted, carrying `email` + `otp`), `LoginActivity` (Back) |
| Progress indicator | `dmax.dialog.SpotsDialog`, message **"Please Wait"**, `setCancelable(false)` |

---

## 2. UI inventory

Root: `ConstraintLayout`, background `@color/primary_blue`, `tools:context=".ForgotPasswordActivity"`.
Body: a vertical `LinearLayout` card — `@drawable/rounded_border_login_register`, `padding 20dp`,
`marginTop 80dp`. **No `ScrollView`** (unlike Register), so the card can be clipped on short screens
once the keyboard opens (issue F2).

| View id | Type | Text / hint (forgot mode) | Visibility at start | Notes |
|---|---|---|---|---|
| *(no id)* | `TextView` | `@string/Product_name` = **"SalvationLamb"** | visible | header, white, bold, `20dp`, `padding 15dp`. The **only string resource used anywhere in AUTH** |
| *(no id)* | `TextView` | **"Hi"** | visible | bold, `20dp`, `@color/black` |
| `forgot_pwd_head` | `TextView` | **"Forgot Password"** | visible | `28dp`, bold. XML default already matches forgot mode, but the code re-assigns it anyway |
| `forgot_pwd_content` | `TextView` | **"Welcome to SalvationLamb. Please enter your email-id."** | visible | `15dp`. Also re-assigned in code with the same text |
| `email_op` | `TextInputLayout` | hint **"Email-Id"** | **visible** | background `@color/home_bg` |
| `email` | `TextInputEditText` | — | visible | **no `inputType`** -> plain-text keyboard (issue F3) |
| `otp_op` | `TextInputLayout` | hint **"OTP"** | **`android:visibility="gone"`** | revealed only after the email step succeeds |
| `otp` | `TextInputEditText` | — | gone | `singleLine="true"`; **no `inputType="number"`**, no `maxLength` (issue F4) |
| `forgot_btn` | `Button` | XML text **"Verify"**, changed to **"confirm"** after step 1 | visible | primary blue, white text |
| `cancel_btn` | `Button` | **"Cancel"** | visible | white background, blue text |

### Two visual states

| | Step 1 — enter email | Step 2 — enter OTP |
|---|---|---|
| `email_op` | visible, editable | **still visible and still editable** (issue F5) |
| `otp_op` | gone | visible |
| `forgot_btn` text | "Verify" | "confirm" (lower-case **c** — inconsistent with every other button, issue F6) |
| `forgot_btn` action | `checkValid(email)` | `checkOtp(email, otp)` |

The transition happens **inside the success callback** of step 1, not as a separate screen.

---

## 3. Actions / event handlers

Wired in the `else` branch of `onCreate` (the forgot branch).

| Trigger | Behaviour |
|---|---|
| `onCreate` (forgot mode) | Sets `forgot_pwd_head.text = "Forgot Password"` and `forgot_pwd_content.text = "Welcome to SalvationLamb. Please enter your email-id."`; wires `forgot_btn` and `cancel_btn`. **No API call is made on open** (verify mode does make one) |
| `forgot_btn` click (step 1) | If `email.text` is blank -> `email.error = "Enter Email"` + Toast **"Enter Email"**. Otherwise `checkValid(email.text.toString())` |
| `forgot_btn` click (step 2, re-wired) | If `otp.text` is blank -> `otp.error = "Enter OTP"` + Toast **"Enter OTP"**. Otherwise `checkOtp(email.text.toString(), otp.text.toString())` |
| `cancel_btn` click | `finish()` only — returns to Login |
| Back press | `super.onBackPressed()`, then `finish()` **and** `startActivity(LoginActivity)` — a **new** Login instance is pushed even though Login is already underneath (issue F7) |

### The listener-swap detail

Inside `checkValid`'s 200 handler the code runs:

```kotlin
otp_op.visibility = View.VISIBLE
forgot_btn.text = "confirm"
if (forgot_btn.text != "verify") {        // always true - text was just set to "confirm"
    forgot_btn.setOnClickListener { ...checkOtp... }
}
```

The `if` is **dead logic** — it can never be false, and the comparison is case-sensitive against
`"verify"` while the XML label is `"Verify"` (issue F8). The listener is replaced every time step 1
succeeds, so tapping "Verify" twice simply re-registers the same handler.

---

## 4. Validation rules

| Step | Field | Rule | Field `.error` | Toast |
|---|---|---|---|---|
| 1 | `email` | `TextUtils.isEmpty(email.text!!.trim())` | `Enter Email` | `Enter Email` |
| 2 | `otp` | `TextUtils.isEmpty(otp.text!!.trim())` | `Enter OTP` | `Enter OTP` |

**There is no format validation at all on this screen.**

- `Util.isValidEmail(...)` is **not** called here (Login and Register both call it), so `abc` is sent
  to the server and the backend has to reject it (issue F9).
- The OTP is not length- or digit-checked.
- `.trim()` is used for the emptiness test, but the **untrimmed** `email.text.toString()` /
  `otp.text.toString()` values are what actually get sent (issue F10).
- `email.text!!` / `otp.text!!` use `!!` — safe in practice for `TextInputEditText`, but a NPE waiting
  on a refactor.

---

## 5. API contracts

Both calls go through `Util.getRetrofit()` -> `https://server.salvationlamb.com`, both are
unauthenticated, both are wrapped in `try/catch` and guarded by `Commons().isNetworkAvailable(this)`,
and both show the SpotsDialog.

### 5.1 Step 1 — request the OTP: `checkValid(emailtxt: String)`

| Item | Value |
|---|---|
| Retrofit method | `RetrofitAPI.postForgotPassword(data)` |
| HTTP | `POST api/v1/password/forgot-password` |
| Headers | none |
| Request body | `{ "email": <String> }` — **in forgot mode only this one key**. (Verify mode adds `isVerifyMail: true`; that branch belongs to `OTP_VERIFY.md`) |

**Success (200)**
1. Toasts `response.body()?.get("message").toString()` — the server's message, **with JSON quotes**.
2. `otp_op.visibility = VISIBLE`.
3. `forgot_btn.text = "confirm"`.
4. Re-wires `forgot_btn` to the OTP step (see §3).

**Error (non-200)**
- Parses `errorBody()` into a `JsonObject`, reads `errorMessage` via `.get(...).toString()`.
- `email.error = errorMessage` **and** Toast of the same string (quotes included).
- `status` is **not** read in this method (unlike Login/Register, which log it).

**`onFailure`** — `dialog.dismiss()` + `Log.e("ForgotPasswordActivity.checkValid", "fail")`. **No user feedback.**

`dialog.dismiss()` also runs at the end of `onResponse` for both branches.

### 5.2 Step 2 — submit the OTP: `checkOtp(emailtxt: String, otpTxt: String)`

| Item | Value |
|---|---|
| Retrofit method | **`RetrofitAPI.postForgotPasswordOtp(data)`** (the `else` branch of the `page` check) |
| HTTP | `POST api/v1/password/verify-otp` |
| Headers | none |
| Request body | `{ "email": <String>, "otp": <String> }` |
| Debug log | `Log.e("data", data.toString())` — **logs the email and OTP in clear text** (issue F11) |

> The method picks the endpoint with `if (page.contentEquals("verify")) postVerifyUser else postForgotPasswordOtp`.
> In this mode it is always `postForgotPasswordOtp` -> `password/verify-otp`.
> Note the easily-confused sibling endpoint `users/verify-otp`, which belongs to `OTP_VERIFY.md`.

**Success (200)**
1. Toasts the server `message` (quoted).
2. Navigates to `ChangePasswordActivity` with extras **`email`** and **`otp`**, then `finish()`.
   -> This is the contract `CHANGE_PASSWORD.md` relies on for its **reset mode**
   (`POST api/v1/password/confirm-password`). `ChangePasswordActivity` reads exactly these two keys.

**Error (non-200)**
- `otp.error = errorMessage` + Toast (quoted). `status` not read.

**`onFailure`** — `dialog.dismiss()` + `Log.e("ForgotPasswordActivity.checkOTP", "fail")`. **No user feedback.**

### 5.3 Models

**None.** This screen never maps a response to a data class — it only reads `message` / `errorMessage`
off the raw `JsonObject`. It is the only AUTH screen with no `DataModels.kt` dependency.

---

## 6. Storage read / written

| Key | Operation |
|---|---|
| — | **none** |

`UserPreferences` is **not imported** by this class. No token, no userId, nothing is persisted.
The `email`/`otp` pair is handed to the next screen purely through intent extras.

---

## 7. Global state touched (`com.veha.util.Util`)

| Field | Operation | Notes |
|---|---|---|
| `Util.userId` | **read only** — and only in the verify branch of `checkOtp` | not used by forgot mode |

Read-only use: `Util.getRetrofit()`. This screen **writes nothing** to global state.

---

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| Email blank | `Enter Email` | Toast LENGTH_LONG + `email.error` |
| OTP blank | `Enter OTP` | Toast LENGTH_LONG + `otp.error` |
| Step 1 succeeds | the server's `message`, **with JSON quotes** (e.g. `"OTP sent to your mail"`) | Toast LENGTH_LONG |
| Step 1 fails | the server's `errorMessage`, **with quotes** (e.g. `"Invalid Email-Id"`) | Toast + `email.error` |
| Step 2 succeeds | the server's `message`, **with quotes** | Toast LENGTH_LONG |
| Step 2 fails | the server's `errorMessage`, **with quotes** (e.g. wrong/expired OTP) | Toast + `otp.error` |
| Either call in flight | `Please Wait` | SpotsDialog (non-cancelable) |
| Network failure or offline | **(none)** | silent (issues F12, F13) |

Every message except the header (`@string/Product_name`) is hard-coded or server-supplied.

---

## 9. Navigation map

| Trigger | Destination | Extras | `finish()`? |
|---|---|---|---|
| `cancel_btn` | back to `LoginActivity` (via `finish()`) | — | **yes** |
| OTP accepted (step 2, 200) | `ChangePasswordActivity` | **`email`**, **`otp`** | **yes** |
| Back press | `LoginActivity` (new instance pushed) | — | yes, but a Login is also started |
| Step 1 / step 2 error | stays on this screen | — | — |

**Entry contract:** must be started **without** a `page` extra (or with any value other than
`"verify"`). `LoginActivity.forgot_pwd` does exactly that.

**Exit contract to CHANGE_PASSWORD:** the extras are literally `"email"` and `"otp"`.
`ChangePasswordActivity` reads both with `intent.getStringExtra(...)` and switches into reset mode
when they are present. **Renaming either key breaks the password-reset flow** — two-agent change,
escalate to PM.

---

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | `setContentView` -> build SpotsDialog -> read `page` -> branch into forgot mode -> set the two texts -> wire `forgot_btn` + `cancel_btn` |
| `onDestroy` | `super.onDestroy()` then unconditional `dialog.dismiss()`. Retrofit calls are **not** cancelled (issue F14) |
| `onBackPressed` | `super.onBackPressed()` then `finish()` + `startActivity(LoginActivity)` — **shared with verify mode**, which is where it makes sense; in forgot mode it duplicates Login (issue F7) |

`page` and `emailID` are `lateinit var`. **`emailID` is only assigned in the verify branch**, so in
forgot mode it stays uninitialised — harmless today because the forgot paths read `email.text`
instead, but any new code that touches `emailID` here will throw
`UninitializedPropertyAccessException` (issue F15).

**No rotation handling.** On rotation the activity is recreated and `onCreate` re-runs the `else`
branch: `otp_op` returns to `gone` and `forgot_btn` reverts to "Verify" with the step-1 listener,
while the `EditText`s keep their text — the user is silently thrown back to step 1 with their OTP
still typed but the field hidden (issue F16).

---

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `Util.getRetrofit()`, `RetrofitAPI.postForgotPassword`, `RetrofitAPI.postForgotPasswordOtp` | PLATFORM / NETWORK | both steps |
| `Commons().isNetworkAvailable` | PLATFORM / COMMONS | connectivity guard |
| `dmax.dialog.SpotsDialog` | PLATFORM / BUILD_CONFIG | progress dialog |
| `@string/Product_name` | APPSHELL / THEMING | header text |
| `ChangePasswordActivity` | AUTH / `CHANGE_PASSWORD.md` | destination of the `email`+`otp` handoff |
| `LoginActivity` | AUTH / `LOGIN.md` | entry point and Back target |
| `OTP_VERIFY.md` | AUTH (same class!) | shares this file — coordinate every edit |

Not used here: `UserPreferences`, `DataModels`, `Util.isValidEmail`, `Picasso`, `lifecycleScope`.

---

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| F1 | Mode selection depends on `getStringExtra("page").toString()` producing the **literal `"null"`**; a "cleanup" to a proper null check or a typo in the extra silently flips the screen into verify mode | High | use a constant + `intent.getStringExtra("page") == MODE_VERIFY`, coordinated with `OTP_VERIFY.md` |
| F2 | No `ScrollView` — with the keyboard open on a small screen the buttons can be unreachable; no `windowSoftInputMode` in the manifest | Medium | wrap in `ScrollView` and/or set `adjustResize` |
| F3 | `email` has no `inputType="textEmailAddress"` | Medium | add `inputType` and `imeOptions` |
| F4 | `otp` has no `inputType="number"`, no `maxLength`, no auto-advance | Medium | add numeric input type and a length cap |
| F5 | After step 1 the email field stays **editable**, so the user can change the address and then submit an OTP for a different email than the one it was sent to — `checkOtp` re-reads `email.text` at tap time | High | disable the email field once the OTP is requested, or pass the submitted address |
| F6 | Button label becomes lower-case **"confirm"** | Cosmetic | use "Confirm" / a string resource |
| F7 | `onBackPressed` pushes a **new** `LoginActivity` even though Login is already on the stack | Medium | just `finish()` in forgot mode (shared method — coordinate with OTP_VERIFY) |
| F8 | `if (forgot_btn.text != "verify")` is dead code and compares against the wrong case (`"Verify"`) | Low | delete the condition |
| F9 | **No email format validation** — `Util.isValidEmail` is never called, unlike Login/Register | Medium | add the same check for consistent UX |
| F10 | Emptiness is checked on the trimmed value but the **untrimmed** text is sent | Low | send `.trim()` |
| F11 | `Log.e("data", data.toString())` prints the email **and OTP** to logcat in release builds | Medium (privacy) | remove the log |
| F12 | `onFailure` shows nothing to the user on either call | High (UX) | toast a generic failure message |
| F13 | Offline -> the method returns silently; no dialog, no toast | Medium | toast "No internet connection" |
| F14 | Retrofit calls are never cancelled in `onDestroy`; the callback touches views of a dead activity | High | keep the `Call` and `cancel()` it |
| F15 | `emailID` is `lateinit` but assigned **only** in verify mode — any new forgot-mode code reading it crashes | Medium | initialise it in both branches |
| F16 | Rotation resets the screen to step 1 (`otp_op` back to `gone`, listener reverted) while the typed OTP survives in the hidden field | Medium | persist the step in `onSaveInstanceState` |
| F17 | No "resend OTP" button and no cooldown/timer | Medium (product) | ask the customer |
| F18 | `message` / `errorMessage` are toasted with their JSON quotes | Low | use `asString` instead of `toString()` |
| F19 | Success and failure of step 1 are indistinguishable to an automated test — both just toast server text | Low | add explicit states |
| F20 | `status` is never logged here, unlike Login/Register, making server issues harder to diagnose | Low | log it |

---

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/ForgotPasswordActivity.kt` | **shared with `OTP_VERIFY.md`** — this agent owns the `else`/forgot branch, `checkValid`'s single-key body, and the `postForgotPasswordOtp` path of `checkOtp` |
| `app/src/main/res/layout/activity_forgot_password.xml` | **shared with `OTP_VERIFY.md`** — both modes render the same views |
| `<activity android:name=".ForgotPasswordActivity">` block in `AndroidManifest.xml` | shared with PLATFORM/BUILD_CONFIG — notify lead |

**Not owned (escalate):** `RetrofitAPI.kt`, `Util.java`, `Commons.kt`, `strings.xml`,
`ChangePasswordActivity.kt`, `LoginActivity.kt`, and every other screen.

---

## 14. How to make common changes

**Add email format validation (fixes F9):** in the step-1 `forgot_btn` listener, add an
`else if (!Util.isValidEmail(email.text.toString()))` branch before `checkValid(...)`, setting
`email.error` and toasting — match the wording Login uses (`"Invalid Email"`). Update §4 + §8.

**Add a "Resend OTP" link (F17):** add a `TextView` to the layout **with a new id that does not exist
in the verify flow's expectations**, make it visible at the same moment `otp_op` becomes visible, and
have it call `checkValid(email.text.toString())` again. Because the layout is shared, you must tell
`OTP_VERIFY.md` via the lead — verify mode will render your new view too and needs its own
visibility rule.

**Lock the email field after step 1 (fixes F5):** in `checkValid`'s 200 handler add
`email.isEnabled = false` next to `otp_op.visibility = View.VISIBLE`, and re-enable it if you add a
"change email" affordance. Verify-mode impact: none, that branch never shows `email_op`.

**Change the OTP endpoint or body:** `postForgotPasswordOtp` lives in `RetrofitAPI.kt` — **PLATFORM /
NETWORK task via PM**. Do not swap it for `postVerifyUser`; that is the verify flow's endpoint and
hits `users/verify-otp`.

**Change what is passed to Change Password:** the extras `"email"` and `"otp"` are a contract with
`CHANGE_PASSWORD.md`. Escalate to PM so both agents change together.

**Restyle:** colors, `rounded_border_login_register` and `@string/Product_name` belong to
APPSHELL/THEMING — only layout-local attributes may change here, and remember the file is shared.

---

## 15. Change log

| Change | Detail |
|---|---|
| Created | Initial FORGOT_PASSWORD module agent, documented line-by-line from the forgot branch of `ForgotPasswordActivity.kt` (219 lines) and `activity_forgot_password.xml` (119 lines): 10 views across 2 visual states, 5 actions, 2 emptiness-only validations, 2 API calls, zero storage writes, 8 user-visible strings, 4 navigation edges, 20 known issues. Recorded the `page == "null"` mode-selection quirk, the dead `forgot_btn.text != "verify"` check, and the `email`+`otp` handoff contract with CHANGE_PASSWORD. |
