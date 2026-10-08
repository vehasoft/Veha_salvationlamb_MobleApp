# ABOUT Module Agent

> Team: **APPSHELL** · Reports to: `agents/APPSHELL/APPSHELL_LEAD.md`
> This agent knows **every detail** of the About screen (a read-only view of your own details)
> **and** of `ExpandableView` — a custom `TextView` used app-wide by the feed.
> It may only edit the files listed in §10 **Owned files**.

---

## ⚠ Correction: `ExpandableView` is **not** an About screen component

The team directory originally paired `ExpandableView.java` with the About screen. Verified in the
source: it is **not referenced by `activity_about.xml` or `AboutActivity.kt` at all**. Its only
usage is:

```kotlin
// HomeAdapter.kt:59  [FEED / HOME_FEED]
val content: ExpandableView = view.findViewById(R.id.post_content)
```

So it is the **post-body text view in every feed card** — owned here (it has no better home, and it
is a shared UI primitive), but **consumed exclusively by FEED**. Any change to it is a cross-team
change (issue AB1).

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **About** (your own details) |
| Classes | `com.veha.activity.AboutActivity` (152 lines) + `com.veha.activity.ExpandableView` (62 lines, **Java**) |
| Layout | `app/src/main/res/layout/activity_about.xml` (11 ids) |
| Manifest | `<activity android:name=".AboutActivity" android:exported="false"/>` |
| View access | `findViewById` |
| Required extras | **none** |
| Entered from | **`ProfileFragment`'s "Edit Profile" button** (PROFILE F-2) — the only caller |
| Exits to | `EditProfileActivity` [PROFILE], `ChangePasswordActivity` [AUTH] |

> Despite the name, this is **not** an "about the app" screen — it shows **the signed-in user's own
> details** and acts as a staging page before editing them (issue AB2).

---

## 2. UI inventory (`activity_about.xml`)

| View id | Type | Bound to |
|---|---|---|
| `header_main` | container | header bar |
| `prodName` | `TextView` | the product name |
| `about_name` | `TextView` | `UserRslt.name` |
| `about_dob` | `TextView` | `dateOfBirth` |
| `about_phone` | `TextView` | `mobile` |
| `about_gender` | `TextView` | `gender` |
| `about_address` | `TextView` | `address` |
| `about_email` | `TextView` | `email` |
| `about_join` | `TextView` | `createdAt` ("joined") |
| `edit` | control | -> `EditProfileActivity` |
| `change_pwd_btn` | control | -> `ChangePasswordActivity` (**no extras** = signed-in mode) |

**Seven read-only fields and two actions.** No avatar, no warrior status, no country/state/city —
a strict subset of what `EditProfileActivity` shows (issue AB3).

---

## 3. Behaviour

`onCreate` binds the nine views, calls `getmyDetails()`, and wires the two buttons.

| Trigger | Destination |
|---|---|
| `edit` | `EditProfileActivity` [PROFILE/EDIT_PROFILE] |
| `change_pwd_btn` | `ChangePasswordActivity` with **no extras** -> signed-in mode [AUTH/CHANGE_PASSWORD] |

### The navigation detour

```
ProfileFragment "Edit Profile"  ->  AboutActivity  ->  EditProfileActivity
```

The user taps "Edit Profile" and lands on a **read-only** screen with another "Edit" button. The
intent appears to be "review, then edit", but the first button's label makes it look like a bug
(PROFILE F-2, issue AB4).

---

## 4. API contract — `getmyDetails()`

| Item | Value |
|---|---|
| Retrofit | `getUser("Bearer $token", Util.userId)` |
| HTTP | `GET api/v1/users/{userId}` |
| Envelope | `result` -> `UserRslt` |
| Guard | `isNetworkAvailable` + a fresh `authToken` observer |
| Token guard | the **always-true `||`** variant (issue AB5) |

This is **copy #9** of the `getMyDetails` block (AUTH A4) — the most duplicated logic in the app:
SPLASH, LOGIN, MAIN_NAV, HOME_FEED, MY_PROFILE, EDIT_PROFILE, ADMIN_AUDIO, ADMIN_VIDEO and here.

**Non-200** — no user-facing message (issue AB6). **`onFailure`** — logs only.

---

## 5. `ExpandableView` (the shared primitive)

```java
public class ExpandableView extends TextView implements View.OnClickListener {
    public void setText(String text) { ... }     // stores the full text, shows a truncated form
    @Override public void onClick(View v) { ... }
    public void expand() { ... }
}
```

| Fact | Detail |
|---|---|
| Base | extends `TextView`, implements `OnClickListener` |
| Purpose | collapsed/expanded long text — the **post body** in `child_post.xml` |
| Consumer | **only** `HomeAdapter` (`R.id.post_content`), which also calls `content.expand()` on tap |
| Language | **Java** — one of only three Java files in the app |
| Constructors | all three view constructors are provided, so it can be inflated from XML |
| Not used here | `activity_about.xml` does **not** use it |

