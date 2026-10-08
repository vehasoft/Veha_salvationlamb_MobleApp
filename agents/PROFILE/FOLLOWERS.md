# FOLLOWERS Module Agent

> Team: **PROFILE** · Reports to: `agents/PROFILE/PROFILE_LEAD.md`
> This agent knows **every detail** of the followers/following screen: the dual-mode activity, the
> `FollowAdapter` row with its follow/unfollow button, the two near-identical endpoints and the
> follow-state logic.
> It may only edit the files listed in §10 **Owned files**.

---

## ⚠ One activity, two lists

`FollowerActivity` serves **both** lists, selected by the `page` extra:

| `page` | Method | Endpoint | Shows |
|---|---|---|---|
| `"follower"` | `getAllFollowers(this)` | `GET api/v1/follows/user/{userId}` | people following this user |
| `"following"` | `getAllFollowing(this)` | `GET api/v1/follows/{userId}` | people this user follows |

The two endpoints differ **only by the `/user` segment** and the two methods are otherwise
near-identical copies (NETWORK N6, issue FL1). Unlike the AUTH dual-mode classes, both modes are
owned by **this single agent** — the user intent ("see a list of people") is the same.

> **Dispatch bug:** both checks are separate `if`s with **no `else`** and **no fallback**, so an
> unrecognised `page` value loads **nothing** and the screen sits empty with no message (issue FL2).

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Followers / Following** |
| Classes | `com.veha.activity.FollowerActivity` (254 lines) + `com.veha.adapter.FollowAdapter` |
| Layouts | `activity_follower.xml` (7 ids), `child_follow.xml` (4 ids) |
| Manifest | `<activity android:name=".FollowerActivity" android:exported="false"/>` |
| View access | `findViewById` **plus a synthetic import for `menu`** (issue FL3) |
| Required extras | **`userId`** and **`page`** (`"follower"` / `"following"`) |
| Entered from | `ProfileFragment` — the `followers_linear` and `following_linear` taps |
| Exits to | `ViewProfileActivity` (row tap), `MainActivity` (logo), overflow destinations |
| Progress | `SpotsDialog`, "Please Wait" |

---

## 2. UI inventory

### `activity_follower.xml`

| View id | Type | Purpose |
|---|---|---|
| `header_main` | container | header bar |
| `prod_logo` | `ImageView` | tap -> `MainActivity` (**no `finish()`**) |
| `night_mode` | view | **declared but never referenced in code** (issue FL4) |
| `day_mode` | view | **declared but never referenced in code** (issue FL4) |
| `menu` | overflow trigger | the app-wide menu |
| `my_follow_list` | `RecyclerView` | the list |
| `no_data` | `LinearLayout` | empty state |

**No title**, so the screen looks identical in both modes — the user cannot tell whether they are
viewing followers or following (issue FL5).

### `child_follow.xml` — one person row

| View id | Type | Bound to |
|---|---|---|
| `follow_list_linear` | `LinearLayout` | row container; tap -> `ViewProfileActivity` (extra `userId` = `follow.id`) |
| `profile_pic_fol` | `ImageView` | `follow.picture` via Picasso (only if non-empty) |
| `name_fol` | `TextView` | `follow.name` |
| `follow_btn` | `Button` | **"follow" / "unfollow"** — see §3 |

Rows are `PostUser` objects (the lightweight user model), not `UserRslt`.

> `profile_pic_fol` and `name_fol` share their ids with `child_likes_list.xml` (FEED/VIEW_LIKES
> VL2) — separate files, same naming convention.

---

## 3. The follow button logic (**broken**)

`FollowAdapter.onBindViewHolder` contains two **consecutive, contradictory** blocks:

```kotlin
if (!myFollowList.containsKey(Util.userId)) holder.followBtn.text = "follow"
else                                        holder.followBtn.text = "unfollow"

if (myFollowList.containsValue(Util.userId)) holder.followBtn.text = "follow"
else                                         holder.followBtn.text = "unfollow"
```

