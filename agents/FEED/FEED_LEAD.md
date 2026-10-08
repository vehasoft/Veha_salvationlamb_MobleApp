# FEED TEAM LEAD Agent

> Reports to: **PROJECT MANAGER** (`AGENTS.md`)
> Manages the 6 module agents below. **Status: all READY.**

---

## 1. Charter

FEED owns **everything about posts**: the timeline, creating a post, viewing a single post,
reactions ("likes"), favourites/bookmarks, sharing, deleting, and the full-screen image preview.

It owns the app's largest and most complex component: `HomeAdapter` (680 lines), which renders four
post types and carries almost all post **actions** inside the adapter itself.

**Boundary:** FEED stops at the post. The author's profile belongs to PROFILE, media playback
*mechanics* beyond the inline player belong to MEDIA, and the bottom navigation hosting the feed
belongs to APPSHELL. FEED does **not** own `RetrofitAPI.kt`, `DataModels.kt` or `Util.java`.

> **Key structural fact:** `HomeFragment` + `HomeAdapter` are **reused by four different screens**
> via a `page`/`type` string. See §3 — this is the single most important thing to understand before
> changing anything in FEED.

---

## 2. Modules owned

| Module agent | Screen / unit | Source files | Status |
|---|---|---|---|
| `HOME_FEED.md` | Home timeline | `fragments/HomeFragment.kt` (528), `adapter/HomeAdapter.kt` (680), `res/layout/fragment_home.xml`, `res/layout/child_post.xml` | READY |
| `ADD_POST.md` | Create post | `activity/AddPostActivity.kt` (292), `res/layout/activity_add_post.xml` | READY |
| `VIEW_POST.md` | Single post — **DEAD + BROKEN** | `activity/ViewPostActivity.kt` (102), `res/layout/activity_view_post.xml` | READY |
| `VIEW_LIKES.md` | Who reacted | `activity/ViewLikesActivity.kt` (199), `adapter/ViewLikesAdapter.kt`, `res/layout/activity_view_likes.xml`, `child_likes_list.xml` | READY |
| `FAVORITES.md` | Saved posts (host shell) | `activity/FavoritesActivity.kt` (121), `res/layout/activity_favorites.xml` | READY |
| `IMAGE_DETAIL.md` | Full-screen image | `activity/ImageDetailActivity.kt` (43), `res/layout/activity_image_detail.xml` | READY |

> **Shared-component warning:** `HomeAdapter` is owned by `HOME_FEED.md` but is **instantiated by
> PROFILE and SEARCH screens too** (`ProfileFragment`, `ViewProfileActivity`,
> `SearchProfileFragment`). Any change to it is a **cross-team** change — escalate to PM so those
> teams are notified.

---

## 3. The `page` / `type` multiplexer (read this first)

### `HomeFragment.getInstance(type)` — 2 known callers

| `type` | Launched by | Behaviour |
|---|---|---|
| `"user"` | `TabAdapter` | normal timeline — `getallPosts` |
| `"fav"` | `FavoritesActivity` | favourites only — `getfavPosts`; create-post FAB suppressed |

### `HomeAdapter(posts, context, page, …)` — 4 `page` values

| `page` | Used by | Follow btn | Delete btn | Fav / Save |
|---|---|---|---|---|
| `"home"` | `HomeFragment` | **visible** | gone | **visible** |
| `"profile"` | `ProfileFragment` [PROFILE] | gone | **visible** | gone |
| `"OtherProfile"` | `ViewProfileActivity` [PROFILE] | gone | gone | gone |
| `"searchProfile"` | `SearchProfileFragment` [SEARCH] | gone | gone | gone |

Both are **magic strings with no constants**, compared with `contentEquals`. A typo silently yields
a post card with every optional control hidden (team issue F-1).

---

## 4. Intra-team flows

```
MainActivity bottom nav [APPSHELL]
   `-> HomeFragment(type="user")
          |-- FAB add_post (warriors only) --> AddPostActivity
          |-- post image tap ---------------> ImageDetailActivity
          |-- reaction count tap -----------> ViewLikesActivity (extra: postId)
          |-- author name/avatar tap -------> ViewProfileActivity [PROFILE]
          |-- share ------------------------> ACTION_SEND chooser ("choose one")
          |-- like -------------------------> PopupMenu (14 reactions) -> POST like
          |-- fav/save ---------------------> POST favorites
          `-- delete (profile page only) ---> DELETE post/{id}

FavoritesActivity
   `-> HomeFragment(type="fav")  -- GET favorites/{userId} --> same card UI

