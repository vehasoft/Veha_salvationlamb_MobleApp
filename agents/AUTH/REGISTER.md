# REGISTER Module Agent

> Team: **AUTH** · Reports to: `agents/AUTH/AUTH_LEAD.md`
> This agent knows **every detail** of the Sign Up screen: fields, buttons, links, the date picker,
> the gender radio group, validation order, API call, global state, error messages, UI and navigation.
> It may only edit the files listed in §13 **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **Sign Up** ("Welcome to SalvationLamb. Please create your account") |
| Class | `com.veha.activity.RegisterActivity` (Kotlin, `AppCompatActivity`) |
| Source | `app/src/main/java/com/veha/activity/RegisterActivity.kt` (202 lines) |
| Layout | `app/src/main/res/layout/activity_register.xml` (256 lines) |
| Manifest entry | `<activity android:name=".RegisterActivity" android:exported="true"/>` — **`exported="true"` with no intent-filter** (see issue R1) |
| View access | Kotlin synthetics — `activity_register.*` **and** `activity_edit_profile.*` are both wildcard-imported, plus 6 explicit single imports to break ambiguity (see issue R2) |
| Entered from | `LoginActivity` (`signup_btn`) — the only in-app entry point |
| Exits to | `LoginActivity` (`signin_btn`), `WebViewActivity` (terms), `ForgotPasswordActivity` in **verify** mode (on success) |
| Progress indicator | `android.app.ProgressDialog` — **not** `SpotsDialog` like the rest of AUTH (issue R3) |

---

## 2. UI inventory

Root: `ScrollView` > `ConstraintLayout` (background `@color/primary_blue`,
`tools:context="com.veha.activity.LoginActivity"` — a **copy-paste leftover**, the real context is
`RegisterActivity`). Body: a vertical `LinearLayout` card —
`@drawable/rounded_border_login_register`, `padding 20dp`, `marginTop 80dp`.

| View id | Type | Text / hint | Attributes |
|---|---|---|---|
| `prod_logo` | `ImageView` | — | `200dp x 50dp`, `@drawable/ic_sl_logo_01_svg`, top-left, `marginTop 10dp` |
| `fname_op` | `TextInputLayout` | hint **"First Name"** | background `@color/home_bg`, `marginTop 10dp` |
| `fname` | `TextInputEditText` | — | `inputType="textCapSentences"` |
| `lname_op` | `TextInputLayout` | hint **"Last Name"** | background `@color/home_bg` |
| `lname` | `TextInputEditText` | — | `inputType="textCapSentences"` |
| `email_op` | `TextInputLayout` | hint **"Email"** | background `@color/home_bg` |
| `email` | `TextInputEditText` | — | **no `inputType`** — plain text keyboard (same gap as Login, issue R4) |
| `mobile_op` | `TextInputLayout` | hint **"Mobile Number"** | background `@color/home_bg` |
| `mobile` | `TextInputEditText` | — | `inputType="phone"`, `maxLength="15"` |
| `password_layout` | `TextInputLayout` | hint **"Password"** | `app:passwordToggleEnabled="true"`, `passwordToggleTint @color/black` |
| `password` | `TextInputEditText` | — | `inputType="textPassword"` |
| `date` | `TextView` (**not** an input) | **"Date of birth"** (placeholder) | `50dp` high, `gravity center_vertical`, `textSize 20dp`, background `@color/home_bg`, **`android:onClick="setDate"`** |
| `gender` | `RadioGroup` | — | horizontal, background `@color/home_bg`, **nothing pre-selected** |
| `male` | `RadioButton` | **"Male"** | `layout_weight 0.3`, `buttonTint @color/black` |
| `female` | `RadioButton` | **"Female"** | `layout_weight 0.3`, `buttonTint @color/black` |
| `terms_conditions` | `TextView` | **"Terms and conditions"** | `@color/primary_blue`, `15dp` |
| `register_btn` | `Button` | **"Sign Up"** | full width, `backgroundTint @color/primary_blue`, text `@color/always_white` |
| `signin_btn` | `Button` | **"Sign in"** | full width, `backgroundTint @color/white`, text `@color/primary_blue` |

