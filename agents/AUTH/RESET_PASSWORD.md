# RESET_PASSWORD Module Agent

> Team: **AUTH** · Reports to: `agents/AUTH/AUTH_LEAD.md`
> This agent knows **every detail** of the **reset-after-OTP** password screen: the hidden
> old-password field, the `email` + `otp` extras it consumes, the unauthenticated API call,
> the silent success, error messages, UI and navigation.
> It may only edit the files listed in §13 **Owned files**.

---

## ⚠ Shared-file warning (read before editing)

`ChangePasswordActivity.kt` and `activity_change_password.xml` are **one class and one layout serving
two different screens**, selected by the presence of the **`email` intent extra**:

| `email` extra | Mode | Owning agent |
|---|---|---|
| **present** | Reset after OTP (user forgot their password) | **this agent** |
| **absent or blank** | Signed-in change (user knows their old password) | `CHANGE_PASSWORD.md` |

```kotlin
val email = intent.getStringExtra("email")
val otp   = intent.getStringExtra("otp")
if (TextUtils.isEmpty(email?.trim())) old_pwd_op.visibility = View.VISIBLE   // <- CHANGE_PASSWORD.md
else                                  old_pwd_op.visibility = View.GONE     // <- this agent
```

**This agent owns the `else` path only** — the `old_pwd_op` GONE branch and the whole
`forgotPassword()` method. The `onCreate` prologue, the shared validation chain, the layout,
`onPause`/`onResume`/`onDestroy` and the two buttons are **shared**: every edit to them must be
reviewed with `CHANGE_PASSWORD.md` through the lead before it ships.

> Note: here `getStringExtra` is **not** followed by `.toString()`, so a missing extra is a real
> `null` and `email?.trim()` is null-safe. This is the **opposite** convention to
> `ForgotPasswordActivity` — the very screen that launches this one — which turns a missing extra
> into the literal string `"null"` (issue P1).

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | shows **"Change Password"** — although this is a *reset* (issue P3) |
| Class | `com.veha.activity.ChangePasswordActivity` (Kotlin, `AppCompatActivity`) — **reset mode** |
| Source | `app/src/main/java/com/veha/activity/ChangePasswordActivity.kt` (203 lines) |
| Layout | `app/src/main/res/layout/activity_change_password.xml` (151 lines) |
| Manifest entry | `<activity android:name=".ChangePasswordActivity" android:exported="false"/>` |
| View access | `kotlinx.android.synthetic.main.activity_change_password.*` |
| Required extras | **`email`** and **`otp`** — both supplied by `ForgotPasswordActivity.checkOtp` |
| Entered from | `ForgotPasswordActivity` (**forgot** mode, OTP accepted) — the **only** entry point |
| Exits to | `finish()` — in practice back to `LoginActivity`, because `ForgotPasswordActivity` already `finish()`ed itself |
| Progress indicator | `dmax.dialog.SpotsDialog`, message **"Please Wait"**, `setCancelable(false)` |
| Requires a session | **no** — the OTP is the proof of identity; no token, no `Util.userId` |

---

## 2. UI inventory

Root: `ConstraintLayout`, background `@color/primary_blue`. Body: a vertical `LinearLayout` card —
`@drawable/rounded_border_login_register`, `padding 20dp`, `marginTop 80dp`. **No `ScrollView`**
(issue P2). No logo and no "SalvationLamb" header.

| View id | Type | Hint / text | Visibility in this mode | Notes |
|---|---|---|---|---|
| *(no id)* | `TextView` | **"Change Password"** | visible | wrong wording for a reset (issue P3) |
| `old_pwd_op` | `TextInputLayout` | hint "Old Password" | **`GONE`** (set in code) | hidden — the user does not know it |
| `old_pwd` | `TextInputEditText` | — | hidden with its parent | **still read** by the click listener, yielding `""` |
| `new_pwd_op` | `TextInputLayout` | hint **"New Password"** | visible | `passwordToggleEnabled="true"` |
| `new_pwd` | `TextInputEditText` | — | visible | `inputType="textPassword"` |
| `cnfm_pwd_op` | `TextInputLayout` | hint **"Confirm Password"** | visible | **no `passwordToggleEnabled`** (issue P4) |
| `cnfm_pwd` | `TextInputEditText` | — | visible | **no `inputType="textPassword"`** — typed in **clear text** (issue P5) |
| `otp_op` | `TextInputLayout` | hint **"OTP"** | **`gone`, never shown** | **dead view** — see below |
| `otp` | `TextInputEditText` | — | never shown | **dead view** |
| `change_pwd_btn` | `Button` | **"Change"** | visible | primary blue, white text |
| `cancel_btn` | `Button` | **"Cancel"** | visible | white background, blue text |

