# MY_PROFILE Module Agent

> Team: **PROFILE** · Reports to: `agents/PROFILE/PROFILE_LEAD.md`
> This agent knows **every detail** of `ProfileFragment` — the fragment that renders **both** your
> own profile and other users' profiles.
> It may only edit the files listed in §11 **Owned files**.

---

## ⚠ Shared-component warning

`ProfileFragment` serves **two screens** via the `who` argument:

| `who` | Host | Owning agent |
|---|---|---|
| `"me"` | `TabAdapter` inside `MainActivity` | **this agent** |
| `"other"` | `ViewProfileActivity` | `VIEW_PROFILE.md` (which owns the host activity) |

**This agent owns the file**, but `VIEW_PROFILE.md` depends on it entirely. Any change must be
verified in **both** modes and reported to PM.

Additionally, the **post list is not ours**: it is FEED's `HomeAdapter` + `child_post.xml`. Bugs in
post rendering, likes, delete or share go to **`HOME_FEED.md`**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Profile** |
| Class | `com.veha.fragments.ProfileFragment` (469 lines) |
| Layout | `app/src/main/res/layout/fragment_profile.xml` (12 ids) |
| Instantiation | `ProfileFragment.getInstance(userId, who)` -> arguments `userId`, `who` |
| View access | `findViewById` on the inflated view |
| Hosted by | `TabAdapter` (`who="me"`, `Util.userId`) and `ViewProfileActivity` (`who="other"`) |
| Exits to | `ImageDetailActivity` [FEED], `AboutActivity` [APPSHELL], `FollowerActivity` x2 |
| Progress | `SpotsDialog`, "Please Wait", dismissed immediately in `onCreateView` |

---

## 2. UI inventory (`fragment_profile.xml`)

| View id | Type | Bound to | Action |
|---|---|---|---|
| `profile_pic_main` | `ImageView` | `UserRslt.picture` via Picasso | tap -> `ImageDetailActivity` (extra `profilePic`), only if non-empty |
| `profile_name` | `TextView` | `name` **+ a role suffix** (see below) | — |
| `profile_about` | `TextView` | *(declared but **never populated**)* | — (issue MP1) |
| `followers_linear` | `LinearLayout` | — | tap -> `FollowerActivity` (`page="follower"`, `userId`) |
| `followers` | `TextView` | follower count | — |
| `following_linear` | `LinearLayout` | — | tap -> `FollowerActivity` (`page="following"`, `userId`) |
| `following` | `TextView` | following count | — |
| `posts_linear` | `LinearLayout` | — | *(no listener)* |
| `posts` | `TextView` | post count | — |
| `edit_profile` | Button | — | **VISIBLE only when `who == "me"`**; tap -> **`AboutActivity`** (issue MP2) |
| `my_post_list` | `RecyclerView` | FEED's `HomeAdapter` | the post list |
| `no_data` | `LinearLayout` | — | empty state |

### The role suffix

```kotlin
val role = if (loginresp.role == "admin") " - " + "Admin"
           else if (loginresp.isWarrior.toBoolean()) " - " + Util.WARRIOR
           else ""
profileName.text = loginresp.name + role
```

So the name renders as `"Jane Doe - Admin"`, `"Jane Doe - Warrior"`, or plain. `"admin"` is a
**magic string** and the concatenation is not localisable (issue MP3).

---

## 3. The adapter handoff (cross-team)

```kotlin
val whoPage = if (who == "me") "profile" else "OtherProfile"
adapter = HomeAdapter(ArrayList(), contexts, whoPage, HashMap(), HashMap(), myLikesMap, this)
```

| Argument | Value here | Consequence |
|---|---|---|
| posts | empty `ArrayList()` | filled later by `getallPosts` |
| page | `"profile"` or `"OtherProfile"` | controls which card buttons show (FEED_LEAD §3) |
| 1st map | **`HashMap()`** | **always empty** (issue MP4) |
| 2nd map | **`HashMap()`** | **always empty** (issue MP4) |
| 3rd map | `myLikesMap` | **real**, built by `getallLikes` |

