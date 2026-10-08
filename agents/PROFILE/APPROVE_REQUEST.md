# APPROVE_REQUEST Module Agent

> Team: **PROFILE** · Reports to: `agents/PROFILE/PROFILE_LEAD.md`
> Scope: this agent knows **every detail** of this screen — views, actions, API, storage,
> validation, messages, navigation. It may only edit the files under **Owned files**.

---

> **This is the app's only moderation screen.** An admin approves or rejects another user's
> profile-change / warrior request here. It is the highest-privilege action in the product, and
> it contains **no permission check of its own** — see `AR1`.

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **Approve request** — a side-by-side diff of a user's current vs requested profile |
| Class | `com.veha.activity.ApproveRequestActivity` (383 LOC) |
| Layout | `res/layout/activity_approve_request.xml` |
| Manifest entry | `<activity android:name=".ApproveRequestActivity" android:exported="false" />` |
| Entered from | **3 places, all `type=warrior` notifications:** `NotificationListAdapter` (in-app tap), `NotificationHelper` (system tray), `SplashScreenActivity` (cold-start FCM deep link). All three gate on `USER`/`Edit`; **this screen does not** (`AR1`) |
| Exits to | `MainActivity` (logo tap, and after approve/reject) · back via `close` |
| Required extra | **`userId: String`** — force-unwrapped as `intent.extras!!.getString("userId").toString()` (`AR5`) |

## 2. UI inventory

A **two-column diff**: for each profile field there is an `ex_*` ("existing") and a `new_*`
("requested") `TextView`, both bound in `onCreate` and both driven by `setValue()` (§3a).

| Pair | `ex_` id | `new_` id | Field |
|---|---|---|---|
| 1 | `ex_name` | `new_name` | display name |
| 2 | `ex_fname` | `new_fname` | first name |
| 3 | `ex_lname` | `new_lname` | last name |
| 4 | `ex_gender` | `new_gender` | gender |
| 5 | `ex_email` | `new_email` | email |
| 6 | `ex_mobile` | `new_mobile` | mobile |
| 7 | `ex_address` | `new_address` | address |
| 8 | `ex_dob` | `new_dob` | date of birth |
| 9 | `ex_city` | `new_city` | city |
| 10 | `ex_country` | `new_country` | country |
| 11 | `ex_pincode` | `new_pincode` | pincode |
| 12 | `ex_language` | `new_language` | mother tongue |
| 13 | `ex_church` | `new_church` | church name |
| 14 | `ex_religion` | `new_religion` | religion |
| 15 | `ex_gift` | `new_gift` | spiritual gift |
| 16 | `ex_warrior` | `new_warrior` | `isWarrior` flag |

Plus:

| View id | Type | Notes |
|---|---|---|
| `@id/ex_pic` / `@id/new_pic` | `ImageView` | existing vs requested avatar |
| `@id/profile_pic` | `ImageView` | header avatar |
| `@id/prod_logo` | `ImageView` | tap → `MainActivity` (**no `finish()`**, `AR8`) |
| `@id/close` | `ImageButton` | `finish()` |
| `@id/approve` | `Button` | `postUpdateRequest("approve", userId)` |
| `@id/reject` | `Button` | `postUpdateRequest("reject", userId)` |

**34 `findViewById` calls in `onCreate`** — all correctly after `setContentView` (unlike the
pre-v1.2.0 `ViewPostActivity` bug).

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| `onCreate` | `setContentView` → 34 `findViewById` → `userId = intent.extras!!.getString("userId").toString()` → `getUpdateRequest(this, userId)` → wire `approve` / `reject` / `logo` / `close` |
| `approve` tap | `postUpdateRequest("approve", userId)` |
| `reject` tap | `postUpdateRequest("reject", userId)` |
| `logo` tap | `startActivity(MainActivity)` without `finish()` (`AR8`) |
| `close` tap | `finish()` |

### 3a. `setValue(existing, new, exString, newString)` — the diff renderer

```kotlin
if (exString == newString) {        // unchanged
    new.visibility = View.GONE
    existing.visibility = View.VISIBLE
    existing.text = newString
} else {                            // changed
    new.visibility = View.VISIBLE
    existing.visibility = View.VISIBLE
    existing.text = exString
    new.text = newString
    new.setTextColor(resources.getColor(R.color.red))   // requested value in red
}
```