So this mode shows **exactly two editable fields**: New Password and Confirm Password.

### The dead OTP views (most relevant here)

`otp_op` / `otp` exist in the layout with `android:visibility="gone"` and are **never referenced by
`ChangePasswordActivity.kt`**. In this mode the OTP is **not typed here** — it was already entered on
`ForgotPasswordActivity` and arrives as an intent extra, which this screen forwards untouched. These
views are left-over scaffolding from an earlier design where the OTP was captured on this screen
(issue P6). Do not wire them up without talking to `FORGOT_PASSWORD.md` through the lead.

---

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| `onCreate` (shared) | `setContentView` -> `UserPreferences(this)` (**created but unused in this mode**) -> build SpotsDialog -> read `email`/`otp` extras -> **`email` present, so `old_pwd_op` = GONE** -> wire the two buttons. **No API call on open** |
| `change_pwd_btn` click | Snapshots `passwordTxt = new_pwd.text`, `cnfmPasswordTxt = cnfm_pwd.text`, `oldPasswordTxt = old_pwd.text` (**read even though hidden**, so always `""`), runs the shared 4-step validation chain (§4), then — because `email` is present — builds the OTP body and calls `forgotPassword(data)` |
| `cancel_btn` click | `finish()` — abandons the reset; the OTP is consumed/wasted and the user must start the forgot flow again |
| Back press | **not overridden** — same effect as Cancel |

---

## 4. Validation rules (shared chain)

An `if / else if` chain in the click listener; each branch toasts **and** sets a field `.error`.
Only the first failure is reported. **The chain is identical in both modes** — editing it affects
`CHANGE_PASSWORD.md` too.

| Order | Condition | Field `.error` | Toast | Effective here? |
|---|---|---|---|---|
| 1 | `!Util.isValidPassword(passwordTxt)` | `new_pwd` | `Password must contain 1 capital, 1 small, 1 number, 1 spl char and length greater than 8` | yes |
| 2 | `TextUtils.isEmpty(passwordTxt.trim())` | `new_pwd` | `Enter Password` | **unreachable** (issue P7) |
| 3 | `!passwordTxt.equals(cnfmPasswordTxt, false)` | `cnfm_pwd` | `New Password and confirm password are not same` | yes |
| 4 | `oldPasswordTxt.equals(cnfmPasswordTxt, false)` | `new_pwd` | `new password is same as old password` | **inert** (issue P9) |
| — | all pass | — | `forgotPassword(data)` | |

### Problems specific to this mode

- **Step 4 is dead weight here** (issue P9). `old_pwd` is `GONE`, so `oldPasswordTxt` is always `""`,
  which can never equal a password that passed `isValidPassword`. There is therefore **no "same as
  your old password" protection during a reset** — a user can reset to the password they already had.
- **Step 2 is unreachable** (issue P7): an empty password already fails `isValidPassword` at step 1.
- Step 4 also compares old against **confirm** rather than **new** (issue P8) — harmless here only
  because the check is inert anyway.
- **Neither `email` nor `otp` is validated** — both extras are forwarded blindly to the server
  (issue P10). If `ForgotPasswordActivity` ever passes a missing `otp`, `addProperty("otp", null)`
  sends a JSON null.

---

## 5. API contract — `forgotPassword(data: JsonObject)`

| Item | Value |
|---|---|
| Retrofit method | `RetrofitAPI.postChangeForgotPassword(data)` |
| HTTP | `POST api/v1/password/confirm-password` |
| Base URL source | `Util.getRetrofit()` -> `https://server.salvationlamb.com` |
| Headers | **none** — the OTP is the proof of identity |
| Guard | `try/catch` + `Commons().isNetworkAvailable(this)` |
| Progress | `dialog.show()` before the call |
| Request body | `{ "email": <extra>, "otp": <extra>, "password": <new password> }` |

