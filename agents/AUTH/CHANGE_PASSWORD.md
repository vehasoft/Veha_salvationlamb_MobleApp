# CHANGE_PASSWORD Module Agent

> Team: **AUTH** · Reports to: `agents/AUTH/AUTH_LEAD.md`
> This agent knows **every detail** of the **signed-in** Change Password screen: the three fields,
> the validation chain, the DataStore token lookup, the authenticated API call, error messages,
> UI and navigation.
> It may only edit the files listed in §13 **Owned files**.

---

## ⚠ Shared-file warning (read before editing)

`ChangePasswordActivity.kt` and `activity_change_password.xml` are **one class and one layout serving
two different screens**, selected by the presence of the **`email` intent extra**:

| `email` extra | Mode | Owning agent |
|---|---|---|
| **absent or blank** | Signed-in change (user knows their old password) | **this agent** |
| **present** | Reset after OTP (user forgot their password) | `RESET_PASSWORD.md` |

```kotlin
val email = intent.getStringExtra("email")
val otp   = intent.getStringExtra("otp")
if (TextUtils.isEmpty(email?.trim())) old_pwd_op.visibility = View.VISIBLE   // <- this agent
else                                  old_pwd_op.visibility = View.GONE     // <- RESET_PASSWORD.md
```

**This agent owns the `TextUtils.isEmpty(email)` path only** — the `old_pwd_op` VISIBLE branch and the
whole `changePassword()` method. The `onCreate` prologue, the shared validation chain, the layout,
`onPause`/`onResume`/`onDestroy` and the two buttons are **shared**: every edit to them must be
reviewed with `RESET_PASSWORD.md` through the lead before it ships.

> Note: here `getStringExtra` is **not** followed by `.toString()`, so a missing extra is a real
> `null` and `email?.trim()` is null-safe. This is the **opposite** convention to
> `ForgotPasswordActivity`, which turns a missing extra into the literal string `"null"` (issue C1).

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **Change Password** |
| Class | `com.veha.activity.ChangePasswordActivity` (Kotlin, `AppCompatActivity`) — **signed-in mode** |
| Source | `app/src/main/java/com/veha/activity/ChangePasswordActivity.kt` (203 lines) |
| Layout | `app/src/main/res/layout/activity_change_password.xml` (151 lines) |
| Manifest entry | `<activity android:name=".ChangePasswordActivity" android:exported="false"/>` |
| View access | `kotlinx.android.synthetic.main.activity_change_password.*` (single wildcard import — no id clash) |
| Required extras | **none** — the absence of `email` *is* the mode selector |
| Entered from | `SettingsActivity` (`change_password` row), `AboutActivity` (`changePass` row) — both with no extras |
| Exits to | `finish()` back to the caller on success; `LoginActivity` only from the (dead) session-lost branch |
| Progress indicator | `dmax.dialog.SpotsDialog`, message **"Please Wait"**, `setCancelable(false)` |
| Requires a session | **yes** — a DataStore `token` and `Util.userId` |

---

## 2. UI inventory

Root: `ConstraintLayout`, background `@color/primary_blue`, `tools:context=".ChangePasswordActivity"`.
Body: a vertical `LinearLayout` card — `@drawable/rounded_border_login_register`, `padding 20dp`,
`marginTop 80dp`. **No `ScrollView`** (issue C2). No logo and no "SalvationLamb" header — the only
AUTH screen without branding.

| View id | Type | Hint / text | Visibility in this mode | Notes |
|---|---|---|---|---|
| *(no id)* | `TextView` | **"Change Password"** | visible | `28dp`, bold — accurate in this mode |
| `old_pwd_op` | `TextInputLayout` | hint **"Old Password"** | **VISIBLE** (set in code) | `passwordToggleEnabled="true"` |
| `old_pwd` | `TextInputEditText` | — | visible | `inputType="textPassword"` |
| `new_pwd_op` | `TextInputLayout` | hint **"New Password"** | visible | `passwordToggleEnabled="true"` |
| `new_pwd` | `TextInputEditText` | — | visible | `inputType="textPassword"` |
| `cnfm_pwd_op` | `TextInputLayout` | hint **"Confirm Password"** | visible | **no `passwordToggleEnabled`** — inconsistent with the other two (issue C4) |
| `cnfm_pwd` | `TextInputEditText` | — | visible | **no `inputType="textPassword"`** — typed in **clear text** (issue C5) |
| `otp_op` | `TextInputLayout` | hint **"OTP"** | **`gone`, never shown** | **dead view** — see below |
| `otp` | `TextInputEditText` | — | never shown | **dead view** |
| `change_pwd_btn` | `Button` | **"Change"** | visible | primary blue, white text |
| `cancel_btn` | `Button` | **"Cancel"** | visible | white background, blue text |