### Static text (no id)

| Text | Style |
|---|---|
| "Sign Up" | `28dp`, bold, `@color/black` |
| "Welcome to SalvationLamb. Please create your account" | `15dp`, `@color/black` |
| "By clicking the signup you agree to our" | `15dp`, `@color/black` — the sentence continues into the `terms_conditions` link |

### Commented-out in the layout (do not re-enable without asking the customer)

- An "About" link in the header.
- A "Hello Everyone" heading.
- A third gender `RadioButton` **"Other"** (`@+id/other`) — so only Male/Female are selectable.

### Not on this screen (unlike Edit Profile)

No profile picture, address, country/state/city, church name, religion, language or warrior fields.
Those are collected later by `EditProfileActivity` (PROFILE team) — which is why the server is told
`isFreshUser: true`.

---

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| `register_btn` click | Snapshots all 7 inputs into fields (`fNameTxt`, `lNameTxt`, `emailTxt`, `mobileTxt`, `passwordTxt`, `genderId = gender.checkedRadioButtonId`, `dobTxt`), then runs `doValidation()`. On `"success"` builds the body and calls `register(data)`; otherwise toasts the validation message. |
| `signin_btn` click | `startActivity(Intent(this, LoginActivity::class.java))` — **no `finish()`** (issue R5) |
| `terms_conditions` click | `WebViewActivity` with extra `WebPageName = "terms"`. **No Privacy Policy link here** (Login has one). |
| `date` click | XML `android:onClick="setDate"` -> `setDate(view: View?)` -> `showDialog(999)` -> `onCreateDialog(999)` |
| Date picked | `myDateListener` writes `"$day-${month + 1}-$year"` into `date.text` (format **`d-M-yyyy`**, not zero-padded) |

### Date picker details

| Item | Value |
|---|---|
| Dialog | `DatePickerDialog(this, android.R.style.Theme_Holo_Dialog_MinWidth, myDateListener, year, month, day)` |
| Initial date | `year = 1960` (`val`), `month = 0`, `day = 1` -> opens on **1 Jan 1960** every time, even after a date is chosen (issue R6) |
| `maxDate` | `System.currentTimeMillis() - 1000` -> future dates blocked, but **no minimum age check** |
| APIs used | `showDialog(int)` / `onCreateDialog(int)` — **deprecated since API 13** (issue R7) |

---

## 4. Validation rules

`doValidation(): String` returns `"success"` (the private `SUCCESS` constant) or the **first** error
message. It is an `if / else if` chain, so only one problem is reported at a time, and each branch
also sets `.error` on the offending view (unlike Login, which only toasts).

| Order | Field | Condition | View `.error` text | Toast / return value |
|---|---|---|---|---|
| 1 | `fname` | `TextUtils.isEmpty(fNameTxt.trim())` | `Enter name` | **`Enter first name`** (mismatched, issue R8) |
| 2 | `fname` | `!Util.isValidName(fNameTxt)` — `^[a-zA-Z\s]+\.?$` | `Enter valid name` | `Enter valid name` |
| 3 | `email` | `TextUtils.isEmpty(emailTxt.trim())` | `Enter email` | `Enter email` |
| 4 | `email` | `!Util.isValidEmail(emailTxt)` | `Invalid Email` | `Invalid Email` |
| 5 | `mobile` | `TextUtils.isEmpty(mobileTxt.trim())` | `Enter mobile number` | `Enter mobile number` |
| 6 | `mobile` | `!Util.isValidMobile(mobileTxt.trim())` — optional `+country` + 10 digits | `Enter valid mobile number` | `Enter valid mobile number` |
| 7 | `password` | `TextUtils.isEmpty(passwordTxt.trim())` | `Enter Password` | `Enter Password` |
| 8 | `password` | `!Util.isValidPassword(passwordTxt)` | `Password must contain 1 capital, 1 small, 1 number, 1 spl char and length greater than 8` | same string |
| 9 | `date` | `isEmpty(dobTxt.trim())` **or** `dobTxt.equals("Date of birth", true)` | `Select Date of birth` | `Select Date of birth` |
| 10 | `gender` | `genderId == -1` | *(none — no view error)* | `Select Gender` |
| — | | all pass | — | `success` -> `register(data)` |

