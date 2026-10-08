# ADMIN_AUDIO Module Agent

> Team: **MEDIA** · Reports to: `agents/MEDIA/MEDIA_LEAD.md`
> This agent knows **every detail** of the curated admin **audio** feed tab.
> It may only edit the files listed in §10 **Owned files**.

---

## ⚠ Twin-file warning

`AdminAudioFragment` (396 lines) and `AdminVideoFragment` (392 lines) are **near-identical copies**.
Verified differences:

| | This agent (Audio) | `ADMIN_VIDEO.md` |
|---|---|---|
| Endpoint | `GET api/v1/post/admin/audio` | `GET api/v1/post/admin/video` |
| Retrofit | `getAudioPost(head, page, size)` | `getVideoPost(head, page, size)` |
| Log tags | `AdminAudioFragment.*` | `AdminVideoFragment.*` |
| Companion | **has `getInstance()`** | has none |

Everything else is duplicated line for line. **Any fix here almost certainly applies there too** —
make both changes in the same change set and say so in the report (team issue M-1).

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Audio** (bottom-nav tab 3) |
| Class | `com.veha.fragments.AdminAudioFragment` (396 lines) |
| Layout | `app/src/main/res/layout/fragment_admin_audio.xml` |
| Instantiation | `AdminAudioFragment()` by `TabAdapter`; a `getInstance()` companion exists but is **unused** (issue AA1) |
| Hosted by | `TabAdapter` position **3** [APPSHELL] |
| View access | `findViewById` |
| Content | **FEED's `HomeAdapter`** with page `"home"` |
| Progress | `SpotsDialog`, "Please Wait" |

---

## 2. What this screen actually is

A **second home feed** pointed at a different endpoint. It reproduces `HomeFragment`'s structure
almost exactly:

| Element | Same as `HomeFragment`? |
|---|---|
| `HomeAdapter(ArrayList(), ctx, "home", myLikesMap, myFollowMap, myFavMap, this)` | **yes**, identical signature and page value |
| 5 network methods (`getallPosts`, `getallLikes`, `getallFollowers`, `getallFav`, `getMyDetails`) | yes |
| `results` + `count` paging, size **10** | yes |
| three state maps | yes — and here they are **all real**, unlike MY_PROFILE's empty ones |
| `Util.player` released in `onPause`/`onDestroy` | yes |
| pull-to-refresh | **no** |

Because the adapter page is **`"home"`**, admin audio cards show **follow and favourite buttons**
for the admin author — inherited from FEED's contract (team issue M-10).

---

## 3. API calls (5)

| Method | Endpoint | Purpose | Envelope |
|---|---|---|---|
| `getallPosts(context, owner, postlist)` | **`GET post/admin/audio?page&size=10`** | the audio feed, paged | `results` + `count` |
| `getallLikes(owner)` | `GET like/user/{userId}` | `myLikesMap` | `results` |
| `getallFollowers(owner)` | `GET follows/{userId}` | `myFollowMap` | `results` |
| `getallFav(owner)` | `GET favorites/{userId}` | `myFavMap` | `results` |
| `getMyDetails(owner)` | `GET users/{userId}` | warrior check -> `Util.user` | `result` |

`getMyDetails` here is the **7th** copy of that block in the app (team issue M-8).

All five follow the same house style: `isNetworkAvailable` guard, SpotsDialog, a **fresh
`authToken` observer per call**, the **always-true `||` token guard**, `dialog.hide()` instead of
`dismiss()`, and commented-out error parsing (issues AA2–AA5).

The unreachable `else` branch toasts `"Somthing Went Wrong \nLogin again to continue"`, clears the
session and opens `LoginActivity`.

---

## 4. Paging

Identical to `HOME_FEED.md` §5.2: `count` is divided by 10, an `updated` flag guards double-appends,
and a **new `OnScrollListener` is added on every page load** and never removed (issue AA6).

---

## 5. Audio playback — `Util.player`

The actual `MediaPlayer` work lives in **`HomeAdapter`** (FEED), not here. This fragment's only
responsibility is **releasing** it:

```kotlin
override fun onPause()  { dialog.dismiss(); if (Util.player != null) { stop(); reset(); release(); Util.player = null } }
override fun onDestroy(){ dialog.dismiss(); if (Util.player != null) { stop(); reset(); release(); Util.player = null } }
```

Because `Util.player` is a **single static instance** shared with `HomeFragment` and
`AdminVideoFragment`, switching tabs while audio plays relies on this teardown running — and
`FragmentPagerAdapter` keeps neighbouring fragments alive, so `onPause` may not fire when expected
(issue AA7, team issue M-5).

---