This mode shows **all three password fields** — it is the only configuration where `old_pwd_op` is visible.

### The dead OTP views

`otp_op` / `otp` exist in the layout with `android:visibility="gone"` and are **never referenced by the
Kotlin** in either mode — no visibility change, no text read. They are left-over scaffolding
(issue C6). Irrelevant to this mode; `RESET_PASSWORD.md` explains why they are unnecessary there too.

### Commented-out in the layout

A "Hello Everyone" heading and a "Welcome to Salvationlamb, please enter your email-id." subtitle.

---

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| `onCreate` (shared) | `setContentView` -> `UserPreferences(this)` -> build SpotsDialog -> read `email`/`otp` extras -> **`email` blank, so `old_pwd_op` = VISIBLE** -> wire the two buttons. **No API call on open** |
| `change_pwd_btn` click | Snapshots `passwordTxt = new_pwd.text`, `cnfmPasswordTxt = cnfm_pwd.text`, `oldPasswordTxt = old_pwd.text`, runs the shared 4-step validation chain (§4), then — because `email` is blank — builds the authenticated body and calls `changePassword(data)` |
| `cancel_btn` click | `finish()` only — returns to Settings / About |
| Back press | **not overridden** — default behaviour, returns to the caller |

---

## 4. Validation rules (shared chain)

An `if / else if` chain in the click listener; each branch toasts **and** sets a field `.error`.
Only the first failure is reported. **The chain is identical in both modes** — editing it affects
`RESET_PASSWORD.md` too.

| Order | Condition | Field `.error` | Toast |
|---|---|---|---|
| 1 | `!Util.isValidPassword(passwordTxt)` | `new_pwd` | `Password must contain 1 capital, 1 small, 1 number, 1 spl char and length greater than 8` |
| 2 | `TextUtils.isEmpty(passwordTxt.trim())` | `new_pwd` | `Enter Password` |
| 3 | `!passwordTxt.equals(cnfmPasswordTxt, false)` | `cnfm_pwd` | `New Password and confirm password are not same` |
| 4 | `oldPasswordTxt.equals(cnfmPasswordTxt, false)` | `new_pwd` | `new password is same as old password` |
| — | all pass | — | `changePassword(data)` |

### Problems in this chain

- **Step 2 is unreachable** (issue C7). An empty password already fails `isValidPassword` at step 1,
  so `"Enter Password"` can never be shown. The two checks are in the wrong order.
- **Step 4 compares the old password against the *confirm* field**, not the new one (issue C8). It
  works only because step 3 has already proven `passwordTxt == cnfmPasswordTxt`. Reordering or
  removing step 3 would silently break this check.
- **Step 4 is the only check that is meaningful in this mode** — in reset mode `old_pwd` is hidden, so
  it is inert there (see `RESET_PASSWORD.md`).
- The old password itself is **never validated** — not checked for emptiness, not format-checked, so a
  blank old password is sent to the server (issue C10).
- `.equals(x, false)` is `ignoreCase = false` — correct for passwords, but the literal `false` reads
  like a bug and invites "cleanup".

---

## 5. API contract — `changePassword(data: JsonObject)`

| Item | Value |
|---|---|
| Retrofit method | `RetrofitAPI.postChangePassword("Bearer $it", data)` |
| HTTP | `POST api/v1/users/change-password` |
| Base URL source | `Util.getRetrofit()` -> `https://server.salvationlamb.com` |
| Headers | `Authorization: Bearer <token>` — read from DataStore **inside** the method |
| Guard | `try/catch` + `Commons().isNetworkAvailable(this)` |
| Progress | `dialog.show()` before the call |
| Request body | `{ "userId": Util.userId, "oldPassword": <String>, "newPassword": <String> }` |

> Body-key note: this endpoint expects **`newPassword`**; the reset endpoint expects **`password`**.
> That asymmetry is the server's contract, not a typo.

