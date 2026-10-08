# BIBLE TEAM LEAD Agent

> Reports to: **PROJECT MANAGER** (`AGENTS.md`)
> Manages: the 3 module agents below. **Status: 3/3 READY.**
> Created by **T-026** (2026-10-08) — this code arrived in the 123 commits between
> `master` (v1.1) and `salvation_lamb_permissions_final_1` (v1.2.0) and had **no agent**.

---

## 1. Charter

The BIBLE team owns the app's **offline scripture reader**: the Bible tab inside `MainActivity`,
the testament/book/chapter navigation, the verse list with multi-select, the copy / share /
bookmark actions, and the "turn these verses into a feed post" composer.

It is the only feature in the app that is **100 % offline after first launch**. Its entire data
source is a single JSON document (`bible.json`, Tamil Old + New Testament) downloaded **once**
from a **different host** (`https://files.salvationlamb.com/`) by `SplashScreenActivity.getBible()`,
written to `filesDir/bible.json`, and parsed into the static `Util.bible` (`org.json.JSONObject`).
No BIBLE screen ever makes a network call **except** the final "post to feed" request.

**Boundary:** the team owns the reader and the composer UI. It does **not** own: the tab host
(`MainActivity`/`TabAdapter`, APPSHELL), the download itself (`SplashScreenActivity.getBible`, AUTH),
the `Util.bible` / `Util.bookmarkedBible` globals and the `bibleBookmark` DataStore key (PLATFORM),
or the rendering of a bible post once it reaches the timeline (`HomeAdapter` / `ViewPostActivity`,
FEED).

> **Migration note (RN):** an RN rewrite must preserve three contracts verbatim or existing
> installs break.
> **(1) The cached JSON structure** — the document has exactly two top-level keys, `"Old"` and
> `"New"`, each an array of books `{ "n": <book name>, "C": [ { "V": [ { "V": <verse text> }, … ] } ] }`.
> Chapter numbers are **implicit** (the index in `C`, 1-based for display); verse numbers are
> **implicit** (the index in `V`). Nothing in the payload is numbered.
> **(2) The bookmark string format** — a single comma-joined triple
> `"<edition>,<book>,<chapter>"` stored under the DataStore key `bibleBookmark`. The **edition
> segment is the localized display string** (`R.string.oldBible` / `R.string.newBible`), not a
> stable `old`/`new` id — see `B-4`. Parsing is `split(",")` with a `size == 3` check; anything
> else is ignored.
> **(3) Offline-first behaviour** — the reader must work with no network, from a file written once
> and **never invalidated** (see `B-2`); it must *not* re-fetch on every launch.
> Everything else (the `Spinner` + two `AutoCompleteTextView` dropdown trio, the `PopupWindow`
> chapter grid, the static `selectedText` list) is Android plumbing and should be replaced, not ported.

---

## 2. Modules owned

| Module agent | Screen / unit | Source files | LOC | Status |
|---|---|---|---|---|
| `BIBLE_ENTRY.md` | Bible tab — two testament cards | `fragments/BibleFragment.kt`, `res/layout/fragment_bible.xml` | 63 + 218 | READY |
| `BIBLE_READER.md` | The reader (dropdowns, verses, copy/share/bookmark/post) | `activity/BibleActivity.kt` (incl. the nested `MyAdapter`), `res/layout/activity_bible.xml`, `child_bible.xml`, `grid_view.xml`, `grid_spinner_item.xml` | 448 + 366/27/13/12 | READY |
| `BIBLE_POST.md` | Bible-passage post composer | `activity/BiblePostActivity.kt`, `res/layout/activity_bible_post.xml` | 173 + 173 | READY |

**Total owned: ~685 LOC of Kotlin across 3 source files + 5 layouts.**

> **Status verified (T-026, 2026-10-08):** all three module docs now exist on disk —
> `BIBLE_ENTRY.md` (188 lines, issue prefix `BE`, 10 issues), `BIBLE_READER.md` (316 lines,
> prefix `BR`, 27 issues) and `BIBLE_POST.md` (256 lines, prefix `BP`, 19 issues) — and each one
> re-confirms the `B-*` ids allocated in §9 against the real v1.2.0 source.