Two of the three state maps are **throwaway empty maps**, unlike `HomeFragment` which passes all
three. In `"profile"` mode the follow/fav buttons are hidden anyway, but in `"OtherProfile"` mode
this still means the card cannot reflect reality.

> Note the argument order differs from `HomeFragment`'s call — here `myLikesMap` sits in the **third**
> map position. Easy to transpose by accident (issue MP5).

---

## 4. API calls (5 methods)

| Method | Endpoint | Purpose | Envelope |
|---|---|---|---|
| `getmyDetails(context, owner)` | `GET users/{userId}` | avatar + name + role; also sets `Util.user` | `result` |
| `getallPosts(context, owner, postlist)` | `GET post/user/{userId}?page&size` | the user's posts, paged | `results` + `count` |
| `getallLikes(owner)` | `GET like/user/{userId}` | builds `myLikesMap` | `results` |
| `getallFollowers(owner)` | `GET follows/user/{userId}` | the follower count | `results` |
| `getallFollowing(owner)` | `GET follows/{userId}` | the following count | `results` |

All five share the FEED/PROFILE house style: `isNetworkAvailable` guard, SpotsDialog, a **new**
`authToken` observer each time, the **always-true `||` token guard**, `dialog.hide()` instead of
`dismiss()`, and commented-out error parsing (issues MP6–MP9).

`getallLikes(viewLifecycleOwner)` is called **before** `inflater.inflate(...)` in `onCreateView`, so
its callback can touch views that do not exist yet if it returns fast (issue MP10).

---

## 5. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read (5 separate observers) |
| DataStore `token` + `userId` | deleted in the unreachable logout branches |
| `Util.user` | **written** by `getmyDetails` — the 5th copy of that block |
| `Util.userId` | read (the `"me"` identity, passed in by `TabAdapter`) |
| `Util.WARRIOR` | read (name suffix) |
| `Util.player` | stopped/released in `onPause` and `onDestroy` (audio posts) |

---

## 6. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | `super` only |
| **`setUserVisibleHint`** | when the tab becomes visible: `requireFragmentManager().beginTransaction().detach(this).attach(this).commit()` — a **full recreate on every tab switch** (issue MP11) |
| `onCreateView` | read `userId`/`who` -> `UserPreferences` -> SpotsDialog -> **`getallLikes`** -> inflate -> 10 `findViewById`s -> toggle `edit_profile` -> wire 4 listeners -> build the adapter |
| `onPause` / `onDestroy` | `dialog.dismiss()` + stop/reset/release/null `Util.player` |
| `onResume` | `dialog.dismiss()` |

`setUserVisibleHint` is **deprecated** (replaced by `FragmentTransaction.setMaxLifecycle`), and the
detach/attach cycle re-runs every network call each time the user returns to the tab.

Arguments are read with `arguments?.get("userId").toString()`, so a missing argument becomes the
literal `"null"` (issue MP12).

---

## 7. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `HomeAdapter` + `child_post.xml` | **FEED / HOME_FEED** | the entire post list |
| `ImageDetailActivity` | FEED / IMAGE_DETAIL | avatar full-screen |
| `AboutActivity` | APPSHELL / ABOUT | the "Edit Profile" button's real destination |
| `FollowerActivity` | PROFILE / FOLLOWERS | both count taps |
| `RetrofitAPI` (5 endpoints) | PLATFORM / NETWORK | all calls |
| `UserRslt`, `Posts`, `PostLikes`, `AllFollowerList` | PLATFORM / DATA_MODELS | parsing |
| `UserPreferences` | PLATFORM / STORAGE | token |
| `Util.*`, `Commons()` | PLATFORM / COMMONS | globals + guard |
| Picasso | PLATFORM / BUILD_CONFIG | avatar |
| `ViewProfileActivity` | PROFILE / VIEW_PROFILE | the `"other"` host |

---

