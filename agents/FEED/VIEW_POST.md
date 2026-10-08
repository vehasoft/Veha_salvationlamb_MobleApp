# VIEW_POST Module Agent

> Team: **FEED** · Reports to: `agents/FEED/FEED_LEAD.md`
> This agent knows **every detail** of the single-post screen — including the fact that it is
> **currently unreachable and would crash on launch**.
> It may only edit the files listed in §10 **Owned files**.

---

## 🚨 Status: DEAD + BROKEN — read before any work

Two verified facts about this screen:

1. **Nothing launches it.** A repository-wide search for `ViewPostActivity` returns exactly **one**
   hit outside its own file: the `<activity>` entry in `AndroidManifest.xml`. No screen, no adapter,
   no deep link ever starts it. (`HomeAdapter` shows posts inline; the reaction count opens
   `ViewLikesActivity`, not this.)
2. **It would crash immediately if it were launched.** All 14 views are initialised as **property
   initialisers**, which run in the constructor — **before `onCreate` calls `setContentView`**:

```kotlin
class ViewPostActivity : AppCompatActivity() {
    val name: TextView = findViewById(R.id.name_post)   // runs in the constructor
    ...
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_view_post)     // too late
```

At construction there is no content view, so `findViewById` returns `null`, and assigning `null` to
a non-null `TextView` throws **`NullPointerException` before `onCreate` ever runs**.

**Treat this screen as unverified scaffolding.** Do not cite it as a working example, and do not
copy its patterns. Any task here starts by moving the 14 lookups into `onCreate`.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name | **View Post** (single post detail) |
| Class | `com.veha.activity.ViewPostActivity` (102 lines) |
| Layout | `app/src/main/res/layout/activity_view_post.xml` |
| Manifest | `<activity android:name=".ViewPostActivity" android:exported="false"/>` |
| View access | **`findViewById` at property-initialiser level** — the defect above |
| Entered from | **nothing** — no caller exists |
| Required extra | `postId` (read as `intent.extras!!.get("postId").toString()`) |
| Exits to | `MainActivity` (logo tap) |
| Progress | `SpotsDialog`, "Please Wait" |

---

## 2. UI inventory (14 declared views)

| View id | Type | Populated? |
|---|---|---|
| `prod_logo` | `ImageView` | — (tap -> `MainActivity`) |
| `head_linear` | `LinearLayout` | never used |
| `profile_pic` | `ImageView` | **never set** |
| `name_post` | `TextView` | **never set** |
| `post_time` | `TextView` | `Util.getTimeAgo(post.createdAt)` |
| `tags` | `TextView` | `post.tags` — **raw**, not `#`-prefixed |
| `post_content` | `TextView` | `post.content` |
| `no_of_reacts` | `TextView` | `"${post.likesCount} people reacts"` |
| `post_pic` | `ImageView` | **never set** |
| `like_pic` | `ImageView` | **never set** |
| `react_btn` | `LinearLayout` | **no listener** |
| `post_layout` | `LinearLayout` | never used |
| `like_btn` | `Button` | **no listener** |
| `share_btn` | `Button` | **no listener** |
| `fav` | `ImageButton` | **no listener** |

**Only 4 of 14 views are ever populated**, and only **one** (`prod_logo`) has a listener. The
author, image, like, share and favourite controls are inert (issue VP1).

The ids mirror `child_post.xml`, so this layout is a flattened copy of the feed card, but the
behaviour was never ported (issue VP2).

---

## 3. Actions

| Trigger | Behaviour |
|---|---|
| `prod_logo` tap | `startActivity(MainActivity)` — **no `finish()`** (issue VP3) |
| everything else | **nothing** |

No Back override, no toolbar, no `finish()` affordance other than the system Back.

---

## 4. API contract

| Item | Value |
|---|---|
| Retrofit | `getPost("Bearer $token", postId)` — the **overload** by path, not the paged one (NETWORK N5) |
| HTTP | `GET api/v1/post/{postId}` |
| Guard | `Commons().isNetworkAvailable(this)` |
| Token | observed via `authToken.asLiveData()` |
| Success | `Gson().fromJson(body.get("result"), Posts::class.java)` — **`result` singular**, unlike FEED's list endpoints which use `results` |

**Populated on 200:** `tags`, `post_time`, `post_content`, `no_of_reacts`. Nothing else.

**Error (non-200):** `Log.e("fail fav", response.errorBody().toString())` — a copy-pasted tag from
the favourites code, and `errorBody().toString()` prints the **object reference**, not the body
(issue VP4). The real parsing is commented out. **No user feedback.**

**`onFailure`:** `Log.e("ViewPost", "fail")` only — and it does **not** dismiss the dialog, so the
non-cancelable spinner stays on screen forever (issue VP5).

### The one thing this screen does right

Its token guard uses **`&&`**:

```kotlin
if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty())
```

This is the **only** correct version in the entire codebase — every other screen uses `||`, making
the guard always true (AUTH A19, COMMONS C-13, HOME_FEED H8). Note the consequence: here the
condition can legitimately be **false**, and there is **no `else`**, so a missing token silently
does nothing (issue VP6).

---

## 5. Storage & global state

| Item | Operation |
|---|---|
| DataStore `token` | read (observed) |
| `Util.getTimeAgo` | read-only helper |
| — | nothing written |