> **Shared-layout warning:** `child_bible.xml` declares its root id as **`@+id/child_post_layout`** —
> the same id FEED uses in `child_post.xml` / `activity_view_post.xml`. The ids are scoped per
> layout so nothing breaks today, but a grep-driven refactor in FEED will hit this file (`B-10`).

---

## 3. Intra-team flows

```
MainActivity (APPSHELL)  tab index 2, tag "Bible", icon @drawable/bible_book
   |  TabAdapter.getItem(2) -> BibleFragment()        <-- the ONLY tab with no permission gate (B-1)
   v
BibleFragment  (fragment_bible.xml)
   |   card @id/oldd  -> Intent(BibleActivity) { "type" = "old" } + FLAG_ACTIVITY_NEW_TASK
   |   card @id/neww  -> Intent(BibleActivity) { "type" = "new" } + FLAG_ACTIVITY_NEW_TASK
   v
BibleActivity  (activity_bible.xml, theme @style/BibleMaterialTheme, sensorPortrait)
   |   reads Util.bible ("Old" / "New")  +  Util.bookmarkedBible ("edition,book,chapter")
   |   Spinner @id/bible        -> edition      -> setHeading(JsonArray)
   |   AutoComplete @id/heading -> book         -> loadChapterList(JsonArray)
   |   AutoComplete @id/fakeSpinner -> chapter  -> setVersesList(position)  [PopupWindow grid]
   |   RecyclerView @id/recycler_view -> MyAdapter (inner class) -> child_bible.xml
   |        long-press a verse -> multi-select mode -> @id/button_container visible
   |   @id/copy_txt   -> ClipboardManager
   |   @id/share_txt  -> ACTION_SEND
   |   @id/bible_bookmark -> toggles Util.bookmarkedBible + DataStore bibleBookmark
   |   @id/post_txt   -> Intent(BiblePostActivity) { "edition"=type, "content", "tags" }
   v
BiblePostActivity  (activity_bible_post.xml)
   |   5 colour swatches (@id/col1..col5) -> colorCode
   |        "#25B567" | "#E24B4B" | "#6E26A6" | "#2C95E1" | "#C121C1"
   |   @id/bible_post_btn -> POST api/v1/post  (postCallHead, type="bible")
   |        200   -> MainActivity + finish()
   |        back  -> Intent(BibleActivity) { "type" = <edition> } + FLAG_ACTIVITY_NEW_TASK,
   |                 then super.onBackPressed()
   `-> back to BibleActivity
