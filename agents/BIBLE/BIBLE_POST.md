# BIBLE_POST Module Agent

> Team: **BIBLE** · Reports to: `agents/BIBLE/BIBLE_LEAD.md`
> Scope: this agent knows **every detail** of this screen — views, actions, API, storage,
> validation, messages, navigation. It may only edit the files under **Owned files**.

---

## 1. Identity

| Item | Value |
|---|---|
| Screen name (user-facing) | **Bible Post** — preview a selected passage on a coloured card, pick a colour, publish it to the feed |
| Class | `com.veha.activity.BiblePostActivity` (173 LOC, Kotlin, `AppCompatActivity`) |
| Layout | `res/layout/activity_bible_post.xml` (174 LOC) — `ScrollView` → `LinearLayout` |
| Manifest entry | `<activity android:name=".BiblePostActivity" android:exported="false" />` — no theme, no `configChanges`, no orientation lock (unlike `BibleActivity`) |
| Entered from | **`BibleActivity` only** — `@id/post_txt` tap with a non-empty selection |
| Exits to | `MainActivity` (logo tap, and after a 200 response) · `BibleActivity` (system Back) · `LoginActivity` (dead session-lost branch, `BP2`) |

**One-line summary:** a preview-and-publish screen. It receives the passage as three intent
extras, lets the user pick one of five background colours, and `POST`s a `type = "bible"` post.
It is the **only** network call in the BIBLE team.

## 2. UI inventory

| View id | Type | Text / hint | Notes |
|---|---|---|---|
| `@id/header_main` | `ConstraintLayout`, `@color/primary_blue` | — | header bar; not bound |
| `@id/prod_logo` | `ImageView` 200×50dp, `@drawable/ic_sl_logo_01_svg` | — | → `logo`; tap → `MainActivity` **without `finish()`** (`BP6` = `B-17`) |
| — (no id) | `TextView`, 20dp bold `@color/black` | **`"Bible Post"`** | hard-coded screen title (`BP11`) |
| `@id/bible_cons_layout` | `ConstraintLayout`, `android:background="@color/color1"` | — | → `bibleLayout`; the preview card. Its background is re-set imperatively by the swatches. XML default `@color/color1` = `#25B567`, matching the `colorCode` default |
| — (child, no id) | `ImageView` `@drawable/bible_left` | — | `visibility="gone"` — dead decoration |
| — (child, no id) | `ImageView` `@drawable/bible_right` | — | `visibility="gone"` — dead decoration |
| `@id/bible_title` | `TextView`, 20dp `@color/always_white` | tools text `"nbscvzdmvcdgjvcgh"` | → `title` (field). **`visibility="gone"` and never assigned** — the computed title is a *local* `val` that shadows the field (`BP4`). The placeholder junk string ships in the APK (`BP11`) |
| `@id/bible_tags` | `TextView`, 15dp `@color/always_white` | — | → `tags`; set to the `tags` extra (`"<book>, <chapter>"`) |
| `@id/bible_content` | `TextView`, 15dp `@color/always_white`, `paddingHorizontal=40dp` | — | → `content`; set to the `content` extra (the verses). **Fixed size, ignores `Util.fontSize`** (`BP10`) |
| `@id/col1` | `LinearLayout` 60dp, `weight=1`, `@color/color1` | — | swatch → `colorCode = "#25B567"` (green) |
| `@id/col2` | `LinearLayout` 60dp, `weight=1`, `@color/color2` | — | swatch → `colorCode = "#E24B4B"` (red) |
| `@id/col3` | `LinearLayout` 60dp, `weight=1`, `@color/color3` | — | swatch → `colorCode = "#6E26A6"` (purple) |
| `@id/col4` | `LinearLayout` 60dp, `weight=1`, `@color/color4` | — | swatch → `colorCode = "#2C95E1"` (blue) |
| `@id/col5` | `LinearLayout` 60dp, `weight=1`, `@color/color5` | — | swatch → `colorCode = "#C121C1"` (magenta) |
| `@id/bible_post_btn` | `Button`, `@drawable/rounded_border_login_register`, tint `@color/primary_blue` | **`"Post"`** | → `postBtn`; disabled on tap, re-enabled **only** in `onResponse` (`BP3`) |