Nulls are normalised to `""` first, so a null→value change renders as `"" → value`. The method
shadows both parameters with local `var`s of the same name — legal but confusing (`AR11`).

> **The red colour is the only signal that a field changed.** There is no label, no count, and no
> way to approve fields individually — approval is **all-or-nothing** for the whole request
> (`AR9`).

## 4. Validation rules

| Field | Rule | Failure message |
|---|---|---|
| — | none — the admin only chooses approve or reject | — |
| `result.user` / `result.updateRequest` | both must be non-`JsonNull`, else the request is treated as already handled | `"This request is already handled"` then `finish()` |

> There is **no confirmation dialog** on either button: a single tap is final (`AR6`).

## 5. API contracts

### `getUpdateRequest(context, userId)`

| Item | Value |
|---|---|
| Retrofit method | `getUpdateRequest(head, userId)` |
| HTTP | `GET api/v1/review/{userId}` |
| Base URL source | `Util.getRetrofit()` → `https://server.salvationlamb.com` |
| Headers | `Authorization: Bearer <token>` |
| Request body | none |
| Success (200) | `{"results": {"user": {…}, "updateRequest": {…}}}` — **a `results` object, not an array**, a fifth envelope variant (`AR12`). Both halves are parsed into `ProfileChange` and fed to 16 `setValue` calls + 2 Picasso avatar loads |
| Both halves null | toast `"This request is already handled"` → `finish()` |
| Error | **401** → toast `@string/Deleted_account` + `LoginActivity`, **no `finish()`, no session clear** (`AR4`). Any other code → two `Log.e` calls, **no user feedback** (`AR3`) |
| Failure (`onFailure`) | `Log.e("EditProfileActivity.getMyDetails", "fail")` — **copy-pasted tag from another class**, nothing shown to the user (`AR3`, `AR13`) |
| Guard | uses the **correct `&&`** token guard — not the `CL-1` bug |

### `postUpdateRequest(status, userId)`

| Item | Value |
|---|---|
| Retrofit method | `postUpdateRequest(head, userId, status)` |
| HTTP | **`POST api/v1/review/approve/{userId}`** or **`POST api/v1/review/reject/{userId}`** — the Retrofit declaration is templated as `api/v1/review/{status}/{userId}`, and `status` is passed as the string literal `"approve"` (line 130) or `"reject"` (line 133). Those are the only two values ever used |
| Headers | `Authorization: Bearer <token>` |
| Request body | **none** — the decision is carried entirely in the path |
| Success (200) | `startActivity(MainActivity)` — **no success toast**, so the admin gets no confirmation that the decision registered (`AR7`) |
| Error | 401 → same as above; other codes → logged only |
| Failure | logged only |
| Guard | correct `&&` form |

> The status strings `"approve"` / `"reject"` are **string literals at the two click sites**, not
> an enum, unlike every other typed constant in the app (`AR10`).

## 6. Storage read / written

| Key | Type | Operation | Value |
|---|---|---|---|
| `token` | `String` | read (continuous observer, **one per call**) | `Bearer $it` for both endpoints |
| `token`, `userId` | — | **delete** | only in the `else` of the token guard (reachable here, since this screen uses `&&`) |

## 7. Global state touched

| Field | Operation | Value |
|---|---|---|
| — | — | **none.** This screen reads no `Util` state at all — not even `Util.permissionMap` (`AR1`) |

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| Request already processed | `"This request is already handled"` | `Toast.LENGTH_LONG` + `finish()` — hard-coded (`AR14`) |
| HTTP 401 | `@string/Deleted_account` → `"Your account is removed\n please contact administrator"` | `Toast.LENGTH_LONG` |
| Missing token | `"Somthing Went Wrong \nLogin again to continue"` | `Toast` — hard-coded and **misspelled** (`AR14`) |
| Approve / reject success | **nothing** | — (`AR7`) |
| Any other error, or `onFailure`, or offline | **nothing** | — (`AR3`) |

## 9. Navigation map

