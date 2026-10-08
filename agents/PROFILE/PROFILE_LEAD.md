# PROFILE TEAM LEAD Agent

> Reports to: **PROJECT MANAGER** (`AGENTS.md`)
> Manages the 4 module agents below. **Status: all READY.**

---

## 1. Charter

PROFILE owns **the user's identity and social graph**: viewing your own profile, viewing someone
else's, editing your details, and the followers/following lists.

It also owns the app's **largest single file**, `EditProfileActivity` (738 lines), which combines a
14-field form, a cascading country/state/city picker, and an image-crop pipeline.

**Boundary:** PROFILE owns the profile *chrome* — avatar, name, counts, the form. The **post list**
inside a profile is rendered by FEED's `HomeAdapter`, and the warrior dialog lives in
PLATFORM/COMMONS. PROFILE does not own either.

---

## 2. Modules owned

| Module agent | Screen / unit | Source files | Status |
|---|---|---|---|
| `MY_PROFILE.md` | Own profile tab | `fragments/ProfileFragment.kt` (469), `res/layout/fragment_profile.xml` (12 ids) | READY |
| `VIEW_PROFILE.md` | Another user's profile | `activity/ViewProfileActivity.kt` (112), `res/layout/activity_view_profile.xml` | READY |
| `EDIT_PROFILE.md` | Edit details | `activity/EditProfileActivity.kt` (**738 — largest in the app**), `res/layout/activity_edit_profile.xml` (31 ids) | READY |
| `FOLLOWERS.md` | Followers / following | `activity/FollowerActivity.kt` (254), `adapter/FollowAdapter.kt`, `res/layout/activity_follower.xml`, `child_follow.xml` | READY |

> **`UsersAdapter` is NOT a PROFILE file.** Despite living next to `FollowAdapter`, its only call
> site is `SearchProfileFragment` — it belongs to **SEARCH**. Verified.

---

## 3. The two multiplexers PROFILE depends on

### `ProfileFragment.getInstance(userId, who)` — 2 callers

| `who` | Launched by | Effect |
|---|---|---|
| `"me"` | `TabAdapter` with `Util.userId` | Edit-profile button **visible**; adapter page `"profile"` (delete buttons on) |
| `"other"` | `ViewProfileActivity` with the target `userId` | Edit button **GONE**; adapter page `"OtherProfile"` (no actions) |

So **one fragment serves both profile screens**, and `MY_PROFILE.md` owns it while
`VIEW_PROFILE.md` owns the activity that hosts it in `"other"` mode. Both agents must be consulted
before editing `ProfileFragment.kt`.

### `FollowerActivity` — `page` extra

| `page` | Endpoint | Shows |
|---|---|---|
| `"follower"` | `GET follows/user/{userId}` | people following this user |
| `"following"` | `GET follows/{userId}` | people this user follows |

One activity, two lists, selected by a string — and the two endpoints are easy to confuse
(NETWORK N6).

---

## 4. Intra-team flows

```
MainActivity bottom nav [APPSHELL]
   `-> ProfileFragment(Util.userId, "me")
          |-- avatar tap -----------> ImageDetailActivity [FEED]
          |-- "Edit Profile" -------> AboutActivity [APPSHELL]  <-- not EditProfile! (F-2)
          |-- followers count ------> FollowerActivity (page="follower", userId)
          |-- following count ------> FollowerActivity (page="following", userId)
          `-- post list ------------> HomeAdapter(page="profile") [FEED]

HomeAdapter author tap [FEED] / ViewLikesAdapter row tap [FEED]
   `-> ViewProfileActivity (extra userId)
          `-> ProfileFragment(userId, "other")
                 `-- post list -----> HomeAdapter(page="OtherProfile") [FEED]

overflow menu "edit_profile" (6+ screens)
   `-> EditProfileActivity
          |-- avatar -> gallery/camera -> CropImage -> POST Users/image/{userId}
          `-- Save -> PUT users/{userId}