```

### State handed between the screens

| Hop | Carrier | Value |
|---|---|---|
| `BibleFragment` -> `BibleActivity` | intent extra `type` | `"old"` \| `"new"` (lowercase, **not** the localized label) |
| `BibleActivity` -> `BiblePostActivity` | extra `edition` | the launch `type` value, *not* the current spinner selection (`B-5`) |
| `BibleActivity` -> `BiblePostActivity` | extra `content` | selected verses, sorted by verse number, joined with `"\n\n"`, trimmed |
| `BibleActivity` -> `BiblePostActivity` | extra `tags` | `"<book>, <chapter>"` from `@id/heading` + `@id/fakeSpinner` |
| `BiblePostActivity` -> `BibleActivity` (back) | extra `type` | the `edition` it received |
| within `BibleActivity` | `companion object var selectedText: ArrayList<String>` | **static, process-wide**; survives the activity (`B-6`) |

---

## 4. Shared state touched by this team

| Store / global | Keys / fields | Read | Written |
|---|---|---|---|
| `Util.bible` (`org.json.JSONObject`, static) | `"Old"`, `"New"` | `BibleActivity.setBibleEdition` → `setHeading` | **never here** — written only by `SplashScreenActivity.getBible()` (AUTH) |
| `Util.bookmarkedBible` (`String`, static) | the `"edition,book,chapter"` triple | `BibleActivity.onCreate` + `setVersesList` | `BibleActivity` bookmark toggle (set to `"Dummy"` when cleared) |
| DataStore `SalvationLamb` | `bibleBookmark` | observed in `SplashScreenActivity` **and** `MainActivity` (both assign `Util.bookmarkedBible`) | `saveBibleBookmark()` / `deleteBibleBookmark()` from `BibleActivity` |
| DataStore `SalvationLamb` | `token` | `BiblePostActivity.postData` (observer created inside the click handler) | — |
| DataStore `SalvationLamb` | `token`, `userId` | — | **deleted** in `BiblePostActivity`'s unreachable session-lost branch (`B-3`) |
| `Util.userId` | — | `BiblePostActivity` post payload | — |
| `Util.permissionMap` / `Util.hasPermission` | — | **never consulted by this team** (`B-1`, `B-9`) | — |
| `filesDir/bible.json` | the whole document | written + read by AUTH only | — |

---

## 5. API surface used by this team

| Endpoint | Method | Base URL | Used by |
|---|---|---|---|
| `salvationlamb-images/bible.json` (`RetrofitAPI.getContent()`) | GET | **`https://files.salvationlamb.com/`** via `Util.getRetrofit(String)` | **AUTH** (`SplashScreenActivity.getBible`) — this team only consumes the cached result |
| `api/v1/{url}` with `url = "post"` (`RetrofitAPI.postCallHead`) | POST | `https://server.salvationlamb.com` via `Util.getRetrofit()` | `BIBLE_POST` |

**Request body for the post call** (every value a `String`, per `G7`): `title`
(`"Bible post - <localized testament>"`), `content`, `tags`, `image` (`""`), `url` (`""`),
`type` (`"bible"`), `userId`, `colorCode` (`"#RRGGBB"`).

> **Third host in the app.** `AGENTS.md` records two base URLs (`Util.getRetrofit()` vs the dead
> `APIUtil.retrofit`); `files.salvationlamb.com` is a **third** (extends `G4`, see `B-2`).

**Response envelopes:** the post call's success body is never parsed — only `response.code() == 200`
is checked; the error body is read as `{status, errorMessage}` and only logged. The bible document
itself has **no envelope**: a bare object with the two keys `Old` and `New` — a fifth shape beside
`result` / `results` / `files` / `notification`.

---

## 6. Cross-team dependencies

| Needs | Owner team | Escalate to PM when |
|---|---|---|
| `MainActivity` tab index 2 + `TabAdapter.getItem(2)` (the entry point) | **APPSHELL / MAIN_NAV** | the tab order, the tag `"Bible"`, or the missing permission gate (`B-1`) changes |
| `NoPermissionActivity` (destination if a gate is ever added) | APPSHELL / NO_PERMISSION | adding the gate |
| `POST api/v1/post`, the `type="bible"` post kind and the `colorCode` field | **FEED / ADD_POST** + **PLATFORM / NETWORK** | the payload, the post type, or the palette changes |
| Rendering of a bible post (`HomeAdapter` `"bible"` branch, `ViewPostActivity` `"bible"` branch, ids `bible_cons_layout` / `bible_tags` / `bible_content`) | **FEED** | anything we send changes — the renderer falls back to a hard-coded `#25B567` |
| `Util.bible`, `Util.bookmarkedBible`, `Util.getRetrofit(String)` | **PLATFORM / COMMONS + NETWORK** | always — shared statics |
| `bibleBookmark` DataStore key + `saveBibleBookmark` / `deleteBibleBookmark` | **PLATFORM / STORAGE** | the key, the value format, or the `"null"`-string read behaviour changes |
| `SplashScreenActivity.getBible()` (download, cache, parse) | **AUTH / SPLASH** | the cache filename, the host, the refresh policy (`B-2`) or its error handling (`S23`) |
| `R.string.oldBible` / `R.string.newBible` (Tamil), `@style/BibleMaterialTheme`, `@drawable/bible_book`, `old_bible.jpg`, `new_bible.jpg`, `color1`..`color5` | **APPSHELL / THEMING** | any rename — the bookmark format depends on the two **string values** (`B-4`) |
| `BibleSelector` data class (`util/DataModels.kt`) | PLATFORM / DATA_MODELS | it is declared but **unreferenced** (`B-11`) — delete only via PM |