## 8. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| MP11 | `setUserVisibleHint` does `detach().attach()` on **every** tab visit — full recreate + 5 re-fetches, via a deprecated API | **High** | load once; use `setMaxLifecycle` |
| MP4 | Two **empty `HashMap()`s** are passed for follow and fav state, so those states never render | **High** | build and pass the real maps |
| MP10 | `getallLikes` runs **before** the layout is inflated | **High** | move it after `inflate` |
| MP6 | 5 `authToken` observers, one per method | **High** | one-shot read |
| MP7 | The always-true `||` token guard; all 5 logout branches are dead | **High** | use `&&` |
| MP8 | Error parsing commented out — a failure looks like an empty profile | **High** | restore and show an error |
| MP13 | Retrofit calls are never cancelled; callbacks touch views after detach | **High** | cancel in `onDestroyView` |
| MP2 | "Edit Profile" opens **`AboutActivity`**, not `EditProfileActivity` | Medium | route directly, or rename the button |
| MP1 | `profile_about` exists in the layout but is **never populated** | Medium | bind it or remove it |
| MP5 | The adapter argument order differs from `HomeFragment`'s call | Medium | named arguments |
| MP9 | `dialog.hide()` instead of `dismiss()` | Medium | use `dismiss()` |
| MP12 | `arguments?.get(...).toString()` yields `"null"` when missing | Medium | `requireArguments().getString(...)` |
| MP15 | `page` starts at 0 here but at 1 in `HomeFragment` — inconsistent paging | Medium | align with HOME_FEED |
| MP3 | `role == "admin"` is a magic string; the suffix is concatenated, not localised | Low | constants + string resources |
| MP14 | `posts_linear` has no listener while the other two counts do | Low | add or remove |
| MP16 | No pull-to-refresh, unlike the home feed | Low | add if wanted |

---

## 9. What is **not** this agent's problem

| Symptom | Real owner |
|---|---|
| Post cards look wrong / like or delete misbehaves | **FEED / HOME_FEED** |
| Avatar viewer cannot pan or zoom | FEED / IMAGE_DETAIL |
| The follower list itself is wrong | PROFILE / FOLLOWERS |
| The edit form is wrong | PROFILE / EDIT_PROFILE |
| "Edit Profile" lands on an odd screen | APPSHELL / ABOUT (plus MP2 here) |

---

## 10. How to make common changes

**Populate `profile_about` (MP1):** `UserRslt` has no "about/bio" field — check
`PLATFORM/DATA_MODELS` first; adding one is a **PLATFORM task via PM**.

**Pass the real follow/fav maps (MP4):** add `getallFav` plus a follow map (mirroring
`HomeFragment`'s `getallFollowers`) and pass them instead of `HashMap()`. Local, but verify the
result in **both** `who` modes.

**Stop the reload on every tab switch (MP11):** delete the `setUserVisibleHint` override and load in
`onCreateView` only. Highest-value fix here — confirm the tab still refreshes when expected.

**Change what the post list shows:** that is `HomeAdapter` — **FEED/HOME_FEED, via PM**. From here
you can only change the `page` string you pass.

**Add a field to the profile header:** add the view to `fragment_profile.xml`, bind it in
`onCreateView`, populate it in `getmyDetails`. Remember it renders in **both** modes — decide
whether it should be visible for other users.

---

## 11. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/fragments/ProfileFragment.kt` | **owned, but shared-use** — `VIEW_PROFILE.md` depends on the `"other"` mode |
| `app/src/main/res/layout/fragment_profile.xml` | **owned, but shared-use** — rendered in both modes |

**Not owned (escalate):** `HomeAdapter.kt`, `child_post.xml` (FEED), `ViewProfileActivity.kt`
(VIEW_PROFILE), `AboutActivity.kt` (APPSHELL), `FollowerActivity.kt` (FOLLOWERS), and all PLATFORM
files.

---

## 12. Change log

| Change | Detail |
|---|---|
| Created | Initial MY_PROFILE module agent documented from `ProfileFragment.kt` (469 lines) and `fragment_profile.xml` (12 ids): the `who` multiplexer, all 12 views incl. the never-populated `profile_about`, the role-suffix logic, the cross-team `HomeAdapter` handoff with its **two empty state maps**, all 5 API calls with their envelopes, the `setUserVisibleHint` detach/attach reload, and 16 known issues. Confirmed the "Edit Profile" button opens `AboutActivity`. |