FollowerActivity row tap -> ViewProfileActivity
```

> **Oddity:** the `edit_profile` button inside `ProfileFragment` opens **`AboutActivity`**
> (APPSHELL), not `EditProfileActivity`. `AboutActivity` then offers its own "Edit Profile" row.
> Verified in the source — a two-step detour (team issue F-2).

---

## 5. API surface used by this team

| Endpoint | Retrofit method | Used by |
|---|---|---|
| `GET api/v1/users/{userId}` | `getUser` | MY_PROFILE, EDIT_PROFILE |
| `GET api/v1/post/user/{userId}?page&size` | `getMyPosts` | MY_PROFILE (paged) |
| `GET api/v1/like/user/{userId}` | `getUserLikes` | MY_PROFILE |
| `GET api/v1/follows/user/{userId}` | `getFollowers` | MY_PROFILE, FOLLOWERS |
| `GET api/v1/follows/{userId}` | `getFollowing` | MY_PROFILE, FOLLOWERS |
| `POST api/v1/follows` | `postFollow` | FOLLOWERS, FEED's adapter |
| `PUT api/v1/users/{userId}` | `putUser` | EDIT_PROFILE |
| `POST api/v1/Users/image/{userId}` | `postProfilePic` | EDIT_PROFILE |
| `GET api/v1/Country` | `getCountries` | EDIT_PROFILE (no auth) |
| `GET api/v1/state/{countryID}` | `getState` | EDIT_PROFILE (no auth) |
| `GET api/v1/city/{stateId}` | `getCity` | EDIT_PROFILE (no auth) |
| `POST api/v1/users/warrior` | `postWarrior` | via `Commons().makeWarrior` |

List responses use **`results`** + `count`; single objects use **`result`** (see FEED_LEAD §5).

---

## 6. Shared state

| State | Who | Note |
|---|---|---|
| `Util.user` | written by MY_PROFILE and EDIT_PROFILE | the **5th and 6th** copies of `getMyDetails` (AUTH A4) |
| `Util.userId` | read everywhere | the "me" identity |
| `Util.isWarrior`, `Util.user.isReviewState` | read | warrior menu gating |
| `Util.WARRIOR` | read | the " - Warrior" name suffix |
| DataStore `token` | read per call | always via a fresh observer |

---

## 7. Cross-team dependencies

| Needs | Owner | Escalate when |
|---|---|---|
| `HomeAdapter` / `child_post.xml` (the post list in every profile) | **FEED / HOME_FEED** | always — PROFILE is a *consumer*, not the owner |
| `ImageDetailActivity` + the `profilePic` extra | FEED / IMAGE_DETAIL | when the extra changes |
| `AboutActivity` (the real edit-profile entry) | APPSHELL / ABOUT | always |
| `main_menu.xml` + the duplicated overflow handler | APPSHELL / THEMING | always |
| `Commons().makeWarrior` | PLATFORM / COMMONS | always (its return value is broken — C-12) |
| any endpoint or `UserRslt` field | PLATFORM / NETWORK, DATA_MODELS | always |
| `android-image-cropper` (`CropImageActivity`) | PLATFORM / BUILD_CONFIG | always |
| `SearchProfileFragment` / `UsersAdapter` | SEARCH | PROFILE does **not** own these |

---

## 8. Definition of done (team-level)

- [ ] Changes to `ProfileFragment` verified in **both** `who = "me"` and `who = "other"` modes.
- [ ] Changes to `FollowerActivity` verified for **both** `page` values.
- [ ] If the profile post list is involved, confirm whether the fix belongs to **FEED/HOME_FEED**.
- [ ] `results` vs `result` envelope respected.
- [ ] `Commons().isNetworkAvailable` guard kept; dialog dismissed on every path.
- [ ] Module `.md` updated in the same change, including its Change log.

---

## 9. Team-level known issues

| # | Issue | Module | Risk |
|---|---|---|---|
| F-1 | `ProfileFragment` serves two screens via `who`, with the adapter page derived as `if (who == "me") "profile" else "OtherProfile"` — no constants | MY_PROFILE, VIEW_PROFILE | silent mis-render |
| F-2 | The in-profile "Edit Profile" button opens **`AboutActivity`**, not `EditProfileActivity` | MY_PROFILE | confusing navigation |
| F-3 | `ProfileFragment.setUserVisibleHint` calls `detach().attach()` on **every** tab selection — a full recreate plus re-fetch, using deprecated APIs | MY_PROFILE | jank, duplicate calls |
| F-4 | `EditProfileActivity` is **738 lines** mixing a 14-field form, 3 cascading spinners, crop/camera and 4 API calls | EDIT_PROFILE | unmaintainable |
| F-5 | `MY_PROFILE` passes **two empty `HashMap()`s** for follow/fav state, so those states never render in profiles | MY_PROFILE | wrong UI state |
| F-6 | The always-true `||` token guard appears in every PROFILE network method | all | dead logout branches |
| F-7 | `Util.user.isReviewState.toBoolean()` is unguarded in the duplicated overflow menus | FOLLOWERS, EDIT_PROFILE | NPE after process death |
| F-8 | `getmyDetails` here is the 5th/6th copy of the same block | MY_PROFILE, EDIT_PROFILE | drift |
| F-9 | `intent.extras!!.get(...)` and `arguments?.get(...).toString()` yield the literal `"null"` for missing values | all | malformed requests |
| F-10 | The overflow-menu block is duplicated here too (FEED VL3) | FOLLOWERS, EDIT_PROFILE, VIEW_PROFILE | 6+ copies app-wide |
| F-11 | **Follow state is unreliable app-wide**: `MY_PROFILE` passes empty maps to `HomeAdapter` (MP4) while `FollowAdapter` consults its map with the **wrong key** (FL7), so every row shows the same follow label | MY_PROFILE, FOLLOWERS + **FEED** | one PM-coordinated fix |
| F-12 | `EditProfileActivity` **disables StrictMode's VM policy** to pass a `file://` URI to the cropper | EDIT_PROFILE | security / correctness |
| F-13 | `activity_edit_profile.xml` shares **14 ids** with `activity_register.xml`, coupling PROFILE to AUTH's synthetic imports | EDIT_PROFILE | cross-team breakage |
| F-14 | `FollowerActivity` starts loading **before** binding its views, risking `UninitializedPropertyAccessException` | FOLLOWERS | crash |
| F-15 | `ViewProfileActivity` force-unwraps `userId!!`, so launching it without the extra crashes | VIEW_PROFILE | crash |

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial PROFILE team lead agent: charter, 4 modules, both multiplexers (`who` for `ProfileFragment`, `page` for `FollowerActivity`), flows, a 12-endpoint API surface, shared state, cross-team rules (the post list belongs to FEED; `UsersAdapter` belongs to SEARCH), and 10 team-level issues. Verified that the in-profile "Edit Profile" button actually opens `AboutActivity`. |
| PROFILE team complete | `MY_PROFILE.md`, `VIEW_PROFILE.md`, `EDIT_PROFILE.md` and `FOLLOWERS.md` written and verified. Added team issues F-11 (app-wide follow-state bug spanning PROFILE + FEED), F-12 (StrictMode disabled for the cropper), F-13 (14-id layout collision with Register), F-14 and F-15 (two crash risks). **All 4 PROFILE module agents are READY.** |