---

## 7. Delegation rules

- Anything about the **two testament cards**, the tab body, or how the reader is launched →
  `BIBLE_ENTRY.md`.
- Anything about **dropdowns, verses, selection, copy, share, bookmark, next/previous** →
  `BIBLE_READER.md`.
- Anything about the **composer, the colour swatches or the post request** → `BIBLE_POST.md`.
- A change to the **bookmark string format** touches `BIBLE_READER` *and* PLATFORM/STORAGE *and*
  AUTH/SPLASH + APPSHELL/MAIN_NAV (both seed `Util.bookmarkedBible`) → **always PM-coordinated**.
- A change to the **bible.json shape** touches AUTH/SPLASH (download + parse) and `BIBLE_READER`
  (consume) → PM-coordinated, and must account for **already-installed caches that are never
  refreshed** (`B-2`).
- Adding a **permission gate** to the Bible tab is an APPSHELL change (`TabAdapter`) → escalate.
- Never edit `HomeAdapter`'s or `ViewPostActivity`'s `"bible"` branch — that is FEED.

---

## 8. Definition of done (team-level)

- [ ] Code follows the conventions in `AGENTS.md` §5.
- [ ] Tested with **`Util.bible == null`** (kill the process from the Bible tab, then restore) — the
      team's most likely crash (`B-7`).
- [ ] Tested with a **cross-testament bookmark** (bookmark an Old-Testament chapter, open the New
      Testament) — `B-8`.
- [ ] Tested **offline** end to end: the reader must work; the post button must not dead-lock (`B-12`).
- [ ] The module agent's `.md` is updated in the same change (incl. its Change log).
- [ ] Cross-team impact reported to PM.
- [ ] `agents/TASKS.md` updated by PM with date & time.

---

## 9. Team-level known issues

