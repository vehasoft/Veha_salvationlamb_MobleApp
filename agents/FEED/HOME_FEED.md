# HOME_FEED Module Agent

> Team: **FEED** · Reports to: `agents/FEED/FEED_LEAD.md`
> This agent knows **every detail** of the home timeline: the fragment, the 680-line adapter, the
> post card, all four post types, the 14-reaction menu, every API call, and the two multiplexer
> strings that make this component serve four different screens.
> It may only edit the files listed in §13 **Owned files**.

---

## ⚠ Shared-component warning

`HomeAdapter` is **instantiated by three screens outside FEED**:

| Screen | Team | `page` value |
|---|---|---|
| `ProfileFragment` | PROFILE | `"profile"` |
| `ViewProfileActivity` | PROFILE | `"OtherProfile"` |
| `SearchProfileFragment` | SEARCH | `"searchProfile"` |

**Any change to `HomeAdapter.kt` or `child_post.xml` is a cross-team change.** Escalate to PM so
PROFILE and SEARCH can review. Always verify a change against **all four** `page` values.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **Home** (the main timeline) |
| Classes | `com.veha.fragments.HomeFragment` (528 lines) + `com.veha.adapter.HomeAdapter` (680 lines) |
| Layouts | `fragment_home.xml` (container), `child_post.xml` (the card, 26 ids) |
| Hosted by | `MainActivity` bottom nav [APPSHELL] via `TabAdapter`, and `FavoritesActivity` |
| Instantiation | `HomeFragment.getInstance(type)` — `type` goes into the arguments `Bundle` |
| View access | **`findViewById`** in the fragment; the adapter uses a `ViewHolder`. Note it also wildcard-imports `kotlinx.android.synthetic.main.activity_main.*` — an **unused, wrong-layout import** (issue H1) |
| Progress | `SpotsDialog`, "Please Wait", dismissed in `onCreateView` immediately after creation |

### The two multiplexers

| Variable | Values | Effect |
|---|---|---|
| `type` (fragment arg) | `"user"`, `"fav"` | which endpoint loads the list |
| `page` (adapter ctor) | `"home"`, `"profile"`, `"OtherProfile"`, `"searchProfile"` | which card buttons are visible |

`HomeFragment` always passes `page = "home"`; the other three values come from PROFILE/SEARCH.

---

## 2. UI inventory

### `fragment_home.xml` — 5 ids

| View id | Type | Purpose |
|---|---|---|
| `refresh` | `SwipeRefreshLayout` | pull-to-refresh (enabled only at scroll top) |
| `scroll` | scroll container | wraps the list |
| `list` | `RecyclerView` | the post list, `LinearLayoutManager` |
| `no_data` | `LinearLayout` | empty state, toggled against `list` |
| `add_post` | `FloatingActionButton` | create post — **visible only when `Util.isWarrior`** |

### `child_post.xml` — 26 ids (the post card)

| Group | View ids |
|---|---|
| Card root | `child_post_layout` |
| Header | `head_linear`, `profile_pic`, `name_post`, `post_time`, `post_full_time`, `follow_post_btn` |
| Body | `post_layout`, `title`, `tags`, `post_content` |
| Image | `post_pic` |
| Video | `post_video` |
| Audio | `audio_layout`, `pause_btn`, `play_btn`, `seekBar` |
| Reactions | `react_btn`, `no_of_reacts` |
| Actions | `child_btn_layout`, `like_btn`, `share_btn`, `fav`, `save_txt`, `Delete_btn` |

> `Delete_btn` is the only **capitalised** id in the project (issue H2).

### Button visibility by `page` (set in `onCreateViewHolder`)

| `page` | `follow_post_btn` | `Delete_btn` | `fav` + `save_txt` |
|---|---|---|---|
| `"home"` | VISIBLE | GONE | VISIBLE |
| `"profile"` | GONE | VISIBLE | GONE |
| `"OtherProfile"` | GONE | GONE | GONE |
| `"searchProfile"` | GONE | GONE | GONE |

**There is no `else` branch** — an unrecognised `page` leaves whatever the XML declared (issue H3).

### Text sizing

`title` is hard-coded to `20F`; `content` and `tags` use **`Util.fontSize`** (PLATFORM/COMMONS);
`tags` is tinted `@color/primary_blue`.

---

## 3. The four post types (`onBindViewHolder`, `when (post.type)`)