> **Colour duplication (`BP9`):** every swatch hex is written **three** times — in
> `values/colors.xml` (`color1`…`color5`), in `Color.parseColor("#…")` and in the `colorCode`
> assignment. `values-night/colors.xml` declares the **same five values**, so the palette is
> theme-independent by accident, not by design. FEED's renderer falls back to a hard-coded
> `#25B567` if `colorCode` is null.

### Static text (no id)

| Text | Notes |
|---|---|
| `"Bible Post"` | screen title, hard-coded in XML |
| `"Post"` | button label, hard-coded in XML |
| `"nbscvzdmvcdgjvcgh"` | placeholder text left on `@id/bible_title` (the view is `gone`) |
| `"Bible post - "` | title prefix, hard-coded in Kotlin; concatenated with `R.string.oldBible` / `newBible` (Tamil) → a mixed-language post title (`BP11`) |
| `"Somthing Went Wrong \nLogin again to continue"` | toast in the **unreachable** session-lost branch (typo, `BP2`) |

## 3. Actions / event handlers

| Trigger | Behaviour |
|---|---|
| `onCreate` | `setContentView(activity_bible_post)` → `UserPreferences(this)` → binds `logo`, `title`, `tags`, `content`, `col1`..`col5`, `postBtn`, `bibleLayout` → reads the 3 extras → computes the local `title` string → sets `tags.text` / `content.text` → wires 5 swatch listeners + the post listener |
| Read extras | `editionTxt = intent.extras!!.getString("edition").toString()`; `tagsTxt = intent.extras!!.getString("tags")`; `contentTxt = intent.extras!!.getString("content")` — **three force-unwraps** (`BP1` = `B-15`). `.toString()` on the first turns a missing value into the **string `"null"`**, not `null` |
| Compute the title | `val title = "Bible post - " + if (editionTxt == "old") getString(R.string.oldBible) else getString(R.string.newBible)` — a **local `val` shadowing the `lateinit var title: TextView` field**, so the `TextView` is never populated (`BP4`). The `else` branch means any unknown `edition` silently renders as the New Testament |
| Tap `@id/prod_logo` | `startActivity(Intent(this, MainActivity::class.java))` — no `finish()`, no flags (`BP6` = `B-17`) |
| Tap `@id/col1`..`@id/col5` | `bibleLayout.setBackgroundColor(Color.parseColor("#…"))` + `colorCode = "#…"`. No selected-state indicator on the swatches (`BP12`) |
| Tap `@id/bible_post_btn` | `postBtn.isEnabled = false`; builds the `JsonObject` payload (8 properties, see §5); calls `postData(data)` |
| `postData(data)` | `try { if (Commons().isNetworkAvailable(this)) { retrofit = Util.getRetrofit(); userPreferences.authToken.asLiveData().observe(this) { … } } } catch (e) { Log.e("AddPostActivity.postData", e.toString()) }` — **offline = total no-op with the button left disabled** (`BP3`, `BP7`) |
| `onBackPressed` | `startActivity(Intent(this, BibleActivity).putExtra("type", editionTxt).setFlags(FLAG_ACTIVITY_NEW_TASK))` **and then** `super.onBackPressed()` — launches a new reader *and* pops this activity (`BP5` = `B-16`) |

### The `postData` callback, branch by branch