**Notable gaps**
- **`lname` (Last Name) is never validated** — it can be empty or contain digits, and it is still sent
  and concatenated into `name` (issue R9).
- No confirm-password field, so a typo in the password locks the user out until they reset it.
- No terms-acceptance checkbox — agreement is implied by the button text.
- `doValidation()` is **called twice** on the failure path (once in the `if`, once inside
  `Toast.makeText(...)`), so every `.error` is set twice (issue R10).

---

## 5. API contract

### Create account — `register(data: JsonObject)`

| Item | Value |
|---|---|
| Retrofit method | `RetrofitAPI.postCall(url = "users", dataModal = data)` |
| HTTP | `POST api/v1/users` |
| Base URL source | `Util.getRetrofit()` -> `https://server.salvationlamb.com` |
| Headers | none (unauthenticated) |
| Guard | `try/catch` + `Commons().isNetworkAvailable(this)` |
| Progress | `dialog.show()` (ProgressDialog, message "Please Wait", non-cancelable) |

**Request body** (10 properties, in insertion order)

| Key | Value |
|---|---|
| `firstName` | `fname` text (untrimmed) |
| `lastName` | `lname` text (untrimmed, unvalidated) |
| `name` | `"$fNameTxt $lNameTxt"` — server-side display name built on the client |
| `email` | `email` text |
| `mobile` | `mobile` text |
| `gender` | `findViewById<RadioButton>(genderId).text.toString().toLowerCase()` -> `"male"` / `"female"` (deprecated `toLowerCase()`, locale-dependent — issue R11) |
| `password` | plain text |
| `dateOfBirth` | `Util.formatDate(dobTxt, toPattern = "MM-dd-yyyy", fromPattern = "dd-MM-yyyy")` — reformatted from the picker's `d-M-yyyy` to **`MM-dd-yyyy`** (issue R12) |
| `isWarrior` | `false` (Boolean literal) |
| `isFreshUser` | `true` (Boolean literal) — forces the profile-completion flow later |

> Note: `isWarrior`/`isFreshUser` are sent as real JSON **booleans** here, while `UserRslt` reads them
> back as **Strings**. That asymmetry is owned by PLATFORM/DATA_MODELS.

**Success (`response.code() == 200`)**
1. `Gson().fromJson(resp.get("result"), UserRslt::class.java)` -> assigned to **`Util.user`**.
2. Navigates to `ForgotPasswordActivity` with `page = "verify"` and `email = emailTxt`.
3. **No `finish()`**, **no token/userId saved**, `Util.userId` stays unset — which is exactly why the
   OTP screen later routes a newly verified user to **Login** rather than straight into the app
   (`ForgotPasswordActivity.checkOtp` branches on `Util.userId.isNullOrEmpty()`). Registration
   therefore always ends in a manual login. Intentional today — do not "fix" without PM sign-off.

**Error (any non-200)**
- Parses `response.errorBody()` into a `JsonObject`; reads `status` and `errorMessage` with
  `.get(...).toString()` (**values keep their JSON quotes**).
- `Log.e("Status", status)`, `Log.e("result", errorMessage)`.
- Toasts the raw `errorMessage` (quotes included) — this is how "email already exists" reaches the user.
- Unlike Login, there is **no** `contains(...)` mapping, so the server's wording is shown verbatim.