| Problem | Detail |
|---|---|
| The first block is **dead** | the second unconditionally overwrites the text it just set (issue FL6) |
| Wrong key | both check **`Util.userId`** (the *viewer*) rather than `follow.id` (the *row's* user), so every row shows the **same** label (issue FL7) |
| Inverted | `containsValue(Util.userId)` true -> `"follow"`, which reads backwards relative to the map's semantics (issue FL8) |
| Net effect | the follow/unfollow label is **effectively arbitrary and identical for every row** |

### What the button does

```kotlin
if (holder.followBtn.text.equals("follow")) { follow(Util.userId, follow.id); text = "unfollow" }
else                                        { follow(Util.userId, follow.id); text = "follow" }
```

**Both branches call the same `follow(...)`** with the same arguments — the endpoint is a **toggle**
server-side. Only the local label differs, so the `if/else` is redundant (issue FL9).

`follow()` posts `{userId, followerId}` to `POST api/v1/follows` and, on 200, toggles the entry in
`myFollowList` locally.

> **This adapter gets two things right:** its token guard uses **`&&`** (correct), and it **does**
> parse and log `status` + `errorMessage` on failure — one of very few places that does. It still
> shows the user nothing, though (issue FL10).

---

## 4. API calls

| Method | Endpoint | Envelope | Notes |
|---|---|---|---|
| `getAllFollowers(context)` | `GET follows/user/{userId}` | `results` -> `AllFollowerList` -> `.user` | builds `followList` + `myFollowerMap` |
| `getAllFollowing(context)` | `GET follows/{userId}` | `results` -> `AllFollowerList` -> `.user` | builds `followList` + `followingMap` |
| `FollowAdapter.follow(userId, followerId)` | `POST follows` | — | toggle |

Each list method creates a **new `FollowAdapter`** on every load, passing a different map
(`myFollowerMap` vs `followingMap`) — so the two modes maintain **separate** state maps whose
contents are then compared against the wrong key (§3).

Both list methods use the **always-true `||` token guard** (issue FL11) and a fresh `authToken`
observer; the activity's error branches are commented out (issue FL12).

---

## 5. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read (observed, per call) |
| DataStore `token` + `userId` | deleted by the menu's logout |
| `Util.userId` | read — as the follow actor **and**, incorrectly, as the row key (FL7) |
| `Util.isWarrior`, `Util.user.isReviewState` | read -> menu visibility (unguarded — issue FL13) |

---

## 6. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | layout -> `UserPreferences` -> SpotsDialog -> read `userId` -> **dispatch on `page`** -> bind `prod_logo`, `my_follow_list`, `no_data` -> wire the overflow menu |
| `onPause` / `onResume` / `onDestroy` | `dialog.dismiss()` |

**Order bug:** `getAllFollowers` / `getAllFollowing` are called **before** `lists` and `nodata` are
bound via `findViewById`, so a fast response can touch `lateinit` views that are not yet assigned —
`UninitializedPropertyAccessException` (issue FL14).

Extras are read with `intent.extras!!.get("userId").toString()` — crashes with no extras, and yields
the literal `"null"` for a missing key (issue FL15).

---

## 7. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `RetrofitAPI.getFollowers` / `getFollowing` / `postFollow` | PLATFORM / NETWORK | all calls |
| `AllFollowerList` -> `PostUser` | PLATFORM / DATA_MODELS | parsing |
| `UserPreferences` | PLATFORM / STORAGE | token + logout |
| `Util.userId`, `Commons()` | PLATFORM / COMMONS | actor + guard |
| Picasso | PLATFORM / BUILD_CONFIG | avatars |
| `R.menu.main_menu`, `R.style.menuStyle` | APPSHELL / THEMING | overflow menu |
| `ProfileFragment` | PROFILE / MY_PROFILE | the only entry point |
| `ViewProfileActivity` | PROFILE / VIEW_PROFILE | row tap |
| `MainActivity`, `SettingsActivity` | APPSHELL | logo + menu |
| `LoginActivity` | AUTH | logout target |

---

## 8. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| FL7 | The follow button checks **`Util.userId`** instead of the row's `follow.id`, so **every row shows the same label** | **High** | key the map lookup on `follow.id` |
| FL6 | Two consecutive contradictory blocks set the label; the first is **dead code** | **High** | delete the first, fix the second |
| FL8 | The surviving check is **inverted** relative to the map's meaning | **High** | invert |
| FL14 | List loading starts **before** `findViewById` binds `lists`/`nodata` — `UninitializedPropertyAccessException` on a fast response | **High** | bind views first |
| FL2 | Two bare `if`s with no `else`/fallback — an unknown `page` loads nothing, silently | **High** | `when` with an `else` |
| FL11 | The always-true `||` guard in both list methods | **High** | use `&&` (the adapter already does) |
| FL13 | `Util.user.isReviewState.toBoolean()` unguarded — NPE after process death | **High** | null-guard |
| FL3 | `menu` resolved via a **synthetic import of another layout** | **High** | use `findViewById` |
| FL16 | The ~60-line overflow menu is duplicated here too | **High** | shared handler (PM-level) |
| FL9 | Both button branches call the identical `follow(...)`, so the `if/else` is pointless | Medium | collapse it |
| FL12 | The activity's error branches are commented out — a failure looks like an empty list | Medium | restore |
| FL5 | **No title** — followers and following look identical | Medium | add a per-mode title |
| FL15 | `intent.extras!!.get(...).toString()` crashes / yields `"null"` | Medium | validate the extras |
| FL10 | `follow()` logs the error properly but still shows the user nothing | Medium | toast on failure |
| FL1 | `getAllFollowers` / `getAllFollowing` are near-identical copies | Medium | one method + a path parameter |
| FL17 | A new adapter is created on every load rather than updating the existing one | Medium | reuse + notify |
| FL20 | Button labels are the lower-case literals `"follow"`/`"unfollow"`, compared by **text** rather than state | Medium | track state, not labels |
| FL4 | `night_mode` and `day_mode` exist in the layout but are **never referenced** | Low | remove, or wire to THEMING |
| FL18 | No pagination — the whole list loads at once | Low | paginate if needed |
| FL19 | `Picasso.with(context)` deprecated | Low | upgrade |

---

## 9. Notable: the state-comparison bug chain

FL7 and MY_PROFILE's MP4 are the **same root problem** seen from two ends: `MY_PROFILE` hands
`HomeAdapter` **empty** follow/fav maps, and `FollowAdapter` consults its map with the **wrong key**.
Follow state is therefore unreliable across the whole app. Fixing it properly spans
**PROFILE + FEED** and should be a single PM-coordinated task.

---

## 10. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/FollowerActivity.kt` | **exclusive** (both modes) |
| `app/src/main/java/com/veha/adapter/FollowAdapter.kt` | **exclusive** |
| `app/src/main/res/layout/activity_follower.xml` | **exclusive** |
| `app/src/main/res/layout/child_follow.xml` | **exclusive** |
| `<activity android:name=".FollowerActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `UsersAdapter.kt` (**SEARCH**, despite sitting beside `FollowAdapter`),
`ProfileFragment.kt` (MY_PROFILE), `ViewProfileActivity.kt` (VIEW_PROFILE), `main_menu.xml`
(APPSHELL), all PLATFORM files.

---

## 11. How to make common changes

**Fix the follow button (FL6/FL7/FL8) — highest value here:** delete the first `if/else`, and change
the second to look up **`follow.id`**, not `Util.userId`. Verify in **both** `page` modes, since each
passes a different map. Then check `HOME_FEED`'s follow button for the same class of bug, and report
to PM (see §9).

**Add a title (FL5):** add a `TextView` to `activity_follower.xml` and set it from the `page` extra.
Local and cheap.

**Fix the init order (FL14):** move the `page` dispatch **below** the `findViewById` block in
`onCreate`. Local.

**Merge the two list methods (FL1):** extract one method taking the Retrofit call and the target map.
Reduces 254 lines noticeably and keeps the modes in sync.

**Wire or remove `night_mode` / `day_mode` (FL4):** theming belongs to **APPSHELL/THEMING** — ask PM
before implementing; deleting the unused views is local.

---

## 12. Change log

| Change | Detail |
|---|---|
| Created | Initial FOLLOWERS module agent documented from `FollowerActivity.kt` (254 lines), `FollowAdapter.kt` and both layouts: the `page` dual-mode dispatch with its missing fallback, 7 + 4 view ids, the two near-identical endpoints, and the **three-part follow-button bug** (dead first block, wrong map key, inverted condition) that makes every row show the same label. Linked it to MY_PROFILE's empty-map issue as one cross-team root cause. 20 known issues. |