| Branch | Behaviour |
|---|---|
| guard | `if (!TextUtils.isEmpty(it) \|\| !it.equals("null") \|\| !it.isNullOrEmpty())` — **always true** (cluster `CL-1`, `BP2` = `B-3`). With a missing token the request still goes out as `Bearer null` |
| `onResponse`, `code() == 200` | `postBtn.isEnabled = true`; removes `tags`, `content`, `title`, `userId` from the **already-sent** `JsonObject` (pointless, `BP13`); `startActivity(MainActivity)`; `finish()`. **The success body is never parsed** — no `result` envelope read, no post id captured |
| `onResponse`, other codes | `postBtn.isEnabled = true`; `Gson().fromJson(response.errorBody()?.string(), JsonObject::class.java)`; `Log.e("Status", …)` + `Log.e("result", …)` — **nothing shown to the user** (`BP8`). `loginresp.get("status")` NPEs on an empty or HTML error body (`BP14`) |
| `call1.cancel()` | called at the end of `onResponse` (both branches) |
| `onFailure` | `Log.e("AddPostActivity.postData", "fail")` **only** — no toast, and **`postBtn` stays disabled forever** (`BP3` = `B-12`) |
| `else` (token empty) | **dead code** (`BP2`): toast `"Somthing Went Wrong \nLogin again to continue"`, `lifecycleScope { deleteAuthToken(); deleteUserId() }`, `startActivity(LoginActivity)` — no `finish()`, no `CLEAR_TASK` |

> **Copy-paste origin:** all three `Log.e` tags read **`"AddPostActivity.postData"`** — this method
> was duplicated from `AddPostActivity` (`BP11`).

## 4. Validation rules

| Field | Rule | Failure message |
|---|---|---|
| `content` / `tags` / `edition` extras | **none** — assumed present and non-empty; `BibleActivity` guarantees a non-empty selection, nothing else does | NPE (`BP1`) |
| `colorCode` | always one of the 5 swatch literals; defaults to `"#25B567"` | n/a |
| Auth token | guard present but **always true** (`CL-1`) | none — `Bearer null` is sent |
| Network | `Commons().isNetworkAvailable(this)` — if false the whole method is skipped **silently** | none (`BP7` = `G6`) |
| Posting permission | **none** — no `Util.hasPermission(POST, Create)` check (`BP15` = `B-9`) | none |
| Double submit | `postBtn.isEnabled = false` guards the button, but the LiveData observer can re-fire the request anyway (`BP16`) | none |

## 5. API contracts

### Create bible post

| Item | Value |
|---|---|
| Retrofit method | `RetrofitAPI.postCallHead(@Header("Authorization") head: String, @Path("url") url: String, @Body dataModal: JsonObject?)` |
| HTTP | `POST api/v1/{url}` with `url = "post"` → **`POST /api/v1/post`** |
| Base URL source | `Util.getRetrofit()` → `https://server.salvationlamb.com` |
| Headers | `Authorization: Bearer <token from the authToken DataStore flow>` (can literally be `Bearer null`, `BP2`) |
| Request body | `{ "title": "Bible post - <localized testament>", "content": "<verses>", "tags": "<book>, <chapter>", "image": "", "url": "", "type": "bible", "userId": "<Util.userId>", "colorCode": "#RRGGBB" }` — **all values `String`** (`G7`) |
| Success (200) | body **ignored**; button re-enabled, 4 properties removed from the local payload, `MainActivity` started, `finish()` |
| Error (non-200) | `errorBody()` parsed as `{status, errorMessage}` and **only logged** (`Log.e("Status"/"result")`); no user feedback (`BP8`) |
| Failure (`onFailure`) | `Log.e("AddPostActivity.postData", "fail")`; **button never re-enabled** (`BP3`) |

> `Util.userId` is read straight from the static — after process death it can be `null`, in which
> case Gson writes `"userId": null` into the payload (`BP17`).

## 6. Storage read / written

| Key | Type | Operation | Value |
|---|---|---|---|
| `token` | `String` | **read** — `userPreferences.authToken.asLiveData().observe(this)` **registered inside the post click handler** (`BP16` = `B-13`, cluster `CL-8`) | used as `Bearer $it` |
| `token` | `String` | **delete** — `userPreferences.deleteAuthToken()` in the **unreachable** `else` branch | — |
| `userId` | `String` | **delete** — `userPreferences.deleteUserId()` in the same unreachable branch | — |
| `bibleBookmark` | — | **not touched** here (owned by `BIBLE_READER`) | — |