**`onFailure`** — `dialog.dismiss()` + `Log.e("REGISTER", "fail")`. **No user feedback.**

`dialog.dismiss()` also runs at the end of `onResponse` for both branches.

---

## 6. Storage read / written

| Key | Operation |
|---|---|
| — | **none** |

`userPreferences = UserPreferences(this@RegisterActivity)` is assigned inside `register()` but is
**never read or written** — dead code (issue R13). No token, no `userId`, no `isFirst` is persisted by
this screen; the session is only created later by `LoginActivity`.

---

## 7. Global state touched (`com.veha.util.Util`)

| Field | When | Value |
|---|---|---|
| `Util.user` | registration 200 | the parsed `UserRslt` — set even though the user is **not** signed in and has **no token** (issue R14) |

Read-only use: `Util.isValidName`, `Util.isValidEmail`, `Util.isValidMobile`, `Util.isValidPassword`,
`Util.formatDate`, `Util.getRetrofit()`.

---

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| First name empty | `Enter first name` (field error: `Enter name`) | Toast + `TextInputEditText.error` |
| First name invalid | `Enter valid name` | Toast + field error |
| Email empty | `Enter email` | Toast + field error |
| Email invalid | `Invalid Email` | Toast + field error |
| Mobile empty | `Enter mobile number` | Toast + field error |
| Mobile invalid | `Enter valid mobile number` | Toast + field error |
| Password empty | `Enter Password` | Toast + field error |
| Password weak | `Password must contain 1 capital, 1 small, 1 number, 1 spl char and length greater than 8` | Toast + field error |
| Date not chosen | `Select Date of birth` | Toast + `TextView.error` |
| Gender not chosen | `Select Gender` | Toast only |
| Call in flight | `Please Wait` | ProgressDialog (non-cancelable) |
| Server rejects (e.g. duplicate email) | the server's `errorMessage`, **with JSON quotes** | Toast |
| Network failure / offline | **(none)** | — silent (issues R15, R16) |

All strings are hard-coded in Kotlin/XML; none are in `res/values/strings.xml`.

---

## 9. Navigation map

| Trigger | Destination | Extras | `finish()`? |
|---|---|---|---|
| `signin_btn` | `LoginActivity` | — | no |
| `terms_conditions` | `WebViewActivity` | `WebPageName="terms"` | no |
| Registration 200 | `ForgotPasswordActivity` (**verify** mode) | `page="verify"`, `email=<typed email>` | no |
| Registration error / failure | stays on this screen | — | — |

**Back button:** not overridden here — Back returns to `LoginActivity` (the only entry point).
Because neither `signin_btn` nor the success path calls `finish()`, repeatedly tapping
"Sign in" / "Sign Up" stacks Login -> Register -> Login -> Register in the same task.
Note the **destination** `ForgotPasswordActivity` *does* override `onBackPressed()` to force
`LoginActivity`, so Back from the OTP screen will not return here.

---

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | `setContentView(R.layout.activity_register)` -> build `ProgressDialog` ("Please Wait", non-cancelable) -> wire `register_btn`, `signin_btn`, `terms_conditions`. The `date` click comes from XML `onClick`. |
| `onCreateDialog(id)` | Returns the `DatePickerDialog` for id `999`, else `null` (deprecated API) |
| `onDestroy` | `super.onDestroy()` then unconditional `dialog.dismiss()`. The Retrofit call is **not** cancelled — a late callback can touch a destroyed activity (issue R17). |

No rotation handling: the activity is recreated, every typed value is **kept** by the `EditText`s
(they save their own state), but the chosen date in `date` is a `TextView` **without**
`freezesText`, so **the picked date is lost on rotation** (issue R18). An in-flight request is
leaked and its dialog disappears.

All 7 snapshot fields are `lateinit var` / `var` and are only assigned inside the `register_btn`
listener. `emailTxt` is also read inside the success callback — safe only because the callback can
only run after the button was pressed.