Because FEED calls `expand()` directly and applies `Util.fontSize` to the same view, **its public
API is effectively a FEED contract** even though the file sits in APPSHELL's scope (issue AB1).

---

## 6. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read (observed) |
| DataStore `token` + `userId` | deleted in the unreachable logout branch |
| `Util.userId` | read |
| `Util.user` | **not** written here, unlike the other `getMyDetails` copies (issue AB7) |

---

## 7. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | layout -> bind 9 views -> `getmyDetails()` -> wire `edit` and `change_pwd_btn` |
| `onPause` / `onResume` / `onDestroy` | `dialog.dismiss()` |

No rotation handling; the details are re-fetched. The Retrofit call is never cancelled (issue AB8).

---

## 8. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `RetrofitAPI.getUser` | PLATFORM / NETWORK | the fetch |
| `UserRslt` | PLATFORM / DATA_MODELS | parsing |
| `UserPreferences` | PLATFORM / STORAGE | token |
| `Util.userId`, `Commons()` | PLATFORM / COMMONS | id + guard |
| `ProfileFragment` | **PROFILE / MY_PROFILE** | the only entry point |
| `EditProfileActivity` | PROFILE / EDIT_PROFILE | the edit action |
| `ChangePasswordActivity` | AUTH / CHANGE_PASSWORD | the password action |
| **`HomeAdapter`** | **FEED / HOME_FEED** | the only consumer of `ExpandableView` |

---

## 9. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| AB5 | The always-true `||` token guard; the logout branch is dead | **High** | use `&&` |
| AB6 | A non-200 shows the user nothing | **High** | toast the error |
| AB8 | The Retrofit call is never cancelled | **High** | cancel in `onDestroy` |
| AB1 | `ExpandableView` lives in APPSHELL but its **only** consumer is FEED's `HomeAdapter` | Medium | documented; consider moving it next to its user |
| AB4 | "Edit Profile" in the profile tab opens this **read-only** screen, which then offers another "Edit" | Medium | relabel, or route straight to `EditProfileActivity` |
| AB2 | The class is named `AboutActivity` but shows user details, not app information | Medium | rename, or add real app info |
| AB3 | Shows only 7 of `UserRslt`'s 28 fields; no avatar or warrior status | Medium | expand, or confirm intent |
| AB7 | Unlike every other `getMyDetails` copy, this one does **not** refresh `Util.user` | Medium | align, or document deliberately |
| AB9 | `getmyDetails` is **copy #9** of the same block | Medium | shared helper (PM-level) |
| AB10 | Dates (`dateOfBirth`, `createdAt`) are shown without `Util.formatDate` formatting | Medium | format consistently |
| AB11 | No app version, licences or credits — what an "About" screen usually has | Low (product) | ask the customer |

---

## 10. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/AboutActivity.kt` | **exclusive** |
| `app/src/main/java/com/veha/activity/ExpandableView.java` | **owned, but shared-use** — FEED's `HomeAdapter` is the only consumer; notify HOME_FEED |
| `app/src/main/res/layout/activity_about.xml` | **exclusive** |
| `<activity android:name=".AboutActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `ProfileFragment.kt` (PROFILE — the caller), `EditProfileActivity.kt`
(PROFILE), `ChangePasswordActivity.kt` (AUTH), `HomeAdapter.kt` / `child_post.xml` (FEED), all
PLATFORM files.

---

## 11. How to make common changes

**Add a field to the About screen:** add a `TextView` to the layout, bind it, and set it from the
`UserRslt` in `getmyDetails`. If the field does not exist on the model, that is a
**PLATFORM/DATA_MODELS** task via PM.

**Change `ExpandableView` (AB1):** its `setText`, `expand()` and click behaviour are used by
**every post card**. Any change is a **FEED-affecting change** — notify `HOME_FEED.md` via PM, and
test with long post bodies at all three font sizes.

**Remove the detour (AB4):** either relabel the profile button, or have `ProfileFragment` open
`EditProfileActivity` directly. That edits a **PROFILE** file — escalate.

**Make it a real About screen (AB2/AB11):** adding version info and licences is a product decision —
ask the customer first; the version would come from `BuildConfig` (BUILD_CONFIG).

---

## 12. Change log

| Change | Detail |
|---|---|
| Created | Initial ABOUT module agent documented from `AboutActivity.kt` (152 lines), `ExpandableView.java` (62 lines) and `activity_about.xml` (11 ids). **Corrected the pairing**: `ExpandableView` is not used by this screen at all — its only consumer is FEED's `HomeAdapter` (`R.id.post_content`). Recorded the 7 read-only fields, the two actions, the `ProfileFragment -> About -> EditProfile` detour, `getMyDetails` **copy #9**, and 11 known issues. |