| # | Issue | Module | Risk |
|---|---|---|---|
| B-7 | `Util.bible` is a **static `JSONObject` seeded only by the splash**. After process death the OS restores `MainActivity` directly, `Util.bible` is `null`, and the first edition selection calls `Util.bible.get("Old")` → **NPE, Bible tab crashes on open** | BIBLE_READER | **Critical (crash)** |
| B-8 | `setHeading` resolves a bookmark with `keyList[keyList.indexOf(bookmarkedContent)]`; when the bookmarked book is not in the selected testament `indexOf` returns **-1** → `IndexOutOfBoundsException`. Reproduces by bookmarking an Old-Testament chapter then opening the New Testament | BIBLE_READER | **Critical (crash)** |
| B-1 | The Bible tab is the **only** `TabAdapter` branch with **no `Util.hasPermission` gate** (positions 0, 1, 3, 4, 5 are all gated) | BIBLE_ENTRY / APPSHELL | **High (security)** |
| B-9 | `BiblePostActivity` creates a post with **no `POST`/`Create` permission check**, while `HomeFragment` gates `AddPostActivity` with exactly that check → a trivial bypass of the posting gate | BIBLE_POST | **High (security)** |
| B-3 | `BiblePostActivity.postData` uses the always-true guard `if (!TextUtils.isEmpty(it) \|\| !it.equals("null") \|\| !it.isNullOrEmpty())` (cluster **CL-1**) — the session-lost `else` is **dead code**, so a missing token posts `Bearer null` | BIBLE_POST | **High** |
| B-12 | The Post button is disabled first and **only re-enabled inside `onResponse`** — `onFailure`, the offline path and the `catch` leave it **permanently disabled** with no message | BIBLE_POST | **High (UX dead end)** |
| B-13 | `postData` registers a **new continuous `authToken` LiveData observer inside the click handler**; any later write to `token` re-fires it and **posts the passage again** | BIBLE_POST | **High (duplicate posts)** |
| B-2 | `bible.json` is cached to `filesDir` and **never invalidated** — a corrected verse on the server never reaches an existing install; it is also fetched from a **third base URL** | AUTH/PLATFORM (consumed here) | **High** |
| B-4 | The bookmark's first segment is the **localized Tamil display string**; translating the app or renaming `R.string.oldBible` **orphans every stored bookmark** (`indexOf` → -1 → `Spinner.setSelection(-1)`) | BIBLE_READER | **High** |
| B-6 | `BibleActivity.selectedText` is a **`companion object` (static) `ArrayList`** mutated from an inner adapter; it outlives the activity and is reassigned (never cleared) in six places | BIBLE_READER | Medium |
| B-14 | `fakeSpinner.text.toString().toInt()` in the next/previous handlers — an empty or non-numeric chapter field throws `NumberFormatException` | BIBLE_READER | Medium |
| B-15 | `intent.extras!!.getString(...)` force-unwraps in **both** activities (`BibleActivity` ×1, `BiblePostActivity` ×3) — launching either without extras is an immediate NPE | BIBLE_READER, BIBLE_POST | Medium |
| B-16 | `BibleFragment` launches the reader with `FLAG_ACTIVITY_NEW_TASK` from a fragment, and `BiblePostActivity.onBackPressed` does the same **plus** `super.onBackPressed()` — the back stack grows unpredictably | BIBLE_ENTRY, BIBLE_POST | Medium |
| B-17 | Logo / home taps call `startActivity(MainActivity)` **without `finish()` or `CLEAR_TOP`**, stacking duplicate `MainActivity` instances | BIBLE_READER, BIBLE_POST | Medium |
| B-18 | Every edition change re-serializes a whole testament (`Util.bible.get("Old").toString()`) and re-parses it with Gson, then rebuilds `bibleMap` / `chapterMap` — a multi-MB round trip on the main thread | BIBLE_READER | Medium (jank) |
| B-19 | Almost no user feedback: copy shows no confirmation, a non-200 post only logs, offline does nothing at all | all three | Medium (UX) |
| B-5 | The composer receives the **launch `type`**, not the spinner's current edition, so switching testament inside the reader produces a post titled with the wrong testament | BIBLE_READER, BIBLE_POST | Medium |
| B-10 | `child_bible.xml`'s root id is `@+id/child_post_layout`, duplicating FEED's post-row id | BIBLE_READER | Low |
| B-11 | `data class BibleSelector(content, isSelected)` in `DataModels.kt` is **never referenced** — dead model | PLATFORM (recorded here) | Low |
| B-20 | Hard-coded user-visible strings and a typo'd toast (`"Please select atleast one"`, `"Bible Post"`, `"post"`, `"Post"`); the only two `strings.xml` entries (`oldBible`, `newBible`) hold **Tamil text in the default `values/` folder** | all three | Cosmetic |

---

## 10. Change log

| Change | Detail |
|---|---|
| Created (T-026, 2026-10-08) | Team created to cover 3 previously unowned source files (~685 LOC) + 5 layouts added by the v1.2.0 branch. Documented the offline-first data path (one JSON from a **third host**, cached forever in `filesDir`), the `"Old"`/`"New"` document shape with implicit chapter/verse numbering, the `"edition,book,chapter"` bookmark contract and its localized-string weakness, the full navigation chain Main → `BibleFragment` → `BibleActivity` → `BiblePostActivity` → back, and 20 team-level issues — headlined by the **`Util.bible` null-after-process-death crash** (`B-7`), the **`indexOf` -1 crash on a cross-testament bookmark** (`B-8`), the **ungated Bible tab** (`B-1`), the **ungated bible post** (`B-9`) that bypasses the `POST`/`Create` permission enforced everywhere else, and the `CL-1` always-true token guard (`B-3`). |