### How the token is obtained (the risky part — exclusive to this mode)

```kotlin
userPreferences.authToken.asLiveData().observe(this) {
    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
        ... postChangePassword("Bearer $it", data) ...
    } else {
        // "Somthing Went Wrong \nLogin again to continue" + clear session + LoginActivity
    }
}
```

- The guard is an **`||` chain of negations and is always `true`** (issue C11). For a token of `""`:
  `!isEmpty("")` is false, but `!"".equals("null")` is **true**, so the condition passes. For `"null"`:
  `!isEmpty("null")` is already true. **No input can reach the `else`.** It should be `&&`.
- Consequence: the **entire `else` branch is dead code** — the "Login again to continue" toast, the
  `deleteAuthToken()` / `deleteUserId()` cleanup and the jump to `LoginActivity` can never run. A
  missing token is instead sent to the server as the literal header `Bearer null` (issue C12).
- A **new LiveData observer is registered on every button tap**, and DataStore re-emits on change, so
  repeated taps multiply the in-flight requests; observers are never removed (issue C13).

**Success (200)** — Toast **"Password changed successfully"**, then `finish()`.

**Error (non-200)** — Toast **"Something went wrong"**. The code that parsed `errorMessage` is
**commented out**, so the server's actual reason (e.g. "old password incorrect") is discarded
(issue C14).

**`onFailure`** — `dialog.dismiss()` + `Log.e("ChangePasswordActivity.changePassword", "fail")`. No user feedback.

`call.cancel()` runs at the end of `onResponse`.

### Models

**None.** This screen never maps a response to a data class. The `Gson` import survives only because
of the commented-out error parsing and is otherwise **unused** (issue C16).

---

## 6. Storage read / written

DataStore **`SalvationLamb`** via `UserPreferences(this)` (created in `onCreate`).

| Key | Type | Operation | When |
|---|---|---|---|
| `token` | String | **read** (observed as LiveData) | on every `change_pwd_btn` tap |
| `token` | String | *delete* | **dead code** — unreachable `else` branch |
| `userId` | String | *delete* | **dead code** — same branch |

No token is re-saved after a successful change, so if the backend invalidates sessions on password
change the stored token goes stale silently (issue C17).

---

## 7. Global state touched (`com.veha.util.Util`)

| Field | Operation | Notes |
|---|---|---|
| `Util.userId` | **read** | sent as the `userId` body key |

Read-only use: `Util.isValidPassword`, `Util.getRetrofit()`. This screen **writes nothing** to global state.

---

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| New password fails the regex (**or is empty**) | `Password must contain 1 capital, 1 small, 1 number, 1 spl char and length greater than 8` | Toast + `new_pwd.error` |
| New password empty | `Enter Password` | **unreachable** (issue C7) |
| Confirm does not match | `New Password and confirm password are not same` | Toast + `cnfm_pwd.error` |
| New password equals the old one | `new password is same as old password` | Toast + `new_pwd.error` |
| Change succeeds | `Password changed successfully` | Toast LENGTH_LONG |
| Non-200 response | `Something went wrong` | Toast — server reason discarded |
| Session lost | `Somthing Went Wrong \nLogin again to continue` (sic — typo) | **unreachable** (issue C11) |
| Call in flight | `Please Wait` | SpotsDialog (non-cancelable) |
| Network failure or offline | **(none)** | silent (issues C18, C19) |

All strings are hard-coded in Kotlin/XML; none are in `res/values/strings.xml`.

---

## 9. Navigation map

| Trigger | Destination | Extras | `finish()`? |
|---|---|---|---|
| Change succeeds | back to the caller (Settings / About) | — | **yes** |
| `cancel_btn` | back to the caller | — | **yes** |
| Back press | back to the caller (default) | — | — |
| Session lost | `LoginActivity` | — | no (**dead code**) |
| Non-200 / failure | stays on this screen | — | — |

**Entry contract:** start with **no extras**. `SettingsActivity` and `AboutActivity` both do exactly
that. Passing an `email` extra by accident silently hands the screen to `RESET_PASSWORD.md`'s branch
— hiding the Old Password field and switching to the unauthenticated endpoint.

---

## 10. Lifecycle (shared)