## 7. Global state touched

| Field | Operation | Value |
|---|---|---|
| `Util.userId` | **read** — `data.addProperty("userId", Util.userId)` | unguarded; `null` after process death (`BP17`) |
| `Util.getRetrofit()` | **call** | the `server.salvationlamb.com` client |
| `BibleActivity.selectedText` | **not touched** — the reader clears it before launching | — |
| `Util.permissionMap` / `Util.hasPermission` | **never consulted** (`BP15`) | — |
| `Util.fontSize` / `Util.isNight` | **never applied**; the card text is `@color/always_white` on a fixed hex, so it reads correctly in both themes by construction | — |

## 8. User-facing messages (exact strings)

| Trigger | Message | Mechanism |
|---|---|---|
| Token empty (**unreachable**, `BP2`) | `Somthing Went Wrong \nLogin again to continue` | `Toast.LENGTH_LONG` (typo: "Somthing") |
| Offline | **none** — silent no-op, button stays disabled (`BP7`, `BP3`) | — |
| Server error (non-200) | **none** — only `Log.e("Status", …)` / `Log.e("result", …)` | logcat |
| Network failure | **none** — only `Log.e("AddPostActivity.postData", "fail")` | logcat |
| Exception in `postData` | **none** — only `Log.e("AddPostActivity.postData", e.toString())` | logcat |
| Post succeeded | **none** — the screen just jumps to `MainActivity` | — |

## 9. Navigation map

