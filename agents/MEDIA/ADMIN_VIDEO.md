# ADMIN_VIDEO Module Agent

> Team: **MEDIA** · Reports to: `agents/MEDIA/MEDIA_LEAD.md`
> This agent knows **every detail** of the curated admin **video** feed tab.
> It may only edit the files listed in §9 **Owned files**.

---

## ⚠ Twin-file warning

`AdminVideoFragment` (392 lines) is a **near-identical copy** of `AdminAudioFragment` (396 lines).
Verified differences:

| | This agent (Video) | `ADMIN_AUDIO.md` |
|---|---|---|
| Endpoint | `GET api/v1/post/admin/video` | `GET api/v1/post/admin/audio` |
| Retrofit | `getVideoPost(head, page, size)` | `getAudioPost(head, page, size)` |
| Log tags | `AdminVideoFragment.*` | `AdminAudioFragment.*` |
| Companion | **none** | has an (unused) `getInstance()` |

Everything else is duplicated line for line. **Any fix here almost certainly applies there too** —
make both changes in the same change set (team issue M-1).

**Read `ADMIN_AUDIO.md` alongside this file.** Its §2–§7 describe the identical structure; this
document focuses on what is video-specific.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Video** (bottom-nav tab 2) |
| Class | `com.veha.fragments.AdminVideoFragment` (392 lines) |
| Layout | `app/src/main/res/layout/fragment_admin_video.xml` |
| Instantiation | `AdminVideoFragment()` by `TabAdapter` — **no companion object** (issue AV1) |
| Hosted by | `TabAdapter` position **2** [APPSHELL] |
| View access | `findViewById` |
| Content | **FEED's `HomeAdapter`** with page `"home"` |
| Progress | `SpotsDialog`, "Please Wait" |

---

## 2. Video-specific behaviour

Unlike audio, **this fragment contains no player code at all** — not even teardown, beyond the
shared `Util.player` block it inherited by copy-paste. Video playback happens entirely inside
`HomeAdapter`'s `"video"` branch:

| Aspect | Where it lives |
|---|---|
| `YouTubePlayerView` + `onReady` callback | `HomeAdapter` [FEED/HOME_FEED] |
| URL construction | **`Util.getVideo(url)`** -> `https://salvationlamb.com/video/<url>` [PLATFORM/COMMONS] |
| Lifecycle of the player view | `HomeAdapter` / the library |

> **`Util.getVideo` is a second hard-coded host**, unrelated to `Util.url`
> (`https://server.salvationlamb.com`). Changing the API host does **not** change the video host
> (COMMONS C-8, issue AV2).

The inherited `Util.player` teardown in `onPause`/`onDestroy` is **dead weight here** — this tab
never starts a `MediaPlayer` — but it is harmless and, because the same adapter can render audio
cards, it is safer to leave it (issue AV3).

---

## 3. API calls (5)

| Method | Endpoint | Purpose | Envelope |
|---|---|---|---|
| `getallPosts(context, owner, postlist)` | **`GET post/admin/video?page&size=10`** | the video feed, paged | `results` + `count` |
| `getallLikes(owner)` | `GET like/user/{userId}` | `myLikesMap` | `results` |
| `getallFollowers(owner)` | `GET follows/{userId}` | `myFollowMap` | `results` |
| `getallFav(owner)` | `GET favorites/{userId}` | `myFavMap` | `results` |
| `getMyDetails(owner)` | `GET users/{userId}` | warrior check -> `Util.user` | `result` |

`getMyDetails` here is the **8th** copy of that block (team issue M-8).

Same house style as the twin: `isNetworkAvailable` guard, a **fresh `authToken` observer per call**,
the **always-true `||` token guard**, `dialog.hide()` instead of `dismiss()`, commented-out error
parsing, and a **new `OnScrollListener` per page load** (issues AV4–AV8).

---

## 4. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read (5 observers) |
| DataStore `token` + `userId` | deleted in the unreachable logout branches |
| `Util.user` | **written** by `getMyDetails` |
| `Util.userId`, `Util.isWarrior`, `Util.fontSize` | read |
| `Util.player` | released in `onPause`/`onDestroy` — **inherited, unused here** |

---

## 5. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | `super` only |
| `onCreateView` | inflate -> `UserPreferences` -> SpotsDialog -> bind list + empty state -> build the `HomeAdapter` -> kick off the loads |
| `onPause` / `onDestroy` | `dialog.dismiss()` + the inherited `Util.player` teardown |
| `onResume` | `dialog.dismiss()` |

**No YouTube player lifecycle handling.** The `androidyoutubeplayer` view is created inside the
adapter and is **never explicitly released** when the tab is left, so a video can keep playing (or
keep its WebView alive) after the user navigates away (issue AV9).