| Callback | Behaviour |
|---|---|
| `onCreate` | layout -> `UserPreferences` -> SpotsDialog -> read extras -> `old_pwd_op` VISIBLE -> wire buttons |
| `onPause` | `dialog.dismiss()` |
| `onResume` | `dialog.dismiss()` — **dismisses the progress dialog every time the screen returns to the foreground**, so a request that is still running loses its spinner (issue C20) |
| `onDestroy` | `dialog.dismiss()` |

This is the only AUTH screen that overrides all three. The Retrofit call is cancelled **after** the
response (`call.cancel()` inside `onResponse`) but **never on destroy**, so an in-flight request still
outlives the activity (issue C21).

`passwordTxt`, `cnfmPasswordTxt`, `oldPasswordTxt` are `lateinit var`s assigned only inside the click
listener.

**No rotation handling.** The `EditText`s keep their text and the mode is recomputed from the extras
in `onCreate`, so the screen survives — but any in-flight request is leaked and its dialog is gone.

---

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `Util.getRetrofit()`, `RetrofitAPI.postChangePassword` | PLATFORM / NETWORK | the authenticated call |
| `Util.isValidPassword` | PLATFORM / COMMONS | the password rule (shared with LOGIN, REGISTER, RESET_PASSWORD) |
| `Util.userId` | PLATFORM / COMMONS | `userId` body key |
| `UserPreferences.authToken` (+ the dead `deleteAuthToken`/`deleteUserId`) | PLATFORM / STORAGE | bearer token |
| `Commons().isNetworkAvailable` | PLATFORM / COMMONS | connectivity guard |
| `dmax.dialog.SpotsDialog` | PLATFORM / BUILD_CONFIG | progress dialog |
| `SettingsActivity` | APPSHELL / `SETTINGS.md` | caller |
| `AboutActivity` | APPSHELL / `ABOUT.md` | caller |
| `LoginActivity` | AUTH / `LOGIN.md` | dead-code exit target |
| `RESET_PASSWORD.md` | AUTH (same class!) | shares this file — coordinate every edit |

---

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| C11 | The token guard `!isEmpty(it) \|\| !it.equals("null") \|\| !it.isNullOrEmpty()` is **always true**; the whole session-lost `else` branch is unreachable | **High** | change to `&&`, or test the token explicitly |
| C12 | As a result, a missing token is sent as the header `Bearer null` instead of logging the user out | **High** | validate before calling |
| C13 | A new `authToken.asLiveData().observe(this)` is registered on **every** button tap and never removed; DataStore re-emissions can fire duplicate requests | **High** | read the token once (e.g. `lifecycleScope.launch { first() }`) outside the listener |
| C14 | The error branch has its `errorMessage` parsing **commented out** and shows a generic "Something went wrong", so "old password incorrect" is indistinguishable from a server fault | **High (UX)** | restore the parsing |
| C18 | `onFailure` shows nothing to the user | **High (UX)** | toast a generic failure message |
| C21 | The Retrofit call is not cancelled in `onDestroy` (only after a response) | **High** | keep the `Call` and `cancel()` it |
| C7 | The `"Enter Password"` branch is unreachable — an empty password fails `isValidPassword` first | Medium | check emptiness before the regex (**shared chain** — coordinate) |
| C8 | The "same as old password" check compares **old vs confirm**, not old vs new | Medium | compare `oldPasswordTxt` with `passwordTxt` (**shared chain**) |
| C10 | The old password is never validated (emptiness or format) before being sent | Medium | add an emptiness check in this mode only |
| C5 | `cnfm_pwd` has **no `inputType="textPassword"`** — the confirmation is typed in clear text | Medium (security) | add `inputType` (**shared layout**) |
| C20 | `onResume` calls `dialog.dismiss()`, killing the spinner of a still-running request | Medium | remove the `onResume` override (**shared**) |
| C19 | Offline -> the method returns silently; no dialog, no toast | Medium | toast "No internet connection" |
| C17 | No token refresh after a password change; a server-side session invalidation goes unnoticed | Medium | re-login or refresh the token on success |
| C1 | Extras are read **without** `.toString()` here but **with** it in `ForgotPasswordActivity` — two opposite null conventions in the same flow | Medium | standardise (PM-level, spans several agents) |
| C2 | No `ScrollView` and no `windowSoftInputMode`; with three fields plus the keyboard the buttons can be unreachable | Medium | wrap in `ScrollView` / set `adjustResize` (**shared layout**) |
| C4 | `cnfm_pwd_op` has no `passwordToggleEnabled`, unlike the other two fields | Low | add for consistency (**shared layout**) |
| C6 | `otp_op` / `otp` are **dead views** — present in the layout, never referenced by the code | Low | delete (**shared layout** — coordinate with RESET_PASSWORD) |
| C16 | `Gson` is imported but unused (only the commented-out code referenced it) | Cosmetic | remove with the C14 fix |
| C22 | `"Somthing Went Wrong"` is misspelled (same typo as `HomeFragment`/`MainActivity`) | Cosmetic | fix when the branch is revived |
| C23 | Passwords are sent in plain JSON while the app sets `usesCleartextTraffic="true"` globally | Medium (security) | PLATFORM/BUILD_CONFIG to restrict cleartext |