> Body-key note: this endpoint expects **`password`**; the signed-in endpoint expects
> **`newPassword`** and also sends `userId` + `oldPassword`. That asymmetry is the server's contract,
> not a typo. This call sends **no `userId`**.

**Success (200)**
1. `Log.e("ok1", response.code().toString())` — a debug log left in place.
2. `finish()`.

**There is no success message of any kind** (issue P15). The screen simply disappears and the user
lands on Login with no confirmation that their password was changed — the single worst UX gap in the
AUTH flow, and a direct contrast with the signed-in mode, which toasts "Password changed successfully".

**Error (non-200)** — Toast **"Something went wrong"**. The block that parsed `status` and
`errorMessage` (and logged the raw response) is **commented out**, so an **expired or already-used
OTP** is indistinguishable from a server fault (issue P14). This matters more here than in the
signed-in mode, because an expired OTP is the most likely failure and the user has no way to learn
they must restart the forgot flow.

**`onFailure`** — `dialog.dismiss()` + `Log.e("ChangePasswordActivity.forgotPassword", "fail")`. No user feedback.

`call.cancel()` runs at the end of `onResponse`.

### Models

**None.** No response is mapped to a data class. The `Gson` import survives only because of the
commented-out error parsing (issue P16).

---

## 6. Storage read / written

| Key | Operation |
|---|---|
| — | **none** |

`UserPreferences` **is** instantiated in the shared `onCreate`, but this mode never reads or writes
it — no token lookup, no session cleanup (issue P13). Nothing is persisted: after a successful reset
the user must log in manually with the new password, which is the correct outcome since they never
had a session here.

---

## 7. Global state touched (`com.veha.util.Util`)

| Field | Operation |
|---|---|
| — | **none** |

Read-only use: `Util.isValidPassword`, `Util.getRetrofit()`. Unlike the signed-in mode, `Util.userId`
is **not** read — correctly, since the user is not signed in.

---

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| New password fails the regex (**or is empty**) | `Password must contain 1 capital, 1 small, 1 number, 1 spl char and length greater than 8` | Toast + `new_pwd.error` |
| New password empty | `Enter Password` | **unreachable** (issue P7) |
| Confirm does not match | `New Password and confirm password are not same` | Toast + `cnfm_pwd.error` |
| New password equals the old one | `new password is same as old password` | **inert in this mode** (issue P9) |
| **Reset succeeds** | **(none)** | silent `finish()` (issue P15) |
| Non-200 (incl. expired/invalid OTP) | `Something went wrong` | Toast — server reason discarded |
| Call in flight | `Please Wait` | SpotsDialog (non-cancelable) |
| Network failure or offline | **(none)** | silent (issues P18, P19) |

All strings are hard-coded in Kotlin/XML; none are in `res/values/strings.xml`.

---

## 9. Navigation map

| Trigger | Destination | Extras | `finish()`? |
|---|---|---|---|
| Reset succeeds | back down the stack — in practice `LoginActivity` | — | **yes** (silently) |
| `cancel_btn` | back down the stack — in practice `LoginActivity` | — | **yes** |
| Back press | same as Cancel (default behaviour) | — | — |
| Non-200 / failure | stays on this screen | — | — |

**Entry contract (inbound):** `ForgotPasswordActivity.checkOtp` starts this screen with extras
literally **`"email"`** and **`"otp"`**, then calls `finish()` on itself. Renaming either key silently
flips this screen into **signed-in mode** — it would show the Old Password field and call the
*authenticated* endpoint without a session. This is the shared contract documented in
`FORGOT_PASSWORD.md` §9; any change is a **two-agent, PM-coordinated** task.

**Why the exit lands on Login:** this screen never navigates explicitly. Because
`ForgotPasswordActivity` already removed itself from the stack, `finish()` reveals whatever was below
it — normally `LoginActivity`.

---

## 10. Lifecycle (shared)