| From | Trigger | To | Extras | finish()? |
|---|---|---|---|---|
| notification (3 sources) | `type=warrior` + `USER`/`Edit` | **`ApproveRequestActivity`** | `userId` | varies by source |
| this | `approve` / `reject` 200 | `MainActivity` | none | **no** (`AR8`) |
| this | logo tap | `MainActivity` | none | **no** (`AR8`) |
| this | `close` | — (back) | — | yes |
| this | already-handled request | — (back) | — | yes |
| this | HTTP 401 | `LoginActivity` | none | **no** (`AR4`) |

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | §3 — binds 34 views, reads the extra, fires `getUpdateRequest` |
| `onDestroy` | **not overridden** — the Retrofit calls are never cancelled (`CL-9`), and the two `authToken` observers are never removed |
| rotation | re-runs `onCreate`, re-fetches, and registers **another** observer (`AR2`) |

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `RetrofitAPI.getUpdateRequest`, `postUpdateRequest` | PLATFORM/NETWORK | fetch + decide |
| `ProfileChange` model | PLATFORM/DATA_MODELS | both halves of the diff |
| `UserPreferences` | PLATFORM/STORAGE | token |
| `Commons().isNetworkAvailable` | PLATFORM/COMMONS | offline guard |
| Picasso | 3rd-party | the two avatars |
| `MainActivity` | APPSHELL | post-decision destination |
| `LoginActivity` | AUTH | 401 destination |
| `NotificationListAdapter`, `NotificationHelper` | **NOTIFICATIONS** | 2 of the 3 entry points |
| `SplashScreenActivity` | AUTH | the cold-start deep-link entry point |

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| AR1 | **No permission check inside the screen** — the client relies entirely on its three callers gating `USER`/`Edit`, so a deep link or `adb am start` opens an apparently working approve/reject UI. **Downgraded 2026-10-08: the customer confirmed the backend authorises `POST api/v1/review/approve\|reject/{userId}` server-side**, so this is a **UI-integrity** issue, not a privilege-escalation one — the buttons render but the API refuses. Still worth a gate in RN (defence in depth + the user gets a clear message instead of a silent failure) | Medium | gate in `onCreate` and route to `NoPermissionActivity` |
| AR6 | **No confirmation dialog.** A single tap on Approve or Reject is final and irreversible, on a screen reached straight from a notification tap | **High** | add a confirm dialog naming the user |
| AR7 | **No success feedback.** After a 200 the admin is dropped on `MainActivity` with no toast, so a mis-tap or double-tap is indistinguishable from success | **High** | toast the outcome before navigating |
| AR3 | Non-401 errors, `onFailure` and the offline path show the user **nothing** (`CL-7`) — the admin cannot tell "rejected" from "network failed" | **High** | toast + retry |
| AR4 | HTTP 401 starts `LoginActivity` with **no `finish()` and no session clear**, so back returns to a dead screen and the next launch repeats the 401 | **High** | adopt the `bailToLogin` pattern from `SplashScreenActivity` (T-024) |
| AR2 | Both methods register a **continuous `authToken` LiveData observer** (`CL-8`); a later token write re-fires them and can **re-submit the approval** | **High** | one-shot read (`first()`) |
| AR5 | `intent.extras!!.getString("userId").toString()` double force-unwrap — launching without the extra is an immediate NPE, and a missing key yields the literal string `"null"` | Medium | `intent.getStringExtra("userId") ?: return finish()` |
| AR8 | Logo tap and post-decision navigation both `startActivity(MainActivity)` with **no `finish()` or `CLEAR_TOP`**, stacking duplicate `MainActivity` instances behind this screen | Medium | `CLEAR_TOP` + `finish()` |
| AR9 | Approval is **all-or-nothing**; an admin cannot accept the name change but reject the avatar | Medium | per-field approval, if the API supports it |
| AR10 | `"approve"` / `"reject"` are **string literals** at the click sites, not an enum, unlike every other typed constant in the app | Medium | add a `ReviewStatus` enum to `DataModels.kt` |
| AR12 | The response uses `{"results": {object}}` — `results` holding an **object**, not an array; a fifth envelope shape (`CL-5`) | Medium | normalise server-side, or document |
| AR15 | 34 `findViewById` calls and 16 near-identical `setValue` invocations in one 383-line Activity, with no view binding | Medium | ViewBinding + a loop over a field list |
| AR11 | `setValue` shadows both of its parameters with local `var`s of the same name | Low | rename the locals |
| AR13 | `onFailure` log tag reads `EditProfileActivity.getMyDetails` — copy-pasted from another class | Low | use `<Class>.<method>` |
| AR16 | `getUpdateRequest(context, userId)` takes a `context` parameter it never uses — the body uses `this@ApproveRequestActivity` throughout | Low | drop the parameter |
| AR14 | Hard-coded user-visible strings: `"This request is already handled"` and the misspelled `"Somthing Went Wrong \nLogin again to continue"` | Cosmetic | move to `strings.xml`; fix app-wide |

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/ApproveRequestActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_approve_request.xml` | **exclusive** |
| `<activity android:name=".ApproveRequestActivity">` in `AndroidManifest.xml` | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `RetrofitAPI.kt`, `DataModels.kt` (`ProfileChange`), `UserPreferences.kt`,
`Util.java`, `Commons.kt` (PLATFORM); `MainActivity` (APPSHELL); `LoginActivity` and
`SplashScreenActivity` (AUTH); `NotificationListAdapter` and `NotificationHelper`
(**NOTIFICATIONS** — they own 2 of the 3 entry points and the `USER`/`Edit` gate).

## 14. How to make common changes

**Add the missing permission gate (`AR1`):** add
`if (!Util.hasPermission(PermissionType.USER.value, Permission.EDIT.value)) { … NoPermissionActivity; finish(); return }`
at the top of `onCreate`. Local to this file. **This is UI integrity, not security** — the backend
authorises approve/reject server-side (confirmed 2026-10-08), so an ungated caller already gets
refused by the API; the gate just means the user sees a clear "no permission" message instead of
two buttons that silently fail. Note that since T-025 `hasPermission` fails closed, so a
push-started process with no loaded permission map would be denied — correct here, but coordinate
with NOTIFICATIONS (see `PUSH_SERVICE NP13`).

**Add a confirmation dialog (`AR6`):** wrap both click handlers in an `AlertDialog` naming the
requesting user. Local change, no API impact.

**Add a new reviewable field:** add the `ex_*` / `new_*` pair to the layout, bind both in
`onCreate`, add one `setValue(...)` call, and confirm the server returns the field inside **both**
`user` and `updateRequest`. `ProfileChange` is **PLATFORM/DATA_MODELS** — escalate for the model.

**Change the approve/reject contract:** the status is a **path segment**, not a body field, so
changing the values is a NETWORK change plus both literals here. PM sign-off.

## 15. Change log

| Change | Detail |
|---|---|
| Updated (2026-10-08 20:30) | **Customer confirmed the backend authorises approve/reject server-side.** `AR1` downgraded **High → Medium** and re-framed as a UI-integrity gap rather than privilege escalation: an ungated caller can open the screen, but the API refuses the action. §14 recipe updated to say the same. Also pinned the two concrete endpoints — `POST api/v1/review/approve/{userId}` and `.../reject/{userId}` (`ApproveRequestActivity.kt:130,133`) — in place of the templated `{status}` form. |
| Created (T-026, 2026-10-08) | Documented the app's only moderation screen from `ApproveRequestActivity.kt` (383) + `activity_approve_request.xml`, previously unowned since the v1.2.0 branch. Captured the 16-pair side-by-side diff UI and its 34 view bindings, the `setValue` equal-vs-changed rule (unchanged → hide the `new_` view; changed → show both and paint the requested value red), the `GET api/v1/review/{userId}` + `POST api/v1/review/{status}/{userId}` contract, the already-handled guard, and all 3 entry points. **16 issues** recorded. Headline: `AR1` — **the screen performs no permission check of its own**, delegating entirely to its three callers, leaving the app's highest-privilege action one unguarded call site away from exposure. Also `AR6`/`AR7`: an irreversible approve/reject with **no confirmation and no success feedback**; and `AR2`: the `CL-8` observer pattern can **re-submit the approval** on a later token write. |