| From | Trigger | To | Extras | finish()? |
|---|---|---|---|---|
| `BibleActivity` | tap `@id/post_txt` with ≥1 verse selected | **`BiblePostActivity`** | `edition` (= the reader's launch `type`, `B-5`), `content`, `tags` | no |
| `BiblePostActivity` | tap `@id/prod_logo` | `MainActivity` | none | **no** (`BP6`) |
| `BiblePostActivity` | 200 response | `MainActivity` | none | **yes** |
| `BiblePostActivity` | system Back | `BibleActivity` (new instance) + pops self | `type` = `editionTxt`, `FLAG_ACTIVITY_NEW_TASK` | `super.onBackPressed()` (`BP5`) |
| `BiblePostActivity` | token empty (**unreachable**) | `LoginActivity` | none | **no** — the user could press Back into a logged-out composer (`BP2`) |

## 10. Lifecycle

| Callback | Behaviour |
|---|---|
| `onCreate` | binding, extras, title computation, 6 listeners (lines 43–104) |
| `onBackPressed` | overridden — see above |
| `onStart` / `onResume` / `onPause` / `onStop` / `onDestroy` | **not overridden** |
| `onSaveInstanceState` / restore | **not implemented** — but the extras survive recreation, so only `colorCode` (and the preview background) is lost on rotation (`BP18`) |
| Rotation | **no `configChanges`** in the manifest (unlike `BibleActivity`) → full recreation; the chosen colour resets to `#25B567` while `colorCode` also resets, so they stay consistent — but the user's choice is silently discarded |
| Process death | `Util.userId` is not restored → `BP17` |
| LiveData observer | `observe(this)` is lifecycle-bound, so it is removed at `DESTROYED` — but it stays **active for the whole activity**, re-firing on any later `token` write (`BP16`) |

## 11. Dependencies

| Dependency | Owner team | Used for |
|---|---|---|
| `RetrofitAPI.postCallHead` + `Util.getRetrofit()` | **PLATFORM / NETWORK** | the only network call in the BIBLE team |
| `UserPreferences.authToken` / `deleteAuthToken` / `deleteUserId` | **PLATFORM / STORAGE** | bearer token, dead logout branch |
| `Util.userId` | PLATFORM / COMMONS | payload field |
| `Commons().isNetworkAvailable(ctx)` | PLATFORM / COMMONS | offline guard |
| `R.string.oldBible` / `R.string.newBible` | APPSHELL / THEMING | the post title suffix |
| `@color/color1`..`color5`, `@color/always_white`, `@color/primary_blue`, `@drawable/rounded_border_login_register`, `@drawable/bible_left` / `bible_right` (both `gone`) | APPSHELL / THEMING | palette + chrome |
| `activity/MainActivity`, `activity/LoginActivity` | APPSHELL / AUTH | navigation targets |
| `activity/BibleActivity` | **BIBLE / BIBLE_READER** | source of the extras, and the Back target |
| The `type = "bible"` post renderer (`HomeAdapter` ~lines 165–176, `ViewPostActivity`), incl. its `colorCode` fallback to `#25B567` | **FEED** | consumes everything this screen sends |

## 12. Known issues / tech debt

| # | Issue | Severity | Suggested fix |
|---|---|---|---|
| BP15 | ~~**No posting permission check** (= `B-9`)~~ — **BY DESIGN, confirmed by the customer 2026-10-08: "Anyone can post Bible."** The screen creates a post with no `Util.hasPermission(POST, Create)` gate while `HomeFragment` gates `AddPostActivity` with exactly that. Consequence to carry into RN: a user denied `POST`/`Create` **can** publish a Bible passage but **cannot** use the normal composer. Intended product rule | By design | **do not add a gate** |
| BP2 | **Always-true token guard** (cluster `CL-1`, = `B-3`): `if (!TextUtils.isEmpty(it) \|\| !it.equals("null") \|\| !it.isNullOrEmpty())`. The `\|\|` chain is `true` for every input, so the `else` (toast + clear session + go to Login) is **dead code** and a missing token produces a `Bearer null` request | High | `if (!it.isNullOrEmpty() && it != "null")`; keep the `else`, and add `finish()` + `CLEAR_TASK` to the Login intent |
| BP3 | **Post button is a dead end on every non-200 path** (= `B-12`): `isEnabled = false` on tap, re-enabled **only inside `onResponse`**. `onFailure`, the offline branch and the `catch` all leave it **permanently disabled with no message** | High | Re-enable on all 4 paths (helper / `finally`) and show a toast |
| BP16 | **Continuous LiveData observer registered inside the click handler** (cluster `CL-8`, = `B-13`): `userPreferences.authToken.asLiveData().observe(this)` sits inside `postData`, called from the button listener. Any later write to `token` re-fires the lambda and **posts the passage again**; tapping Post twice registers two observers | High | Read the token once (`authToken.first()` in a coroutine) or hoist a single observer into `onCreate` |
| BP1 | **Three `intent.extras!!` force-unwraps** (= `B-15`) for `edition`, `tags`, `content`. Launching without extras NPEs instantly; `.toString()` on the first also converts a missing value into the string `"null"`, which then flows into the post title | Medium | `intent?.extras?.getString(...)` with defaults and an early `finish()` when the passage is missing |
| BP5 | **`onBackPressed` starts `BibleActivity` with `FLAG_ACTIVITY_NEW_TASK` *and* calls `super.onBackPressed()`** (= `B-16`) — a new reader instance is pushed (possibly into a new task) while this one pops, so Back does not return to the reader the user came from and the stack grows | Medium | Just `super.onBackPressed()` — the reader is already below on the stack |
| BP4 | **Variable shadowing hides a view**: `val title = "Bible post - …"` shadows `lateinit var title: TextView`, so `@id/bible_title` is never populated. The layout masks the bug by marking that `TextView` `visibility="gone"` — the user sees **no title**, while the title sent to the server is the computed string | Medium | Rename the local to `postTitle`; either populate and show the `TextView` or delete it |
| BP7 | **Silent offline no-op** (= `G6`, `B-19`): `if (Commons().isNetworkAvailable(this))` with no `else` — tapping Post offline disables the button and does literally nothing | Medium | `else { postBtn.isEnabled = true; toast(...) }` |
| BP8 | **Non-200 responses are only logged** — no toast, no inline error; the user sees the button re-enable and nothing else | Medium | Surface `errorMessage` in a toast / snackbar |
| BP6 | Logo tap starts `MainActivity` **without `finish()` or `CLEAR_TOP`** (= `B-17`) | Medium | `finish()`, or `CLEAR_TOP \| SINGLE_TOP` |
| BP14 | `loginresp.get("status").toString()` / `get("errorMessage").toString()` without null checks — an empty body, an HTML 502 page or any non-JSON error throws inside `onResponse` | Medium | `runCatching` + `has(...)` checks; fall back to `response.code()` |
| BP17 | `data.addProperty("userId", Util.userId)` reads an unguarded static — `null` after process death, producing `"userId": null` in the payload | Medium | Read `userId` from DataStore, or validate before posting |
| BP18 | No `onSaveInstanceState`: rotation (no `configChanges` in the manifest) discards the chosen `colorCode` and resets the preview to green with no warning | Low | Persist `colorCode` in the instance state |
| BP13 | After a 200 the code removes `tags`, `content`, `title` and `userId` from the `JsonObject` that has **already been serialized and sent** — a no-op inherited from `AddPostActivity` | Low | Delete the 4 `data.remove(...)` calls |
| BP9 | Each swatch colour is written **three** times (`values/colors.xml`, `Color.parseColor`, `colorCode`), and `values-night/colors.xml` repeats the same 5 hexes; FEED re-hard-codes `#25B567` as its fallback | Low | Single source: `ContextCompat.getColor(R.color.colorN)` + a shared constant list |
| BP12 | The 5 swatches have **no selected state** — nothing marks the active colour except the preview background; no `contentDescription`, no ripple | Low | Add a check / stroke on the active swatch; add content descriptions |
| BP10 | `@id/bible_content` / `@id/bible_tags` are fixed at 15dp (**dp not sp**) and ignore `Util.fontSize`; the title is 20dp | Low | Use `sp`; honour the app font-size setting |
| BP11 | Copy-paste and string debt: all `Log.e` tags say **`"AddPostActivity.postData"`**; `"Bible post - "` is hard-coded English concatenated with Tamil strings; `"Bible Post"` and `"Post"` are hard-coded in XML; the placeholder `"nbscvzdmvcdgjvcgh"` ships on `@id/bible_title`; the toast says `"Somthing"` (= `B-20`) | Cosmetic | Fix the log tags, move all strings to `strings.xml`, delete the placeholder |
| BP19 | Dead UI and imports: `@drawable/bible_left` / `bible_right` and `@id/bible_title` are `visibility="gone"` and never shown; `import android.os.Build` is unused | Cosmetic | Delete |

> Team issues confirmed here: `B-3` (= `BP2`), `B-5` (consumed — the `edition` extra is the
> reader's launch `type`, so switching testament inside the reader mistitles the post),
> `B-9` (= `BP15`), `B-12` (= `BP3`), `B-13` (= `BP16`), `B-15` (= `BP1`), `B-16` (= `BP5`),
> `B-17` (= `BP6`), `B-19` (= `BP7` / `BP8`), `B-20` (= `BP11`).

## 13. Owned files

| File | Ownership |
|---|---|
| `app/src/main/java/com/veha/activity/BiblePostActivity.kt` | **exclusive** |
| `app/src/main/res/layout/activity_bible_post.xml` | **exclusive** |
| `util/RetrofitAPI.kt` (`postCallHead`), `util/Util.java` (`getRetrofit`, `userId`) | **not owned** — PLATFORM |
| `util/UserPreferences.kt` (`authToken`, `deleteAuthToken`, `deleteUserId`) | **not owned** — PLATFORM / STORAGE |
| `res/values/colors.xml` (`color1`..`color5`) | **not owned** — APPSHELL / THEMING |
| `adapter/HomeAdapter.kt` / `activity/ViewPostActivity.kt` `"bible"` branches | **not owned** — FEED. Never edit them from this module |
| `activity/BibleActivity.kt` | **not owned** — `BIBLE_READER.md` |

## 14. How to make common changes

Recipes for the most likely change requests on this screen.

- **Add / change / remove a colour swatch:** it is a **4-place change** — `values/colors.xml` +
  `values-night/colors.xml` (THEMING), the `LinearLayout` in `activity_bible_post.xml`, the
  `Color.parseColor` literal and the `colorCode` string here. Also tell **FEED**: `HomeAdapter`
  parses whatever hex we send and falls back to `#25B567`. Fix `BP9` first if you touch this.
- **Add a field to the post (e.g. a font or alignment):** the payload is built inline in the click
  handler; `postCallHead` takes a free-form `JsonObject`, so no PLATFORM change is needed for the
  **client** — but the server contract and the FEED renderer both must be agreed via PM first.
- **Make the button recoverable (`BP3`):** extract
  `private fun finishPosting(msg: String? = null) { postBtn.isEnabled = true; msg?.let { toast(it) } }`
  and call it from `onFailure`, the non-200 branch, the offline `else` and the `catch`.
- **Fix the duplicate-post bug (`BP16`):** replace the observer with
  `lifecycleScope.launch { val t = userPreferences.authToken.first(); … }`, or hoist a single
  `observe` into `onCreate` that just caches the token in a field.
- **Fix the token guard (`BP2`):** the exact same `CL-1` pattern appears in ~30 files; fix it here
  only, do **not** mass-edit other teams' files — report the pattern to PM instead.
- **Show the post title (`BP4`):** rename the local `val title`, set `this.title.text = postTitle`
  and drop `visibility="gone"` from `@id/bible_title` — remember the string is a mixed
  English + Tamil concatenation (`BP11`).
- **Add the permission gate (`BP15`):** copy `HomeFragment`'s exact call; on denial start
  `NoPermissionActivity` and `finish()`. Do it in `BibleActivity` too (`BR11`) so the user is not
  shown a composer they cannot submit.
- **Make Back behave (`BP5`):** delete the whole `onBackPressed` override. The reader is already on
  the stack — the override exists only because `BibleFragment` launched it with
  `FLAG_ACTIVITY_NEW_TASK` (`BE5`); fix both together.

## 15. Change log

| Change | Detail |
|---|---|
| Created (T-026, 2026-10-08) | Documented the bible-passage composer (173 LOC + 174 LOC layout) against the v1.2.0 baseline: the 3 force-unwrapped intent extras (`edition`, `tags`, `content`), the `"Bible post - " + R.string.oldBible/newBible` title, the 5 colour swatches (`#25B567` default, mirrored in `color1`..`color5`), the 8-field `POST /api/v1/post` payload sent via `postCallHead("Bearer $token", "post", data)`, the disable/re-enable button flow, and the `onBackPressed` return to `BibleActivity`. 19 issues recorded. Top defects: **`BP15`** — the post is created with **no `POST`/`Create` permission check** (`B-9`), bypassing the gate `HomeFragment` enforces on `AddPostActivity`; **`BP2`** — the `CL-1` always-true token guard makes the session-lost branch dead code and sends `Bearer null` (`B-3`); **`BP3`/`BP16`** — the Post button is left **permanently disabled** on `onFailure` / offline / `catch` (`B-12`) while the token LiveData observer registered **inside the click handler** can **re-post the passage** on any later token write (`B-13`, `CL-8`). Also newly found: **`BP4`** — a local `val title` shadows the `title: TextView` field so `@id/bible_title` is never populated (masked by `visibility="gone"`), plus `BP13` (properties removed from an already-sent payload), `BP14` (unguarded error-body parse) and `BP17` (`Util.userId` read unguarded after process death). |