| Callback | Behaviour |
|---|---|
| `onCreate` | layout -> `UserPreferences` (unused here) -> SpotsDialog -> read extras -> `old_pwd_op` GONE -> wire buttons |
| `onPause` | `dialog.dismiss()` |
| `onResume` | `dialog.dismiss()` — **dismisses the progress dialog every time the screen returns to the foreground**, so a request that is still running loses its spinner (issue P20) |
| `onDestroy` | `dialog.dismiss()` |

The Retrofit call is cancelled **after** the response (`call.cancel()` inside `onResponse`) but
**never on destroy**, so an in-flight request still outlives the activity (issue P21).

**No rotation handling.** The `EditText`s keep their text and the mode is recomputed from the intent
extras in `onCreate`, so the mode survives a rotation — but an in-flight request is leaked and its
dialog is gone.

---

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `Util.getRetrofit()`, `RetrofitAPI.postChangeForgotPassword` | PLATFORM / NETWORK | the unauthenticated call |
| `Util.isValidPassword` | PLATFORM / COMMONS | the password rule (shared with LOGIN, REGISTER, CHANGE_PASSWORD) |
| `Commons().isNetworkAvailable` | PLATFORM / COMMONS | connectivity guard |
| `dmax.dialog.SpotsDialog` | PLATFORM / BUILD_CONFIG | progress dialog |
| `UserPreferences` | PLATFORM / STORAGE | instantiated in shared `onCreate`, unused here |
| `ForgotPasswordActivity` | AUTH / `FORGOT_PASSWORD.md` | supplies the `email` + `otp` extras — the inbound contract |
| `LoginActivity` | AUTH / `LOGIN.md` | effective destination after `finish()` |
| `CHANGE_PASSWORD.md` | AUTH (same class!) | shares this file — coordinate every edit |

---

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| P15 | A successful reset shows **no confirmation at all** — the screen just closes | **High (UX)** | toast "Password reset successfully" before `finish()` |
| P14 | The error branch has its `errorMessage` parsing **commented out**, so an **expired/invalid OTP** shows only "Something went wrong" and the user is never told to restart the forgot flow | **High (UX)** | restore the parsing and special-case OTP errors |
| P18 | `onFailure` shows nothing to the user | **High (UX)** | toast a generic failure message |
| P21 | The Retrofit call is not cancelled in `onDestroy` (only after a response) | **High** | keep the `Call` and `cancel()` it |
| P9 | The "same as old password" check is **inert** in this mode (`old_pwd` is GONE, so the value is `""`), so a user can reset to their current password | Medium | enforce server-side, or drop the check explicitly in this branch |
| P10 | Neither `email` nor `otp` is validated; a missing `otp` would send a JSON null | Medium | guard the extras and bail out with a message |
| P7 | The `"Enter Password"` branch is unreachable — an empty password fails `isValidPassword` first | Medium | check emptiness before the regex (**shared chain** — coordinate) |
| P8 | The "same as old password" check compares **old vs confirm**, not old vs new | Medium | compare `oldPasswordTxt` with `passwordTxt` (**shared chain**) |
| P5 | `cnfm_pwd` has **no `inputType="textPassword"`** — the confirmation is typed in clear text | Medium (security) | add `inputType` (**shared layout**) |
| P20 | `onResume` calls `dialog.dismiss()`, killing the spinner of a still-running request | Medium | remove the `onResume` override (**shared**) |
| P19 | Offline -> the method returns silently; no dialog, no toast — and the OTP may expire meanwhile | Medium | toast "No internet connection" |
| P2 | No `ScrollView` and no `windowSoftInputMode` | Medium | wrap in `ScrollView` / set `adjustResize` (**shared layout**) |
| P1 | Extras are read **without** `.toString()` here but **with** it in `ForgotPasswordActivity`, the screen that launches this one — two opposite null conventions in one flow | Medium | standardise (PM-level, spans several agents) |
| P3 | The heading says "Change Password", not "Reset Password"; there is no logo/branding and nothing tells the user which email the reset applies to | Medium | set the heading per mode and show the email |
| P13 | `UserPreferences` is instantiated by the shared `onCreate` but unused in this mode | Low | harmless; tidy if the shared prologue is refactored |
| P4 | `cnfm_pwd_op` has no `passwordToggleEnabled`, unlike `new_pwd_op` | Low | add for consistency (**shared layout**) |
| P6 | `otp_op` / `otp` are **dead views** — the OTP is passed as an extra, not typed here | Low | delete (**shared layout** — coordinate with CHANGE_PASSWORD and FORGOT_PASSWORD) |
| P12 | `Log.e("ok1", ...)` debug logging left in the success path | Low | remove |
| P16 | `Gson` is imported but unused (only the commented-out code referenced it) | Cosmetic | remove with the P14 fix |
| P17 | After a reset the user must find their own way back to Login and sign in; no auto-login and no guidance | Low (product) | ask the customer |
| P23 | Passwords are sent in plain JSON while the app sets `usesCleartextTraffic="true"` globally | Medium (security) | PLATFORM/BUILD_CONFIG to restrict cleartext |