---

## 6. Lifecycle

| Callback | Behaviour |
|---|---|
| *(constructor)* | 14 `findViewById` calls — **crashes here** |
| `onCreate` | `setContentView` -> `UserPreferences` -> SpotsDialog -> logo listener -> read `postId` -> `getPost(postId)` |
| `onDestroy` | `dialog.dismiss()` |

`intent.extras!!.get("postId").toString()` double-traps: `extras!!` throws if the activity was
started with no extras at all, and `.toString()` on a missing key yields the literal `"null"`
(issue VP7) — the same pattern as AUTH A13.

---

## 7. Dependencies

| Dependency | Owner | Used for |
|---|---|---|
| `RetrofitAPI.getPost(head, postId)` | PLATFORM / NETWORK | the fetch |
| `Posts` | PLATFORM / DATA_MODELS | parsing |
| `UserPreferences` | PLATFORM / STORAGE | token |
| `Util.getTimeAgo`, `Commons()` | PLATFORM / COMMONS | formatting + guard |
| `MainActivity` | APPSHELL / MAIN_NAV | logo destination |

---

## 8. Known issues / tech debt

| # | Issue | Severity | Fix |
|---|---|---|---|
| VP0 | **All 14 `findViewById` calls run in the constructor, before `setContentView`** — guaranteed NPE on launch | **Critical** | move them into `onCreate` after `setContentView` |
| VP8 | **No caller anywhere** — the screen is unreachable dead code | **High** | wire it up from `HomeAdapter`, or delete it |
| VP1 | Only 4 of 14 views are populated; like, share, fav and react have **no listeners** | **High** | implement, or remove the controls |
| VP5 | `onFailure` never dismisses the non-cancelable dialog — permanent spinner | **High** | dismiss in both callbacks |
| VP4 | Error logging uses the tag `"fail fav"` and prints `errorBody().toString()` (an object reference) | Medium | use `.string()` and a correct tag |
| VP7 | `intent.extras!!.get("postId").toString()` — crashes with no extras, yields `"null"` with a missing key | Medium | `intent.getStringExtra("postId")` + validation |
| VP6 | The (correct) `&&` guard has **no `else`**, so a missing token silently does nothing | Medium | add the standard logout branch |
| VP2 | The layout duplicates `child_post.xml` ids without the behaviour | Medium | reuse the card, or delete |
| VP11 | No image/audio/video handling at all, unlike the feed card | Medium | port `when (post.type)` |
| VP3 | The logo navigates to `MainActivity` without `finish()` | Low | `finish()` |
| VP9 | `tags` is shown raw, not via `HomeAdapter.getTags()`'s `#tag` formatting | Low | reuse the helper |
| VP10 | `"${post.likesCount} people reacts"` — grammar, and no singular form | Low | pluralise |

---

## 9. Verdict for the PM

This screen is **not shippable as written** and **not reachable**, so it is currently harmless. Two
clean options:

1. **Delete it** (`ViewPostActivity.kt`, `activity_view_post.xml`, the manifest entry) — removes 102
   lines of broken code plus a layout.
2. **Revive it** — fix VP0, wire a caller in `HomeAdapter` (post-body tap -> `postId` extra),
   populate the remaining 10 views and port the card's actions.

Either is a **PM decision**; this agent must not choose unilaterally.

---

## 10. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/ViewPostActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_view_post.xml` | **exclusive** |
| `<activity android:name=".ViewPostActivity">` in the manifest | shared with PLATFORM/BUILD_CONFIG |

**Not owned (escalate):** `RetrofitAPI.kt`, `DataModels.kt`, `UserPreferences.kt`, `Util.java`,
`Commons.kt`, `HomeAdapter.kt` (HOME_FEED — would need the new launch point).

---

## 11. How to make common changes

**Make it work at all (VP0):** move all 14 `findViewById` calls out of the property initialisers and
into `onCreate` **after** `setContentView`, declaring them `private lateinit var`. Nothing else in
this screen can be tested until this is done.

**Give it a caller (VP8):** add a click listener in `HomeAdapter` (likely on `post_layout` or the
title) starting `ViewPostActivity` with a `postId` extra. That edits `HomeAdapter.kt`, owned by
`HOME_FEED.md` and shared with PROFILE/SEARCH — **two-agent change, PM first**.

**Populate the remaining views:** `profile_pic` and `name_post` come from `post.user` (`PostUser`),
`post_pic` from `post.picture` via Picasso. Follow `HOME_FEED.md` §3 for the per-type logic.

**Implement like/share/fav:** the logic already exists in `HomeAdapter.likePost` / `favPost` and the
`ACTION_SEND` chooser. Reuse rather than re-implement — coordinate with HOME_FEED so the two stay in
sync.

---

## 12. Change log

| Change | Detail |
|---|---|
| Created | Initial VIEW_POST module agent documented from `ViewPostActivity.kt` (102 lines). Recorded two blocking facts verified against the codebase: **no caller exists** (only the manifest references the class) and **all 14 `findViewById` calls run in the constructor before `setContentView`**, guaranteeing an NPE on launch. Catalogued all 14 views (only 4 populated, 1 listener), the `GET post/{postId}` call, the uniquely-correct `&&` token guard, and 12 known issues plus a delete-or-revive recommendation for the PM. |