| `post.type` | Shown | Hidden | Mechanism |
|---|---|---|---|
| `"image"` | `post_pic` | `audio_layout`, `post_video` | Picasso loads `post.picture`; tap -> `ImageDetailActivity` |
| `"audio"` | `audio_layout` (`play_btn`, `pause_btn`, `seekBar`) | `post_pic`, `post_video` | **`Util.player`** (static `MediaPlayer`) + a `Runnable` polling the seek bar |
| `"video"` | `post_video` | `post_pic`, `audio_layout` | `androidyoutubeplayer` `YouTubePlayerView`, `onReady` callback |
| anything else | text only | all media | falls through — no `else` branch logs the unknown type (issue H4) |

`post.type` is a free-form `String` from the server; the three known values are matched literally.

### Audio playback

Uses the **global static `Util.player`**, so:

- only one audio post can play at a time (arguably intended),
- the seek-bar `Runnable` keeps posting while the card scrolls off screen,
- `HomeFragment.onPause` and `onDestroy` both `stop()`, `reset()`, `release()` and null it.

This is the app's only inline audio player and the direct cause of team issue F-10 (issue H5).

---

## 4. Actions on the card (all implemented inside `HomeAdapter`)

| Trigger | Behaviour |
|---|---|
| `like_btn` | opens a `PopupMenu` inflated from `R.menu.react_menu` with **14 reactions**; each `R.id.reactN` maps to `R.string.reactN` and calls `likePost(post, reaction, holder)` |
| `react_btn` / `no_of_reacts` | `ViewLikesActivity` with the post id |
| `share_btn` | `Intent.ACTION_SEND` via `Intent.createChooser(..., "choose one")` |
| `fav` / `save_txt` | `favPost(userId, postId, holder)` -> `POST favorites`; the server replies with a `message` of `"fav"` or otherwise, toggling the icon |
| `follow_post_btn` | `follow(userId, followerId, holder)` -> `POST follows` |
| `Delete_btn` | `deletePost(post)` -> `DELETE post/{id}` (profile mode only) |
| `profile_pic` / `name_post` | `ViewProfileActivity` [PROFILE] |
| `post_pic` | `ImageDetailActivity` |
| `post_content` | `holder.content.expand()` — the rich-text expander |

### The 14 reactions

`react1`–`react14` in `strings.xml`, e.g. *"Praise the LORD"*, *"Praise the LORD, O my soul"*,
*"LORD, please heal me"*, *"LORD, please strengthen me"*, *"LORD, please save me"*,
*"LORD, please comfort me"*, *"LORD, have mercy on me"*. A 15th (*"My Mother, please pray for me"*)
is **commented out** in `strings.xml`.

The menu handler is a **14-branch `when` on `R.id.reactN`**, each line calling `likePost` with
`getString(R.string.reactN)`. Adding a reaction means editing `react_menu.xml`, `strings.xml` **and**
this `when` (issue H6).

---

## 5. API calls

### 5.1 `HomeFragment` — 5 methods, all following the same shape

| Method | Endpoint | Purpose | Envelope |
|---|---|---|---|
| `getallPosts(context, owner, postlist)` | `GET post?page=N&size=10` | the timeline, paged | `results` + `count` |
| `getfavPosts(context, owner)` | `GET favorites/{userId}` | `type="fav"` list | `results`, each item parsed as `FavPost` then `.posts` extracted |
| `getallLikes(owner)` | `GET like/user/{userId}` | builds `myLikesMap` | `results` |
| `getallFollowers(owner)` | `GET follows/{userId}` | builds `myFollowMap` | `results` |
| `getallFav(owner)` | `GET favorites/{userId}` | builds `myFavMap` | `results` |
| `getMyDetails(owner)` | `GET users/{userId}` | warrior check -> `Util.user`, `userType`, `showCreatePost` | `result` (singular) |

Every one of them:
1. guards with `Commons().isNetworkAvailable(...)`,
2. shows the SpotsDialog,
3. **registers a new `userPreferences.authToken.asLiveData().observe(owner)`** (issue H7),
4. uses the always-true token guard `!isEmpty || !equals("null") || !isNullOrEmpty` (issue H8),
5. on non-200 **shows the `no_data` view with the error parsing commented out** (issue H9),
6. calls `dialog.hide()` — not `dismiss()` (issue H10).

The `else` branch of the token guard (unreachable) toasts
`"Somthing Went Wrong \nLogin again to continue"`, deletes the token + userId, and opens
`LoginActivity` — the standard logout contract, which **can never execute here**.

### 5.2 Pagination

```kotlin
count = Integer.parseInt(resp?.get("count").toString())
count /= 10                                  // total pages
...
list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrollStateChanged(rv: RecyclerView, dx: Int) {
        if (!rv.canScrollVertically(1)) {    // hit the bottom
            if (count > page) { page++; getallPosts(context, owner); updated = false }
        }
    }
})
```