AddPostActivity -- POST post --> back to feed (manual refresh)
```

---

## 5. API surface used by this team

All via `Util.getRetrofit()` (PLATFORM/NETWORK), all `Bearer <token>`.

| Endpoint | Retrofit method | Used by |
|---|---|---|
| `GET api/v1/post?page&size` | `getPost(head, page, size)` | HOME_FEED (paged, size **10**) |
| `GET api/v1/post/{postId}` | `getPost(head, postId)` | HOME_FEED (refresh one card), VIEW_POST |
| `GET api/v1/favorites/{userId}` | `getMyFav` / `getFav` | HOME_FEED (`type="fav"`), FAVORITES |
| `GET api/v1/like/user/{userId}` | `getUserLikes` | HOME_FEED (my reactions map) |
| `GET api/v1/like/post/{postId}` | `getPostLike` | VIEW_LIKES |
| `GET api/v1/follows/{userId}` | `getFollowing` | HOME_FEED (follow-state map) |
| `GET api/v1/users/{userId}` | `getUser` | HOME_FEED (warrior check) |
| `POST api/v1/like` | `postCallHead(head, "like", body)` | HOME_FEED |
| `POST api/v1/favorites` | `postFav` | HOME_FEED |
| `POST api/v1/follows` | `postFollow` | HOME_FEED |
| `POST api/v1/post` | `postCallHead(head, "post", body)` | ADD_POST |
| `DELETE api/v1/post/{postId}` | `deletePost` | HOME_FEED (profile mode) |

### ⚠ The `results` envelope (FEED-specific)

AUTH screens read `response.body().get("result")`. **Every list endpoint in FEED reads
`get("results")` — plural — plus a sibling `count` field** used for pagination:

```kotlin
val arr: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
count = Integer.parseInt(resp?.get("count").toString()) / 10
```

Single-object responses still use `result` (singular). Mixing them up yields an empty list with no
error (team issue F-2).

---

## 6. Shared state touched by this team

| State | Who | Note |
|---|---|---|
| `Util.isWarrior` | read | gates the create-post FAB |
| `Util.userId` | read | sent in like/fav/follow bodies |
| `Util.user` | written by `HomeFragment.getMyDetails` | the **4th** copy of the triplicated `getMyDetails` (AUTH A4) |
| `Util.fontSize` | read | applied to post content and tags |
| `Util.listview` | read/written | list vs grid toggle |
| **`Util.player`** | read/written | the **static `MediaPlayer`** for inline audio; `HomeFragment.onPause`/`onDestroy` stop, reset, release and null it |
| DataStore `token` | read | observed per call |

---

## 7. Cross-team dependencies

| Needs | Owner team | Escalate when |
|---|---|---|
| any endpoint/body change | PLATFORM / NETWORK | always |
| `Posts` / `PostUser` / `FavPost` / `PostLikes` fields | PLATFORM / DATA_MODELS | always |
| `HomeAdapter` behaviour or constructor | **PROFILE + SEARCH** | always — 3 external screens instantiate it |
| author profile navigation | PROFILE / VIEW_PROFILE | when the extra keys change |
| reaction strings `react1`–`react14`, `react_menu.xml` | APPSHELL / THEMING | always |
| bottom-nav hosting of `HomeFragment` | APPSHELL / MAIN_NAV | always |
| audio/video playback beyond the inline player | MEDIA | when `Util.player` semantics change |

---

## 8. Definition of done (team-level)

- [ ] Change tested against **all four `page` values**, not just `"home"`.
- [ ] If `HomeAdapter` changed, PROFILE and SEARCH notified via PM.
- [ ] List parsing still reads **`results`** (plural) and `count`.
- [ ] `Commons().isNetworkAvailable` guard retained; the dialog dismissed on every path.
- [ ] `Util.player` still released in `onPause`/`onDestroy` if audio code was touched.
- [ ] Module `.md` updated in the same change, including its Change log.

---

## 9. Team-level known issues

| # | Issue | Module | Risk |
|---|---|---|---|
| F-1 | `page`/`type` are magic strings with no constants, compared via `contentEquals` | HOME_FEED | silent mis-render |
| F-2 | List endpoints use `results` + `count`, unlike AUTH's `result` | all | empty lists, no error |
| F-3 | `HomeAdapter` (680 lines) holds **all** post actions — like, fav, follow, delete, share, playback — instead of the screen | HOME_FEED | untestable, cross-team coupling |
| F-4 | The always-true `!isEmpty \|\| !equals("null") \|\| !isNullOrEmpty` token guard appears **5 times** in `HomeFragment` alone | HOME_FEED | dead logout branches |
| F-5 | A new `authToken` observer is registered per call, in 5 methods | HOME_FEED | duplicate requests |
| F-6 | `dialog.hide()` is used instead of `dismiss()` in FEED callbacks | HOME_FEED | leaked window on config change |
| F-7 | Error branches are **commented out**; failures just show the "no data" view | HOME_FEED | indistinguishable empty vs failed |
| F-8 | Pull-to-refresh calls `detach().attach()` on the fragment — a full recreate | HOME_FEED | jank, duplicate calls |
| F-9 | A new `OnScrollListener` is added on **every** page load, never removed | HOME_FEED | listeners accumulate |
| F-10 | `Util.player` is a static `MediaPlayer` shared by every card | HOME_FEED, MEDIA | leaks, overlapping audio |
| F-11 | 14 reaction strings are addressed by `R.id.reactN` -> `R.string.reactN` pairs with no data structure | HOME_FEED | fragile |
| F-12 | `Posts.likesCount` / `shareCount` are `String` | all | conversion crashes |
| F-13 | `getMyDetails` duplicated here as well — the **4th** copy | HOME_FEED | drift (AUTH A4) |

---

## 10. Change log

| Change | Detail |
|---|---|
| Created | Initial FEED team lead agent: charter, 6 modules, the `page`/`type` multiplexer table, flows, 12-endpoint API surface, the `results`-vs-`result` envelope difference, shared state (incl. the static `Util.player`), cross-team rules for the shared `HomeAdapter`, and 13 team-level issues. |
| HOME_FEED ready | `HOME_FEED.md` written and verified against `HomeFragment.kt` + `HomeAdapter.kt` + both layouts. |
| FEED team complete | `ADD_POST.md`, `VIEW_POST.md`, `VIEW_LIKES.md`, `FAVORITES.md` and `IMAGE_DETAIL.md` written and verified. Key findings escalated to PM: **`ViewPostActivity` has no caller and initialises 14 views in its constructor before `setContentView` (guaranteed NPE)**; `FavoritesActivity` is a host shell whose content is entirely `HOME_FEED`'s; `IMAGE_DETAIL`'s layout was **mis-attributed** — `preview_image.xml` belongs to `MainActivity` (APPSHELL). `VIEW_LIKES` is recorded as the **reference implementation** for the correct `&&` token guard. **All 6 FEED module agents are READY.** |