---

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `Util.getRetrofit()`, `RetrofitAPI.postCall` | PLATFORM / NETWORK | the registration call |
| `UserRslt` | PLATFORM / DATA_MODELS | success parsing |
| `Util.isValidName/Email/Mobile/Password` | PLATFORM / COMMONS | validation (shared with LOGIN, EDIT_PROFILE) |
| `Util.formatDate` | PLATFORM / COMMONS | DOB reformatting (throws `ParseException`) |
| `Util.user` | PLATFORM / COMMONS | global user cache |
| `Commons().isNetworkAvailable` | PLATFORM / COMMONS | connectivity guard |
| `UserPreferences` | PLATFORM / STORAGE | imported & instantiated but unused |
| `activity_edit_profile.xml` ids | PROFILE / EDIT_PROFILE | **accidental** synthetic-import coupling (issue R2) |
| `LoginActivity`, `ForgotPasswordActivity` | AUTH (siblings) | navigation targets |
| `WebViewActivity` | MEDIA / WEBVIEW | terms page |

---

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| R1 | `android:exported="true"` with no intent-filter — any app can launch the sign-up screen | Medium (security) | set `exported="false"` (PLATFORM/BUILD_CONFIG) |
| R2 | Both `activity_register.*` **and** `activity_edit_profile.*` synthetics are wildcard-imported; the two layouts share **14 ids** (`fname`, `lname`, `email`, `mobile`, `gender`, `date`, `male`, `female`, `prod_logo`, `*_op`, `other`), patched with 6 explicit imports. Adding an id to **either** layout can silently rebind a view here | High | drop the `activity_edit_profile.*` import; longer term migrate this screen to ViewBinding (needs PM sign-off) |
| R3 | Uses deprecated `ProgressDialog`; the rest of AUTH uses `SpotsDialog` | Low | switch to `SpotsDialog` for consistency |
| R4 | `email` field has no `inputType="textEmailAddress"`; no `imeOptions` chain across the form | Medium | add `inputType` + `imeOptions="actionNext"` |
| R5 | `signin_btn` and the success path never call `finish()` -> Login/Register stack grows | Medium | `finish()` after `startActivity` |
| R6 | The date picker always reopens at **1 Jan 1960**; `year` is a `val` and `month`/`day` are never updated from the selection | Low | keep the chosen date in the fields and seed the picker with it |
| R7 | `showDialog(999)` / `onCreateDialog(int)` are deprecated | Low | build the `DatePickerDialog` directly in `setDate`, or use `MaterialDatePicker` |
| R8 | Empty first name sets the field error to "Enter name" but toasts "Enter first name" | Low | use one string |
| R9 | **Last Name is never validated** — may be empty or contain digits, and is concatenated into `name` | Medium | add empty + `isValidName` checks |
| R10 | `doValidation()` runs twice on failure (side effects applied twice) | Low | store the result in a local `val` |
| R11 | `toLowerCase()` without a locale (deprecated) on the gender label | Low | `lowercase(Locale.ROOT)`, or better: map the radio **id** to a value instead of its label |
| R12 | DOB travels through three formats: picker writes `d-M-yyyy`, `formatDate` parses `dd-MM-yyyy` and emits `MM-dd-yyyy`. `SimpleDateFormat` is lenient so `1-5-1990` parses, but the chain is fragile and locale-dependent | Medium | keep a `Calendar`/epoch value and format once at send time |
| R13 | `userPreferences` is instantiated and never used | Low | delete |
| R14 | `Util.user` is populated for a user who is not signed in and has no token | Medium | do not set global user state until login succeeds |
| R15 | `onFailure` gives no user feedback | High (UX) | toast a generic failure message |
| R16 | Offline -> the method returns silently; no dialog, no toast | Medium | toast "No internet connection" |
| R17 | Retrofit call is never cancelled in `onDestroy` | High | keep the `Call` and `cancel()` it, or guard with `isFinishing` |
| R18 | The picked date lives only in `date.text` (a `TextView` without `freezesText`) -> lost on rotation | Medium | add `android:freezesText="true"` or hold the value in a field |
| R19 | `errorMessage` is toasted with its JSON quotes | Low | use `asString` instead of `toString()` |
| R20 | No confirm-password field and no terms checkbox | Medium (product) | ask the customer before adding |
| R21 | Password sent in plain JSON; the app globally sets `usesCleartextTraffic="true"` | Medium (security) | PLATFORM/BUILD_CONFIG to restrict cleartext |
| R22 | `tools:context` points at `LoginActivity` | Cosmetic | correct to `RegisterActivity` |
| R23 | `android:onClick="setDate"` breaks under obfuscation (`minifyEnabled` is currently `false`) | Low | wire the listener in Kotlin, or keep a proguard rule |