| Fact | Detail |
|---|---|
| Page size | hard-coded **10** in both the request and the divisor |
| Start page | `page = 1` (set in `onCreateView`), though the empty check tests `page == 0` (issue H11) |
| Guard | an `updated` boolean stops double-appends |
| **Bug** | a **new** `OnScrollListener` is added on every successful page load and never removed, so listeners accumulate (issue H12) |
| `count` parse | `Integer.parseInt(resp.get("count").toString())` — `toString()` on a `JsonElement` keeps quotes for strings; works only because `count` is a JSON number (issue H13) |

### 5.3 `HomeAdapter` — 5 more methods

| Method | Endpoint | Note |
|---|---|---|
| `likePost(post, reaction, holder)` | `POST like` via `postCallHead` | updates the count in place |
| `favPost(userId, postId, holder)` | `POST favorites` | branches on the server `message` being `"fav"` |
| `follow(userId, followerId, holder)` | `POST follows` | toggles the button label |
| `deletePost(post)` | `DELETE post/{postId}` | profile mode |
| `getPost(postId)` | `GET post/{postId}` | refreshes a single card after an action |

So the adapter performs **network I/O directly**, holding its own `UserPreferences` and `SpotsDialog`
created in `onCreateViewHolder` (issue H14).

---

## 6. State maps

Three `HashMap<String, String>` are built in the fragment and **passed into the adapter** so each
card can render its own state without extra calls:

| Map | Built by | Used for |
|---|---|---|
| `myLikesMap` | `getallLikes` | has the user reacted, and with what |
| `myFollowMap` | `getallFollowers` | is the author already followed |
| `myFavMap` | `getallFav` | is the post saved |

They are created **empty in `onCreateView`** and filled asynchronously, while the adapter is also
constructed in `onCreateView` with those same (still empty) references. Correct rendering therefore
depends on the maps being mutated in place before the first bind — a race, not a guarantee
(issue H15).

---

## 7. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read (observed, 5 separate observers) |
| DataStore `token` + `userId` | deleted in the unreachable logout branch |
| `Util.isWarrior` | read -> FAB visibility |
| `Util.userId` | read -> request bodies |
| `Util.user` | **written** by `getMyDetails` |
| `Util.fontSize` | read -> content/tag text size |
| `Util.player` | read/written -> audio playback |

---

## 8. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | `super` only |
| `onCreateView` | reads `type`; creates the 3 maps; inflates; builds `UserPreferences` + SpotsDialog (then **immediately dismisses** it); binds the 4 views; FAB visibility from `Util.isWarrior`; calls `getMyDetails` and `getallLikes`; sets `page = 1`; creates the adapter with an **empty** list; attaches the scroll listener that manages pull-to-refresh |
| `onPause` | `dialog.dismiss()` + stop/reset/release/null `Util.player` |
| `onResume` | `dialog.dismiss()` |
| `onDestroy` | `dialog.dismiss()` + stop/reset/release/null `Util.player` |

**Pull-to-refresh** does not re-fetch; it runs
`requireFragmentManager().beginTransaction().detach(this).attach(this).commit()` — a full fragment
recreate using the **deprecated** `requireFragmentManager()` (issue H16).

Note `getallPosts` is **not** called from `onCreateView`; the list is populated by whichever caller
drives it (`FavoritesActivity` calls `getfavPosts`, the tab host calls `getallPosts`).

---

## 9. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `RetrofitAPI` (7 endpoints) + `Util.getRetrofit()` | PLATFORM / NETWORK | all calls |
| `Posts`, `PostUser`, `FavPost`, `PostLikes`, `UserRslt` | PLATFORM / DATA_MODELS | parsing |
| `UserPreferences` | PLATFORM / STORAGE | token |
| `Util.*`, `Commons()` | PLATFORM / COMMONS | globals, guard, `getVideo`, `getTimeAgo` |
| Picasso | PLATFORM / BUILD_CONFIG | images |
| `androidyoutubeplayer` | PLATFORM / BUILD_CONFIG | video |
| `richeditor-android` | PLATFORM / BUILD_CONFIG | expandable content |
| `react_menu.xml`, `react1`–`react14` | APPSHELL / THEMING | reactions |
| `ImageDetailActivity`, `ViewLikesActivity`, `AddPostActivity` | FEED siblings | navigation |
| `ViewProfileActivity` | PROFILE | author profile |
| `LoginActivity` | AUTH | forced re-login target |

---