---

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/ChangePasswordActivity.kt` | **shared with `CHANGE_PASSWORD.md`** — this agent owns the `old_pwd_op` GONE branch, the `email`/`otp`/`password` body builder, and the whole `forgotPassword()` method |
| `app/src/main/res/layout/activity_change_password.xml` | **shared with `CHANGE_PASSWORD.md`** — both modes render the same views; this mode hides `old_pwd_op` |
| `<activity android:name=".ChangePasswordActivity">` block in `AndroidManifest.xml` | shared with PLATFORM/BUILD_CONFIG — notify lead |

**Not owned (escalate):** `RetrofitAPI.kt`, `UserPreferences.kt`, `Util.java`, `Commons.kt`,
`ForgotPasswordActivity.kt` (AUTH sibling — owns the inbound contract), `LoginActivity.kt`,
`colors.xml` / `styles.xml`.

---

## 14. How to make common changes

**Add a success message (fixes P15):** in `forgotPassword`'s 200 branch, add
`Toast.makeText(this, "Password reset successfully", Toast.LENGTH_LONG).show()` before `finish()`.
This method is **exclusive to this agent** — no coordination needed. Update §5 and §8 here.

**Show the real server error, especially an expired OTP (fixes P14):** restore the commented-out
`Gson().fromJson(response.errorBody()?.string(), JsonObject::class.java)` parsing in this method,
read `errorMessage`, toast it, and — when it indicates an OTP problem — tell the user to restart the
forgot flow. Exclusive to this agent.

**Show which email is being reset (P3):** the `email` extra is already read in `onCreate`; display it
in a `TextView`. Adding a view touches the **shared layout** — notify `CHANGE_PASSWORD.md` via the
lead, and set its visibility only in this branch.

**Validate the incoming extras (P10):** at the top of the `else` branch, bail out with a Toast and
`finish()` if `otp` is null/blank. Touches the shared `onCreate` prologue — coordinate.

**Reorder or trim the validation chain (P7/P8/P9):** the chain is **shared**. Agree the new order
with `CHANGE_PASSWORD.md` first (step 4 is meaningful there, inert here), then update §4 and §8 in
**both** docs.

**Capture the OTP on this screen instead (P6):** the OTP is currently collected by
`ForgotPasswordActivity`. Moving it here means un-hiding `otp_op`, reading `otp.text`, and making
`FORGOT_PASSWORD.md` stop forwarding the extra — a **three-agent change**, escalate to PM.

**Change the request body or endpoint:** `postChangeForgotPassword` lives in `RetrofitAPI.kt` —
**PLATFORM / NETWORK task via PM**. Do not rename `password` to `newPassword`; that is the other
endpoint's contract.

---

## 15. Change log

| Change | Detail |
|---|---|
| Created | Initial RESET_PASSWORD module agent, split out of `CHANGE_PASSWORD.md` so each mode of `ChangePasswordActivity` has its own agent (mirroring the FORGOT_PASSWORD / OTP_VERIFY split). Documented the `else` branch of `ChangePasswordActivity.kt` (203 lines) and `activity_change_password.xml` (151 lines): 11 view ids with this mode's visibility (only 2 editable fields), 4 actions, the shared validation chain annotated for which steps are effective here, the unauthenticated `password/confirm-password` call, zero storage writes, zero global writes, 8 user-visible strings, 4 navigation edges, 23 known issues. Recorded the inbound `email`+`otp` contract from `FORGOT_PASSWORD.md`, the silent success (P15), the indistinguishable expired-OTP error (P14) and the inert "same as old password" check (P9). |