---

## 6. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| **`HomeAdapter` + `child_post.xml`** | **FEED / HOME_FEED** | all rendering and the YouTube player |
| `androidyoutubeplayer:core:12.0.0` | PLATFORM / BUILD_CONFIG | playback |
| **`Util.getVideo(url)`** | PLATFORM / COMMONS | the second hard-coded host |
| `RetrofitAPI.getVideoPost` + 4 others | PLATFORM / NETWORK | the calls |
| `Posts`, `PostUser`, `PostLikes`, `UserRslt`, `AllFavList` | PLATFORM / DATA_MODELS | parsing |
| `UserPreferences` | PLATFORM / STORAGE | token |
| `TabAdapter` | APPSHELL / MAIN_NAV | hosting |
| `LoginActivity` | AUTH | forced re-login target |
| `AdminAudioFragment` | **MEDIA / ADMIN_AUDIO** | its twin — mirror every change |

---

## 7. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| M-1 | A **~390-line near-duplicate** of `AdminAudioFragment` | **High** | shared base fragment (PM-level, two agents) |
| AV9 | The YouTube player is never released on tab change — playback/WebView can outlive the screen | **High** | release in `onPause` (needs FEED, since the view lives in the adapter) |
| AV5 | A new `OnScrollListener` per page load | **High** | add it once |
| AV4 | 5 `authToken` observers, one per method | **High** | one-shot read |
| AV6 | The always-true `||` token guard; all 5 logout branches dead | **High** | use `&&` |
| AV7 | Error parsing commented out — a failure looks like an empty feed | **High** | restore and show an error |
| AV10 | Retrofit calls are never cancelled | **High** | cancel in `onDestroyView` |
| AV2 | `Util.getVideo` hard-codes a **second host** independent of the API base URL | Medium | move to NETWORK config (PLATFORM) |
| AV8 | `dialog.hide()` instead of `dismiss()` | Medium | use `dismiss()` |
| M-10 | Page `"home"` shows follow/fav buttons on curated admin content | Medium | new page value (needs FEED) |
| M-8 | `getMyDetails` copy #8 | Medium | shared helper (PM-level) |
| AV3 | Inherited `Util.player` teardown that this tab never needs | Low | leave it; note it when refactoring |
| AV1 | No `getInstance()` companion, unlike its twin | Low | align the two |
| AV11 | No pull-to-refresh and no screen title | Low | add if wanted |

---

## 8. What is **not** this agent's problem

| Symptom | Real owner |
|---|---|
| The video does not play / the player looks wrong | **FEED / HOME_FEED** (the `"video"` branch) |
| The video URL is wrong | PLATFORM / COMMONS (`Util.getVideo`) |
| Follow/fav buttons appear on admin content | FEED / HOME_FEED (page `"home"`) |
| The tab is in the wrong position | APPSHELL / MAIN_NAV (`TabAdapter`) |

---

## 9. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/fragments/AdminVideoFragment.kt` | **exclusive** |
| `app/src/main/res/layout/fragment_admin_video.xml` | **exclusive** |

**Not owned (escalate):** `HomeAdapter.kt` / `child_post.xml` (**FEED — the YouTube player lives
there**), `AdminAudioFragment.kt` (ADMIN_AUDIO — the twin), `Util.java` (`getVideo`, `player`),
`TabAdapter.kt` (APPSHELL), all PLATFORM files.

---

## 10. How to make common changes

**Any change at all:** check whether `ADMIN_AUDIO.md` needs the same edit.

**Release the player on tab change (AV9):** the `YouTubePlayerView` is created in `HomeAdapter`, so
the fix needs a FEED change (expose a release hook) plus a call from this fragment's `onPause`.
**Two-agent change via PM** — the highest-value fix here.

**Change the video host (AV2):** `Util.getVideo` is PLATFORM/COMMONS — escalate. Note that changing
`Util.url` alone will **not** affect video URLs.

**Fix the scroll-listener leak (AV5):** move `addOnScrollListener` into `onCreateView`. Local —
mirror in the twin.

**Merge the twins (M-1):** propose a `BaseAdminFeedFragment` taking the Retrofit call and log tag.
It spans two agents' files — PM decides, then both agents execute together.

---

## 11. Change log

| Change | Detail |
|---|---|
| Created | Initial ADMIN_VIDEO module agent documented from `AdminVideoFragment.kt` (392 lines): the verified 4-point diff against its audio twin, the fact that **all** video playback lives in FEED's `HomeAdapter` while `Util.getVideo` supplies a **second hard-coded host**, all 5 API calls, the inherited-but-unused `Util.player` teardown, the unreleased YouTube player, and 14 known issues. |