## 10. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| H14 | The adapter does **network I/O** and owns a `UserPreferences` + dialog; 680 lines mixing view binding, playback and 5 API calls | **High** | move calls to the fragment/ViewModel |
| H12 | A new `OnScrollListener` is added on **every** page load, never removed | **High** | add it once in `onCreateView` |
| H7 | 5 `authToken` observers, one per method, re-firing on every DataStore write | **High** | one-shot read |
| H8 | The always-true token guard appears 5x; the logout branch is dead | **High** | use `&&` |
| H9 | Error parsing is commented out — a failed load is indistinguishable from an empty feed | **High** | restore it and show a distinct error state |
| H5 | Audio uses the static `Util.player` plus a polling `Runnable` not tied to the card lifecycle | **High** | per-holder player, release on recycle |
| H17 | Retrofit calls are never cancelled; callbacks touch views after detach | **High** | cancel in `onDestroyView` |
| H16 | Pull-to-refresh does `detach().attach()` via the deprecated `requireFragmentManager()` | Medium | re-fetch the data instead |
| H15 | The three state maps are passed **empty** to the adapter and filled asynchronously | Medium | pass immutable data after load |
| H10 | `dialog.hide()` instead of `dismiss()` | Medium | use `dismiss()` |
| H11 | `page` starts at 1 but the empty check tests `page == 0`, so a genuinely empty first page never shows `no_data` | Medium | align the values |
| H3 | No `else` in the `page` visibility chain | Medium | add a default |
| H4 | No `else` in the `post.type` `when` | Medium | log/handle unknown types |
| H6 | Adding a reaction requires editing 3 places; the handler is a 14-branch `when` | Medium | drive it from a list |
| H18 | `type` is read with `arguments?.get("type").toString()`, so a missing arg becomes the string `"null"` | Medium | same trap as AUTH A13 |
| H19 | `getMyDetails` here is the **4th** copy of the same block | Medium | shared helper (PM-level) |
| H1 | `HomeFragment` imports `kotlinx.android.synthetic.main.activity_main.*` — the **wrong layout**, unused | Low | delete the import |
| H13 | `Integer.parseInt(get("count").toString())` is fragile | Low | use `asInt` |
| H2 | `Delete_btn` is capitalised, unlike every other id | Cosmetic | rename |

---

## 11. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/fragments/HomeFragment.kt` | **exclusive** |
| `app/src/main/java/com/veha/adapter/HomeAdapter.kt` | **owned, but shared-use** — PROFILE and SEARCH instantiate it; notify via PM |
| `app/src/main/res/layout/fragment_home.xml` | **exclusive** |
| `app/src/main/res/layout/child_post.xml` | **owned, but shared-use** — rendered by all four `page` modes |

**Not owned (escalate):** `RetrofitAPI.kt`, `DataModels.kt`, `UserPreferences.kt`, `Util.java`,
`Commons.kt`, `react_menu.xml`, `strings.xml`, and the PROFILE/SEARCH screens that host this adapter.

---

## 12. How to make common changes

**Add a field to the post card:** add the view to `child_post.xml`, bind it in the `ViewHolder`, set
it in `onBindViewHolder`. **Decide its visibility for all four `page` values** and add it to the §2
table. Cross-team — notify PROFILE and SEARCH via PM.

**Add a reaction:** add the item to `react_menu.xml` (APPSHELL), the string to `strings.xml`
(APPSHELL), and a branch to the `when` in `HomeAdapter`. Three files, two teams — PM first.

**Change the page size:** update both the `getPost(..., page, 10)` argument and the `count /= 10`
divisor — they must match, and the `10` is currently written twice.

**Add a new post type:** add a branch to the `when (post.type)` in `onBindViewHolder`, show/hide the
right containers, and confirm the server's `type` value with PLATFORM/NETWORK. Also update
`ADD_POST.md` so the type can actually be created.

**Fix the scroll-listener leak (H12):** move `addOnScrollListener` out of the response callback into
`onCreateView` and read `count`/`page` from fields. Local to this agent.

**Touching audio:** `Util.player` is PLATFORM-owned global state used by MEDIA too — coordinate
before changing its lifecycle.

---

## 13. Change log

| Change | Detail |
|---|---|
| Created | Initial HOME_FEED module agent documented from `HomeFragment.kt` (528 lines), `HomeAdapter.kt` (680 lines), `fragment_home.xml` (5 ids) and `child_post.xml` (26 ids): both multiplexers with their 2 and 4 values, the per-`page` button-visibility matrix, all four post types, the 14-reaction menu, 11 API calls across fragment and adapter, the `results`+`count` pagination with its listener leak, the three async state maps, the full lifecycle incl. `Util.player` handling, and 19 known issues. |