## 6. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read (5 observers) |
| DataStore `token` + `userId` | deleted in the unreachable logout branches |
| `Util.user` | **written** by `getMyDetails` |
| `Util.userId`, `Util.isWarrior`, `Util.fontSize` | read |
| **`Util.player`** | **released and nulled** in `onPause`/`onDestroy` |

---

## 7. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | `super` only |
| `onCreateView` | inflate -> `UserPreferences` -> SpotsDialog -> bind the list and empty state -> build the `HomeAdapter` -> kick off the loads |
| `onPause` / `onDestroy` | `dialog.dismiss()` + full `Util.player` teardown |
| `onResume` | `dialog.dismiss()` |

No `setUserVisibleHint` override, so the tab does not force a reload on every visit.

---

## 8. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| **`HomeAdapter` + `child_post.xml`** | **FEED / HOME_FEED** | all rendering and playback |
| `RetrofitAPI.getAudioPost` + 4 others | PLATFORM / NETWORK | the calls |
| `Posts`, `PostUser`, `PostLikes`, `UserRslt`, `AllFavList` | PLATFORM / DATA_MODELS | parsing |
| `UserPreferences` | PLATFORM / STORAGE | token |
| `Util.player`, `Util.user`, `Commons()` | PLATFORM / COMMONS | global state + guard |
| `TabAdapter` | APPSHELL / MAIN_NAV | hosting |
| `LoginActivity` | AUTH | forced re-login target |
| `AdminVideoFragment` | **MEDIA / ADMIN_VIDEO** | its twin — mirror every change |

---

## 9. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| M-1 | This file is a **~390-line near-duplicate** of `AdminVideoFragment` | **High** | extract a shared base fragment taking the endpoint (PM-level, two agents) |
| AA6 | A new `OnScrollListener` is added on **every** page load | **High** | add it once |
| AA2 | 5 `authToken` observers, one per method | **High** | one-shot read |
| AA3 | The always-true `||` token guard; all 5 logout branches dead | **High** | use `&&` |
| AA4 | Error parsing commented out — a failure looks like an empty feed | **High** | restore and show an error |
| AA7 | Relies on `onPause` to release the shared static `Util.player`, which the pager may not call when expected | **High** | per-card player, or explicit tab-change handling |
| AA8 | Retrofit calls are never cancelled | **High** | cancel in `onDestroyView` |
| AA5 | `dialog.hide()` instead of `dismiss()` | Medium | use `dismiss()` |
| M-10 | Page `"home"` shows follow/fav buttons on curated admin content | Medium | confirm the intended UX, then pass a new page value (needs FEED) |
| M-8 | `getMyDetails` copy #7 | Medium | shared helper (PM-level) |
| AA1 | An unused `getInstance()` companion (the video twin has none) | Low | use it or delete it |
| AA9 | No pull-to-refresh, unlike the home feed | Low | add if wanted |
| AA10 | No screen title — the tab is identified only by its nav icon | Low | confirm with APPSHELL |

---

## 10. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/fragments/AdminAudioFragment.kt` | **exclusive** |
| `app/src/main/res/layout/fragment_admin_audio.xml` | **exclusive** |

**Not owned (escalate):** `HomeAdapter.kt` / `child_post.xml` (**FEED — all rendering and the audio
player live there**), `AdminVideoFragment.kt` (ADMIN_VIDEO — the twin), `Util.java` (`player`),
`TabAdapter.kt` (APPSHELL), all PLATFORM files.

---

## 11. How to make common changes

**Any change at all:** check whether `ADMIN_VIDEO.md` needs the same edit. These files diverge only
by endpoint and log tag — silent divergence is the main risk in MEDIA.

**"Audio won't play / the seek bar is wrong / two tracks overlap":** **not this agent.** Playback is
`HomeAdapter`'s — route to `HOME_FEED.md` (and note `Util.player` is PLATFORM-owned).

**Hide follow/fav on curated content (M-10):** requires a **new `page` value** in `HomeAdapter`'s
visibility chain — a FEED change via PM; this agent then passes the new string.

**Add pull-to-refresh (AA9):** wrap the list in a `SwipeRefreshLayout` and re-call `getallPosts`.
Prefer a real re-fetch over `HomeFragment`'s `detach().attach()` approach.

**Fix the scroll-listener leak (AA6):** move `addOnScrollListener` out of the response callback into
`onCreateView`. Local — and mirror it in the twin.

---

## 12. Change log

| Change | Detail |
|---|---|
| Created | Initial ADMIN_AUDIO module agent documented from `AdminAudioFragment.kt` (396 lines): its role as a second home feed on `post/admin/audio`, the verified 4-point diff against its video twin, all 5 API calls with envelopes, the `results`+`count` paging with its listener leak, the `Util.player` teardown contract shared with FEED, and 13 known issues. |