---

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/RegisterActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_register.xml` | **exclusive** |
| `<activity android:name=".RegisterActivity">` block in `app/src/main/AndroidManifest.xml` | shared with PLATFORM/BUILD_CONFIG — notify lead |

**Not owned (escalate):** `RetrofitAPI.kt`, `DataModels.kt`, `UserPreferences.kt`, `Util.java`,
`Commons.kt`, `activity_edit_profile.xml` (PROFILE), `activity_forgot_password.xml` (AUTH siblings),
`colors.xml` / `styles.xml` / `rounded_border_login_register.xml`, and every other screen.

---

## 14. How to make common changes

**Add a field to the form (e.g. "Confirm Password", church name):**
1. Add a `TextInputLayout` + `TextInputEditText` to `activity_register.xml`.
   **Check the new id does not already exist in `activity_edit_profile.xml`** (issue R2) — if it does,
   add an explicit `import kotlinx.android.synthetic.main.activity_register.<id>` line.
2. Snapshot it into a field in the `register_btn` listener.
3. Add a branch to `doValidation()` **in the right position** (the chain reports only the first error),
   setting both the view `.error` and the returned message.
4. If it must reach the server, add a `data.addProperty(...)` — and confirm the key with
   PLATFORM/NETWORK first; a new response field is a PLATFORM/DATA_MODELS task via PM.
5. Update §2, §4, §5 and §8 of this doc.

**Re-enable the "Other" gender option:** uncomment the `@+id/other` `RadioButton` in the layout; no
Kotlin change is needed because the value comes from `findViewById(genderId).text`. Confirm with
PLATFORM/NETWORK that the backend accepts `"other"`, and fix R11 while you are there.

**Change the date format sent to the server:** only edit the `Util.formatDate(...)` patterns in the
body builder; `Util.formatDate` itself is PLATFORM/COMMONS. Keep `myDateListener`'s display format
and the parse pattern in sync (R12).

**Change a validation message:** edit the matching `doValidation()` branch and update §4 + §8 here.

**Add a Privacy Policy link (to match Login):** add a `TextView` next to `terms_conditions` and start
`WebViewActivity` with `WebPageName = "privacy"`; the page itself belongs to MEDIA/WEBVIEW.

**Change where a new account goes after sign-up:** the current flow (verify screen -> Login) depends
on `Util.userId` being empty, and the decision lives in `ForgotPasswordActivity.checkOtp`
(`OTP_VERIFY.md`). Escalate to PM — this is a two-agent change.

---

## 15. Change log

| Change | Detail |
|---|---|
| Created | Initial REGISTER module agent, documented line-by-line from `RegisterActivity.kt` (202 lines) and `activity_register.xml` (256 lines): 18 view ids, 5 actions, the date-picker flow, a 10-step validation chain, 1 API call with a 10-key body, 1 global write, 13 user-visible strings, 4 navigation edges, 23 known issues. Verified the 14-id synthetic clash with `activity_edit_profile.xml`. |