---

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/ChangePasswordActivity.kt` | **shared with `RESET_PASSWORD.md`** — this agent owns the `old_pwd_op` VISIBLE branch, the authenticated body builder, and the whole `changePassword()` method incl. the token observer |
| `app/src/main/res/layout/activity_change_password.xml` | **shared with `RESET_PASSWORD.md`** — both modes render the same views; only this mode shows `old_pwd_op` |
| `<activity android:name=".ChangePasswordActivity">` block in `AndroidManifest.xml` | shared with PLATFORM/BUILD_CONFIG — notify lead |

**Not owned (escalate):** `RetrofitAPI.kt`, `UserPreferences.kt`, `Util.java`, `Commons.kt`,
`ForgotPasswordActivity.kt` (AUTH sibling), `SettingsActivity.kt` / `activity_settings.xml` and
`AboutActivity.kt` / `activity_about.xml` (APPSHELL callers), `colors.xml` / `styles.xml`.

---

## 14. How to make common changes

**Fix the token guard (C11/C12/C13):** replace the whole `observe` block with a one-shot read
(`lifecycleScope.launch { val token = userPreferences.authToken.first() ... }`), change the `||`
chain to a real emptiness test, and keep the logout branch so it can finally execute. This method is
**exclusive to this agent** — no coordination needed. `UserPreferences` itself is PLATFORM/STORAGE:
do not edit it, only change how this screen consumes it.

**Show the real server error (fixes C14):** in the `else` branch, restore the
`Gson().fromJson(response.errorBody()?.string(), JsonObject::class.java)` parsing, read
`errorMessage`, and toast it — keeping "Something went wrong" as the fallback. Exclusive to this
agent; `RESET_PASSWORD.md` has the same fix to make separately in its own method.

**Validate the old password (C10):** add an emptiness check for `oldPasswordTxt` — but **only guard it
behind the signed-in branch**, because in reset mode the field is hidden and must stay optional.
That makes it a change to the **shared** chain: notify `RESET_PASSWORD.md` via the lead.

**Reorder the validation chain (C7/C8):** the chain is **shared**. Agree the new order with
`RESET_PASSWORD.md` first, then update §4 and §8 in **both** docs.

**Add `inputType` to the confirm field (C5) or a `ScrollView` (C2):** both are **shared layout**
changes — coordinate.

**Change the password policy:** `Util.isValidPassword` is **PLATFORM/COMMONS** and is shared with
LOGIN, REGISTER and RESET_PASSWORD — escalate to PM.

**Change the request body or endpoint:** `postChangePassword` lives in `RetrofitAPI.kt` —
**PLATFORM / NETWORK task via PM**.

---

## 15. Change log

| Change | Detail |
|---|---|
| Created | Initial CHANGE_PASSWORD module agent covering both modes of `ChangePasswordActivity.kt` (203 lines) and `activity_change_password.xml` (151 lines). |
| Split into two agents | Scope narrowed to the **signed-in** mode only (`email` extra absent). The reset-after-OTP mode moved to the new `agents/AUTH/RESET_PASSWORD.md`, mirroring the FORGOT_PASSWORD / OTP_VERIFY split. Added the shared-file warning header, marked every shared element (validation chain, layout, lifecycle) as coordinate-before-edit, and kept issues C1, C2, C4-C8, C10-C14, C16-C23 that apply to this mode. |
